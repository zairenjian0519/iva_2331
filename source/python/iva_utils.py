# cython: language_level=3
from iva_logger import logger
import json
from math import atan2, pi
from subprocess import Popen, PIPE
from io import BytesIO
from exif import Image
import cv2
cv2.ocl.setUseOpenCL(False)
cv2.setNumThreads(0)
from concurrent.futures import ThreadPoolExecutor
import numpy as np
import base64
from box import Box
from copy import deepcopy
import requests
import os
from datetime import datetime
import time
from paho.mqtt import client as mqtt_client
from multiping import MultiPing
import copy
import psutil
from interval import Interval
import glob
import zipfile
import shutil
import sys
from shapely.geometry import Polygon, box, Point
from shapely.validation import make_valid
from turbojpeg import TurboJPEG, TJPF_BGR
import re
from queue import Queue
from pony.orm import db_session
jpeg = TurboJPEG()

class ThreadPoolExecutor(ThreadPoolExecutor):
    def __init__(self, max_workers=None,thread_name_prefix=""):
        super().__init__(max_workers,thread_name_prefix)
        self._work_queue = Queue(self._max_workers)


def invalid_region(invalids,x1,y1,x2,y2):
    is_invalid = 1
    box_polygon = box(x1,y1,x2,y2)
    for invalid in invalids:
        p = Polygon(invalid)
        polygon = p if p.is_valid else make_valid(p).buffer(0.001)
        intersect = box_polygon.intersection(polygon).area/ box_polygon.area
        if intersect > 0.5:
            is_invalid -= 1
            break
    return is_invalid




def get_iou(box1 :np.array, box2: np.array):
    """
    :param box1: [N, 4]
    :param box2: [M, 4]
    :return: [N, M]
    """
    area1 = (box1[:, 2] - box1[:, 0]) * (box1[:, 3] - box1[:, 1])
    area2 = (box2[:, 2] - box2[:, 0]) * (box2[:, 3] - box2[:, 1])
    # broadcasting, 两个数组各维度大小 从后往前对比一致， 或者 有一维度值为1；
    lt = np.maximum(box1[:, np.newaxis, :2], box2[:, :2])
    rb = np.minimum(box1[:, np.newaxis, 2:], box2[:, 2:])
    wh = rb - lt
    wh = np.maximum(0, wh) # [N, M, 2]
    inter = wh[:, :, 0] * wh[:, :, 1]
    iou = inter / (area1[:, np.newaxis] + area2 - inter)
    return iou

def pHash(img):
    # 感知哈希算法
    # 缩放32*32
    img = cv2.resize(img, (32, 32))   # , interpolation=cv2.INTER_CUBIC

    # 转换为灰度图
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    # 将灰度图转为浮点型，再进行dct变换
    dct = cv2.dct(np.float32(gray))
    # opencv实现的掩码操作
    dct_roi = dct[0:8, 0:8]

    phash = []
    avreage = np.mean(dct_roi)
    for i in range(dct_roi.shape[0]):
        for j in range(dct_roi.shape[1]):
            if dct_roi[i, j] > avreage:
                phash.append(1)
            else:
                phash.append(0)
    return phash

def hammingDist(s1, s2):
    return sum([ch1 != ch2 for ch1, ch2 in zip(s1, s2)])

def thread_pool(args_list, execute_func):
    with ThreadPoolExecutor(1) as pool:
        pool.map(execute_func, args_list)

def for_every_camera(args_list, execute_func):
    for args in args_list:
        execute_func(args)




def main2sub(url,camera_name):
    if camera_name == "hk":
        if "main" in url:
            sub_url = url.replace("main","sub")
        elif url.endswith("1"):
            sub_url = url[:-1] + "2"
        else:
            sub_url = url
    elif camera_name == "dh":
        sub_url = url[:-1] + "1" if url.endswith("0") else url
    else:
        sub_url = url
    logger.info("原始视频流{},设备厂商{},转换视频流{}".format(url,camera_name,sub_url))
    return sub_url

def sub2main(url,camera_name,sub_type):
    if "sub" in url:
        sub_url = url.replace("sub","main")
    elif url.endswith("2"):
        sub_url = url[:-1] + "1"
    else:
        sub_url = url
    return sub_url

def compare_dict(last_dict, current_dict, stream_channels, sixteen_dict):
    '''
    比较两个字典:用于判断心跳值
    '''
    if not last_dict and not current_dict:
        return
    else:
        for key1, key2 in zip(list(last_dict), list(current_dict)):
            if last_dict[key1] < current_dict[key2]:
                last_dict[key1] = current_dict[key2] #心跳正常
                #logger.info("{}心跳正常".format(key1))
            else:
                del last_dict[key1]
                del current_dict[key2]

                if key1 == "page16":
                    for page_pid in sixteen_dict.values():
                        try:
                            os.kill(page_pid, 9)
                        except OSError:
                            pass
                else:
                    camera_id_info = stream_channels.pop(key1, None)
                    if camera_id_info:
                        topic_pid = camera_id_info[0]
                        os.kill(topic_pid, 9)
                        #cmd = ['sed', '-i', '{}c\    value, keep = 0, False'.format(str(use_channel_line)), '/home/super/AI_Service/static/change_stream.py']
                        #Popen(cmd)
 
                logger.info("{}心跳异常,任务结束".format(key1))


# def show_gpu_status(gpu_id):
#     '''
#     return:gpu空余显存
#     '''
#     pynvml.nvmlInit()  # 必须先初始化
#     handle = pynvml.nvmlDeviceGetHandleByIndex(gpu_id)
#     meminfo = pynvml.nvmlDeviceGetMemoryInfo(handle)
#     pynvml.nvmlShutdown()
#     return meminfo.free // 1048576

def show_apu_status(device):
    p = Popen(['lynxi-smi'],stdout=PIPE)
    stdout = p.stdout.readlines()[10].decode()
    total_mem = 8192
    use_mem = int(re.findall(r"(\d+)MiB/8192MiB",stdout)[0])

    return total_mem-use_mem

def show_cpu_status():
    '''
    return:cpu平均占用率
    '''
    total_cpu = psutil.cpu_percent(interval=0.5,percpu=True)
    average_cpu = np.mean(total_cpu).item()
    return average_cpu

def show_disk_status(file_path="/"):
    disk_info = psutil.disk_usage(file_path)
    #sdiskusage(total=1006530654208, used=68517429248, free=886812884992, percent=7.2)
    free = disk_info.free
    free_space = free // 1073741824 #2 ** 30 单位是G
    return free_space

@db_session
def photo_dump(db,file_path="/home/super/AI_Service/alarm_picture/download/image/*/*.jpg"):
    f = glob.glob(file_path)
    sorted_f = sorted(f, key=lambda pic_file: os.path.getctime(pic_file))
    deleted_f = sorted_f[:len(sorted_f)//2]

    end_pic = deleted_f[-1]

    #print(len(deleted_f))
    #print(deleted_f[-1])
    end_item = db.execute('SELECT alarm_time FROM alarm_detail WHERE pic_path = \"{end_pic}\"'.format_map(vars()))
    end_time = end_item.fetchall()[0][0]
    #bb = db.execute('SELECT pic_path FROM alarm_detail WHERE alarm_time = \"{b}\"'.format_map(vars()))
    #print(bb.fetchall()[0][0])
    is_drop = db.execute('DELETE FROM alarm_detail WHERE alarm_time <= \"{end_time}\"'.format_map(vars()))

    for i in deleted_f:
        os.remove(i)
    for filename in glob.glob("/home/super/AI_Service/alarm_picture/download/*.zip"):
        os.remove(filename)
    logger.info("共删除{}张报警图片".format(len(deleted_f)))

def get_size(file_path="/home/super/AI_Service/alarm_picture/download/ori_image/*/*.jpg"):
    f = glob.glob(file_path)
    total_size = 0
    for i in f:
        total_size += os.path.getsize(i)
    return total_size // 1048576 #2 ** 20 单位是M

def zip_dir(dir_="/home/super/AI_Service/alarm_picture/download/ori_image"):
    zip_file = "/home/super/AI_Service/alarm_picture/download/ori_image_" + str(int(time.time())) + ".zip"
    with zipfile.ZipFile(zip_file, 'w',zipfile.ZIP_DEFLATED) as z:
        dn = os.path.basename(dir_)
        z.write(dir_, arcname=(dn))
        for root, dirs, files in os.walk(dir_):
            for fn in files:
                fp = os.path.join(root, fn)
                z.write(
                    fp,
                    arcname=dn + '/' + os.path.relpath(fp, dir_)
                )
    logger.info("{}压缩成功".format(zip_file))
    return zip_file



def url2img(args):
    imgname, pushurl = args
    # print("--imgname, pushurl--", imgname, pushurl)
    cmd = ['/usr/local/ffmpeg/bin/ffmpeg', '-y', 
        '-loglevel',  'quiet',
        # '-timeout', '1000000',
        # '-rtsp_transport', 'tcp',
        '-i', pushurl,
        '-ss', '00:00:01',
        '-frames:v', '1',
        '-f', 'mjpeg',
        # '-q:v', '1',
        '/home/super/AI_Service/alarm_picture/download/show_image/{imgname}.jpg'.format_map(vars())
        ]
    proc = Popen(cmd)
    #proc.terminate()
    #proc.wait()
    proc.communicate(1)
    return 1




def lxz_ping(ip_addr):
    '''
    return 1通0不通
    '''
    mp = MultiPing([ip_addr])
    mp.send()
    res = 1 if mp.receive(0.5)[0] else 0
    return res

"""
def send_mail(zip_file):
    smtp_server = 'smtp.126.com'
    username = 'liuxiaozheat@126.com'
    password = 'QMONIPDOANQXIVRE'
    sender = 'liuxiaozheat@126.com'
    receivers = ['852949170@qq.com']
    today_time = datetime.today().strftime("%m-%d %H：%M")
    msg = MIMEMultipart()
    # 邮件正文
    EMAIL_FROM_NAME = '测试平台'   # 自定义发件人名称
    msg.attach(MIMEText("你好，数据管理员！\r\n今日数据已回流，请查看附件",'plain','utf-8'))   # 文本内容换行\r\n
    msg['From'] = formataddr(pair=(EMAIL_FROM_NAME, sender))     # 自定义发件人的名称
    # msg['To'] = receivers[0]  # 发送给receivers里的第一个用户
    msg['To'] = ";".join(receivers)  # 发送给多个好友
    subject = "{}--jmeter接口测试报告".format(today_time)
    msg['Subject'] = subject
    data = open(zip_file, 'rb')
    ctype, encoding = mimetypes.guess_type(zip_file)
    if ctype is None or encoding is not None:
        ctype = 'application/octet-stream'
    maintype, subtype = ctype.split('/', 1)
    file_msg = MIMEBase(maintype, subtype)
    file_msg.set_payload(data.read())
    data.close()
    encoders.encode_base64(file_msg)  # 把附件编码
    file_msg.add_header('Content-Disposition', 'attachment', filename="测试报告.zip")  # 修改邮件头
    msg.attach(file_msg)
    #start_t = time.time()
    #try:
    server = smtplib.SMTP(smtp_server)
    server.ehlo()
    server.login(username,password)
    server.sendmail(sender,receivers,msg.as_string())
    server.quit()
    logger.info("发送成功")
    except Exception as err:
        print("发送失败")
        print(err)
    #print(time.time() - start_t)
"""

def connect_mqtt(mqtt_host, mqtt_port):
    def on_connect(client, userdata, flags, rc):
        if rc == 0:
            logger.info("连接MQTT Broker成功")
        else:
            logger.error("连接MQTT Broker失败！错误码 %d\n", rc)
    '''
    1	协议版本错误
    2	无效的客户端标识
    3	服务器无法使用
    4	错误的用户名或密码
    5	未经授权
    '''


    client = mqtt_client.Client()
    client.on_connect = on_connect

    client.connect_async(mqtt_host, mqtt_port, 600) #
    client.loop_start()
    return client

class mqttProducer:
    def __init__(self, system_cfg):
        host = system_cfg.get("mqtt_ip","0.0.0.0")
        port = int(system_cfg.get("mqtt_port", 1883))
        self.client = connect_mqtt(host,port)

    def subscribe(self):
        #subscribe(self.client, self.topic)
        self.client.loop_forever()

    def publish(self, topic, pyload):
        def on_publish(client, userdata, mid): #回调函数
            logger.info("报警信息推送至前端页面成功")
        result = self.client.publish(topic, pyload) # json.dumps(payload))
        self.client.on_publish = on_publish

    def disconnect(self):
        self.client.disconnect()

def rtsp2rtmp(rtsp_url, topic, sixteen_dict):
    rtmp_url = "rtmp://127.0.0.1:1935/live/{}".format(topic)
    cmd = ['/usr/local/ffmpeg/bin/ffmpeg',     
           '-re',
           '-loglevel',  'quiet',
           '-rtsp_transport', 'tcp',
           '-i',         rtsp_url,
           '-c:v',       'copy',
           '-f',         'flv',
           '-r',         '25',
           '-s',         '320x240',
           '-an', 
           '-g',         '5',
           '-bf',        '0',
           '-b:v',       '1024k',
           '-preset:v',  'ultrafast',
           '-tune:v',    'zerolatency',
           rtmp_url]
    proc = Popen(cmd)
    sixteen_dict[topic] = proc.pid
    proc.communicate()



def bg_stream(camera_id, stream_channels, mix_type):
    _, use_channel_line, last_mix_type, _ = stream_channels[camera_id]
    cmd = ['sed', '-i', '{}s/{}/{}/g'.format(str(use_channel_line), str(last_mix_type), str(mix_type)), '/home/super/AI_Service/static/change_stream.py']
    stream_channels[camera_id][2] = mix_type
    proc = Popen(cmd)

def num_generator(lst1, lst2):
    for i in lst1:
        for j in lst2:
            yield i, j

def bg_task(detectmode, total_img_path, ori_hw, person_boxes, result_other, img_bs64, camera_id, rtsp_ip, mqtt_client, rois_dict, statis, statis_func_dict, last_alarm_time, line1, line2, is_resize):
    t0 = time.time()
    ori_h, ori_w = ori_hw
    if camera_id not in last_alarm_time:
        last_time_dict = {0:0,1:0,2:0,3:0,4:0,5:0,6:0,7:0,8:0,9:0,10:0,11:0,12:0,13:0,14:0,15:0,16:0,18:0,19:0,20:0,21:0,22:0,23:0,24:0,25:0,26:0,27:0,28:0,29:0,30:0,31:0,"fire_list":None,"fire_count":0}   #建一个报警时间字典，每次更新
        last_alarm_time[camera_id] = last_time_dict
    #result_other = split_result(result2,[]) #先拆分结果
    #result_other = remove_small(split_result2, min_hw, ori_h, ori_w) #过滤小目标
    #if result_other or :    
    

    '''
    for out_label, rois in rois_dict.items():
        rois_elec = []
        for roi in rois["camera_roi"]:
            every_elec = elec_fence_py([tuple(map(lambda a,b:int(a*b),roi_point,ori_hw[::-1])) for roi_point in roi])
            rois_elec.append(every_elec)
        rois_dict[out_label]["camera_roi"] = rois_elec
    '''
    try:
        img = str2np(img_bs64) if isinstance(img_bs64,str) else img_bs64
    except Exception as e:
        logger.error(e)
        return
    #img = str2np(img_bs64)
    #img = bin2np(img_bs64)
    img_path = os.path.join(total_img_path,"image/{rtsp_ip}".format_map(vars())) 
    ori_img_path = os.path.join(total_img_path,"ori_image/{rtsp_ip}".format_map(vars()))
    mk_img_path(img_path)
    mk_img_path(ori_img_path)
    for i in result_other:
        res_label = i[0][0]
        alarm_grade = i[0][6]
        target_threshold = i[0][8]
        #myy 添加告警推送
        jk_box = i[0][1]

        targetcount = len(i)
        if targetcount <= target_threshold:
            continue
        my_roi = rois_dict[str(res_label)]
        roi_list = my_roi.get("camera_roi")
        invalids = my_roi.get("invalid_roi")
        if roi_list:
            #过滤电子围栏
            n_i = []
            for r in roi_list:

                r = Polygon(r)
                #r = r if r.is_valid else make_valid(r).buffer(0.001)
                for j in i:
                    res = r.covers(Point(j[2]+j[4]/2,j[3]+j[5]))
                    #res = r.detect([j[2]+j[4]/2,j[3]+j[5]])
                    if res and (j not in n_i):
                        n_i.append(j)
            n_targetcount = len(n_i)
            if res_label == 26 or res_label == 25:
                send_result(ori_h, ori_w, invalids, line1, line2, n_i, n_targetcount, roi_list, img.copy(), ori_img_path, img_path, res_label, alarm_grade, camera_id ,mqtt_client, detectmode, is_resize,jk_box)
            else:           
                if n_targetcount <= target_threshold:
                    continue
                if last_alarm_time.get(camera_id) and int(time.time()) - last_alarm_time[camera_id][res_label] >= i[0][7]:
                    if res_label == 2:
                        fire_count = last_alarm_time[camera_id]["fire_count"]
                        last_fire_count = fire_count
                        #myy发现这是火只报3次的地方
                        if fire_count == 5:
                            last_alarm_time[camera_id]["fire_list"] = None
                            last_alarm_time[camera_id]["fire_count"] = 0
                            # last_alarm_time[camera_id][res_label] += 21600    #惩罚6小时
                            last_alarm_time[camera_id][res_label] += 21600   
                            continue
                        fire_phash_list = []
                        for fire_box in n_i:
                            fire_img = img[fire_box[3]:fire_box[3]+fire_box[5],fire_box[2]:fire_box[2]+fire_box[4]]
                            fire_phash = pHash(fire_img)
                            fire_phash_list.append(fire_phash)
                        last_fire_phash_list = last_alarm_time[camera_id]["fire_list"]
                        if last_fire_phash_list:
                            for last_fire_phash, now_fire_phash in num_generator(last_fire_phash_list, fire_phash_list):
                                hm_dis = hammingDist(last_fire_phash, now_fire_phash)
                                if hm_dis <= 13:
                                    fire_count += 1
                                    break
                            last_alarm_time[camera_id]["fire_list"] = fire_phash_list
                            if fire_count > last_fire_count:
                                last_alarm_time[camera_id]["fire_count"] = fire_count
                                continue

                    
                    last_alarm_time[camera_id][res_label] = int(time.time())

                    send_result(ori_h, ori_w, invalids, line1, line2, n_i, n_targetcount, roi_list, img.copy(), ori_img_path, img_path, res_label, alarm_grade, camera_id ,mqtt_client, detectmode, is_resize,jk_box)
        else:
            if last_alarm_time.get(camera_id) and int(time.time()) - last_alarm_time[camera_id][res_label] >= i[0][7]:
                if res_label == 2:
                    fire_count = last_alarm_time[camera_id]["fire_count"]
                    last_fire_count = fire_count
                    if int(time.time()) - last_alarm_time[camera_id][res_label] <= i[0][7] + 5:
                        fire_count += 1
                    #myy发现这是火只报3次的地方
                    if fire_count >= 3:
                        last_alarm_time[camera_id]["fire_list"] = None
                        last_alarm_time[camera_id]["fire_count"] = 0
                        # last_alarm_time[camera_id][res_label] += 21600    #惩罚6小时
                        last_alarm_time[camera_id][res_label] += 0    
                        continue
                    fire_phash_list = []
                    for fire_box in i:
                        fire_img = img[fire_box[3]:fire_box[3]+fire_box[5],fire_box[2]:fire_box[2]+fire_box[4]]
                        fire_phash = pHash(fire_img)
                        fire_phash_list.append(fire_phash)
                    last_fire_phash_list = last_alarm_time[camera_id]["fire_list"]
                    if last_fire_phash_list:
                        for last_fire_phash, now_fire_phash in num_generator(last_fire_phash_list, fire_phash_list):
                            hm_dis = hammingDist(last_fire_phash, now_fire_phash)
                            if hm_dis <= 13:
                                fire_count += 1
                                break
                        if fire_count > last_fire_count:
                            last_alarm_time[camera_id]["fire_count"] = fire_count
                            last_alarm_time[camera_id]["fire_list"] = fire_phash_list
                            continue
                    last_alarm_time[camera_id]["fire_list"] = fire_phash_list
                
                last_alarm_time[camera_id][res_label] = int(time.time())
                t1 = time.time()
                # print("2处的时间 =",t1-t0)
                send_result(ori_h, ori_w, invalids, line1, line2, i, targetcount, [], img.copy(), ori_img_path, img_path, res_label, alarm_grade, camera_id ,mqtt_client, detectmode, is_resize,jk_box)
                # print("send用时间 =",time.time()-t0)
    person_num = len(person_boxes)
    for k,v in statis.items():
        statis_rois = rois_dict[str(k)].get("camera_roi")
        k = int(k)
        statis_func = statis_func_dict[k]
        invalids = v['invalid_rois']
        v_target_threshold = v["target_threshold"]
        if k == 13 and person_num <= v_target_threshold:
            continue
        if statis_rois:
            n_person = []
            leave_flag = 0 #离岗标志位
            for statis_roi in statis_rois:
                #statis_roi_list_texts.append(statis_roi.roi)
                statis_boxes = []
                statis_count = 0
                statis_roi = Polygon(statis_roi)
                #statis_roi = statis_roi if statis_roi.is_valid else make_valid(statis_roi).buffer(0.001)
                for person_box in person_boxes:
                    if not invalid_region(invalids,person_box[2],person_box[3],person_box[2]+person_box[4],person_box[3]+person_box[5]):
                        continue
                    statis_roi = Polygon(statis_roi)
                    center_x = int(statis_roi.centroid.x)
                    foot_left_x = person_box[2]
                    foot_center_x = person_box[2]+person_box[4]//2
                    foot_right_x = person_box[2]+person_box[4]
                    if center_x > foot_right_x:
                        foot_x = foot_right_x
                    elif  foot_left_x <= center_x <= foot_right_x:
                        foot_x = foot_center_x
                    else:
                        foot_x = foot_left_x
                    res = statis_roi.covers(Point(foot_x,max(person_box[3],person_box[3]+person_box[5]-50)))
                    if res and (person_box not in n_person):
                        statis_boxes.append(person_box)
                        statis_count += 1
                if k == 10:
                    if statis_func(statis_count) and not lxz_occlusion(img):
                        leave_flag += 1
                    else:
                        n_person += statis_boxes
                elif k != 10 and statis_func(statis_count):
                    n_person += statis_boxes
            n_count = len(n_person)
            if (n_count and k == 10 and not leave_flag) or (k==10 and invalids) or (not n_count and k != 10) or (k == 13 and n_count <= v_target_threshold) or int(time.time()) - last_alarm_time[camera_id][k] < v["alarm_Interval_period"]:
                continue
            else:
                last_alarm_time[camera_id][k] = int(time.time())
                n_person = [] if k == 10 else n_person
                if k == 10 and lxz_occlusion(img):
                    continue
                send_result(ori_h, ori_w, invalids, line1, line2, n_person, len(n_person), statis_rois, img.copy(), ori_img_path, img_path, k, v["alarm_grade"], camera_id ,mqtt_client, detectmode,is_resize,[])

            '''
            if n_person and int(time.time()) - last_alarm_time[camera_id][k] >= v["alarm_Interval_period"]:
                last_alarm_time[camera_id][k] = int(time.time())
                send_result(n_person,  statis_roi_list_texts, img.copy(), ori_img_path, img_path, k, v["alarm_grade"], camera_id ,mqtt_client)
            elif k == 10 and not n_person and int(time.time()) - last_alarm_time[camera_id][k] >= v["alarm_Interval_period"]: #离岗
                last_alarm_time[camera_id][k] = int(time.time())
                send_result(n_person,  statis_roi_list_texts, img.copy(), ori_img_path, img_path, k, v["alarm_grade"], camera_id ,mqtt_client)
            '''

        else:
            new_person_boxes = [person_box for person_box in person_boxes if invalid_region(invalids,person_box[2],person_box[3],person_box[2]+person_box[4],person_box[3]+person_box[5])]
            new_person_num = len(new_person_boxes)
            if statis_func(new_person_num) and int(time.time()) - last_alarm_time[camera_id][k] >= v["alarm_Interval_period"]:
                last_alarm_time[camera_id][k] = int(time.time())
                if k == 10 and lxz_occlusion(img):
                    continue

                #person_boxes = [] if k == 10 else person_boxes
                send_result(ori_h, ori_w, invalids, line1, line2, new_person_boxes, new_person_num, [], img.copy(), ori_img_path, img_path, k, v["alarm_grade"], camera_id ,mqtt_client, detectmode, is_resize,[])



def send_result(ori_h, ori_w, invalids, tl1, tl2, i, targetcount, roi_list, img, ori_img_path, img_path, res_label, alarm_grade, camera_id, mqtt_client, detectmode, is_resize,jk_box):
    if res_label == 26:
        result_jk = {"jk_box":jk_box}
        jk_data = json.dumps(result_jk)
        mqtt_client.publish("ClearanceInferenceResult", jk_data )
    elif res_label == 25:
        result_yp = {"yp_box":jk_box}
        yp_data = json.dumps(result_yp)
        mqtt_client.publish("RotorBladeInferenceResult", yp_data )
    else:
        # print(i, img.shape)
        t0 = time.time()
        boxes_text = str(i) if i else None
        roi_list_texts = str(roi_list) if roi_list else None
        #2023-07-19_18_47_39_16.jpg
        imgname = datetime.now().strftime("%Y-%m-%d_%H_%M_%S_%f")+"_{}.jpg".format(res_label)
        #modify_exif_v2(img, boxes_text, ori_img_path+"/"+imgname,roi_list_texts)
        if roi_list: #[[[19.2, 32.4], [1881.6, 32.4], [1881.6, 1036.8], [19.2, 1036.8], [19.2, 32.4]]]
            for roi in roi_list:
                plot_one_roi(roi, img,tl2)
        if i:
            for j in i:
                draw_border(j, img, tl1, tl2)
                #plot_one_box(j,img)

        if invalids:
            points = [np.array(invalid) for invalid in invalids]
            # if img.shape[0]<640:
            #     ori_h, ori_w = 480, 640

            draw_mask(points, tl2, img, ori_h, ori_w)

            # points = np.array(invalids) if len(invalids) == 1 else [np.array(invalid) for invalid in invalids]
            # zeros = np.zeros((ori_h, ori_w,3),dtype=np.uint8)
            # mask = cv2.fillPoly(zeros, points, (98,9,11))
            # img = 0.9 * mask + img
        #zeros = np.zeros((),dtype=uint8)

        # Keep the source frame dimensions: enlarging a sub-stream only makes
        # compression artifacts larger and cannot add usable alarm detail.
        resized = img
        pic_url = img_path+"/"+imgname
        cv2.imwrite(pic_url, resized, [int(cv2.IMWRITE_JPEG_QUALITY),90]) if invalids else lxz_imwrite(pic_url, resized)
        # print("保存图片用时 ：",time.time()-t0)
    
        image_prefix = os.getenv("DUCHEN_ALARM_IMAGE_URL_PREFIX", "/alarm_picture").rstrip("/")
        image_root = os.path.dirname(os.path.dirname(img_path))
        image_relative_path = os.path.relpath(pic_url, image_root).replace(os.sep, "/")
        image_url = f"{image_prefix}/{image_relative_path}"
        result_params = {"id":camera_id,"data":[{"key":"","value":res_label,"grade":alarm_grade,"isRotate":detectmode,"targetCount":targetcount,"time":datetime.now().strftime("%Y-%m-%d %H:%M:%S"),"picUrl":image_url,"videoUrl":""}]}
    #{'id': '00000000000000000001', 'data': {'key': '', 'value': 4, 'grade': '4', 'time': '', 'picUrl': '/home/super/AI_Service/alarm_picture/download/image/192.168.1.78/2022-12-14_10_57_56_4.jpg', 'videoUrl': ''}} 
        if res_label == 18:
            result_params["data"][0]["carNumber"] = i[0][-1] 
        mqtt_client.publish("alarm_result",json.dumps(result_params))
        logger.info(result_params)

def draw_mask(pts, tl2, img, ori_h, ori_w):
    transparency = 5 * tl2
    zeros = np.zeros((ori_h, ori_w,3),dtype=np.uint8)
    zeros2 = zeros.copy()
    for i in range(int(max(zeros.shape) / transparency * 2)):
        cv2.line(zeros, (0, transparency * i), (transparency * i, 0), color=(255,255,255), thickness=tl2)
    cv2.polylines(zeros,pts,True,(255,255,255))
    for j in pts:
        cv2.fillPoly(zeros2, [j], (1,1,1))
    zeros*= zeros2
    img *= np.array((1 - zeros/255),dtype=np.uint8)
    img += np.array((zeros*0.47),dtype=np.uint8)



def draw_border(boxes, img, tl1, tl2 ,color =(35, 22, 201)): #京东红
    x1, y1, box_w, box_h = boxes[2:6]
    x4, y4 = x1 + box_w, y1 + box_h
    x2, y2, x3, y3 = x1, y4, x4, y1  #左下， 右上
    # center_x,  center_y = int((x4+x1)/2), int((y4+y1)/2) #  center
    #center_x,  center_y = int(x1 + box_w/2), int(y1 + box_h/2)
    #cv2.circle(img, (center_x, center_y), 3, color, -1)    
    
    line_length = int(min(box_h, box_w) / 7)
    # print(line_length)
    # cv2.circle(img, (x1, y1), 3, (255, 0, 255), -1)    #  top_left
    # cv2.circle(img, (x2, y2), 3, (255, 0, 255), -1)    #  bottom-left
    # cv2.circle(img, (x3, y3), 3, (255, 0, 255), -1)    #  top-right
    # cv2.circle(img, (x4, y4), 3, (255, 0, 255), -1)    #  bottom-right

    cv2.line(img, (x1, y1), (x1 , y1 + line_length), color, tl1)  #  top-left
    cv2.line(img, (x1 , y1 + line_length),(x2 , y2 - line_length), color, tl2)

    cv2.line(img, (x1, y1), (x1 + line_length , y1), color, tl1)
    cv2.line(img, (x1 + line_length , y1),(x3 - line_length, y3), color, tl2)
    
    cv2.line(img, (x2, y2), (x2 , y2 - line_length), color, tl1)  #  bottom-left
    cv2.line(img, (x2, y2), (x2 + line_length , y2), color, tl1)
    cv2.line(img, (x2 + line_length , y2),(x4 - line_length , y4), color, tl2)
    cv2.line(img, (x3, y3), (x3 - line_length, y3),color, tl1)  #  top-right
    cv2.line(img, (x3, y3), (x3, y3 + line_length),color, tl1)
    cv2.line(img, (x3, y3 + line_length),(x4 , y4 - line_length), color, tl2)
    cv2.line(img, (x4, y4), (x4 , y4 - line_length), color, tl1)  #  bottom-right
    cv2.line(img, (x4, y4), (x4 - line_length , y4), color, tl1)


def lxz_drawline(img,pt1,pt2,color,thickness,style='dotted',gap=10): #画虚线 gap越小越密
    dist =((pt1[0]-pt2[0])**2+(pt1[1]-pt2[1])**2)**.5 
    pts= [] 
    for i in np.arange(0,dist,gap): 
        r=i/dist 
        x=int((pt1[0]*(1-r)+pt2[0]*r)+.5) 
        y=int((pt1[1]*(1-r)+pt2[1]*r)+.5) 
        p = (x,y) 
        pts.append(p) 
    #print(len(pts))
    if style=='dotted': 
        for p in pts: 
            cv2.circle(img,p,thickness,color,-1) 
    else: 
        s=pts[0] 
        e=pts[0] 
        i=0 
        for p in pts: 
            s=e 
            e=p 
            if i%2==1: 
                cv2.line(img,s,e,color,thickness) 
            i+=1 


def plot_one_roi(x,img,tl2):
    for i in range(len(x)-1):
        lxz_drawline(img, x[i], x[i+1], (255,255,0),tl2) #葬蓝色
        #cv2.line(img, x[i], x[i+1], color=(255, 255, 0), thickness=2) #天蓝色
    lxz_drawline(img, x[i+1], x[0], (255,255,0),tl2) #葬蓝色


def mk_img_path(img_path):
    os.makedirs(img_path, exist_ok=True)
    # The Python worker may run as root while the Java service deletes as forlinx.
    # Keep camera directories writable by both services.
    try:
        os.chmod(img_path, 0o777)
    except OSError:
        logger.warning("cannot make alarm image directory writable: %s", img_path)


def plot_one_box(x, img):
    c1, c2 = (x[2],x[3]), (x[4]+x[2],x[5]+x[3])
    #cv2.putText(img,str(label),(x[2],x[3]),cv2.FONT_HERSHEY_COMPLEX_SMALL,tl,color,1)
    cv2.rectangle(img, c1, c2, color=(0,255,0), thickness=2, lineType=cv2.LINE_AA) #绿色


def req_ai_server(json_data):
    requests.post(url="http://0.0.0.0:8899/analyresult",json=json_data)#.json()
    #if resp.status_code != 200:
    #    print(resp.json())

def req_ai_server2(json_data,b_img):
    f = {"image":("example.jpg",b_img,"image/jpg",{})}
    resp = requests.post(url="http://0.0.0.0:8899/analyresult",files=f)
'''
def remove_small(boxes, min_wh_dict, ori_h, ori_w):
    new_boxes = deepcopy(boxes)
    for i in range(len(new_boxes)):
        for j in range(len(new_boxes[i])):
            min_w, min_h = min_wh_dict.get(new_boxes[i][j][0])
            if new_boxes[i][j][4] < min_w*ori_w and new_boxes[i][j][5] < min_h*ori_h:
                boxes[i].remove(new_boxes[i][j])
    final_boxes = list(filter(None, boxes))
    return final_boxes
'''

def split_result(list01, list02):
    if len(list01) == 0:
        return list02
    if len(list01) == 1:
        list02.append(list01)
        return list02
    b = list01[1:]
    res = list01[:1]
    for i in range(len(b)-1,-1,-1):
        if b[i][0] == list01[0][0]:
           res.append(b[i])
           del b[i]
    list02.append(res)
    return split_result(b,list02)


def str2np(bs64):
    image = base64.b64decode(bs64)
    np_img = jpeg.decode(image, pixel_format=TJPF_BGR)
    return np_img


def get_stream_params(url: str):
    cmd = ['/usr/local/ffmpeg/bin/ffprobe',
        '-timeout', '2000000', 
        '-v', 'quiet',
        '-print_format', 'json',
        '-show_format', '-show_streams', url]
    proc = Popen(cmd, stdout=PIPE)
    stdout = proc.communicate()[0]
    bio = BytesIO(stdout)
    json_out = json.load(bio)
    params = {}
    if not 'streams' in json_out:
        return {}
    for stream in json_out['streams']:
        if stream['codec_type'] == 'video':
            params['width'] = stream['width']
            params['height'] = stream['height']
            params['framerate'] = int(eval(stream['r_frame_rate'])) #基本帧率
            params['codec'] = stream['codec_name']
            return params
    return {}


def modify_exif(imgname, img_text, ori_imgname):
    with open(imgname, 'rb') as f:
       img_bytes = f.read()
    new_img = Image(img_bytes)
    new_img.image_description = img_text
    with open(ori_imgname,'wb') as f2:
       f2.write(new_img.get_file())


def modify_exif_v2(img, img_text, ori_imgname, roi_text=None):
    """
    img : (np.ndarray)
    img_text : (str) example: '[[300,987,3232,333]]'
    roi_text : (str) example: '[[200,3232,2321,12312321,213213]]'
    ori_imgname: (str)

    """

    #img_encode = cv2.imencode('.jpg', img)[1]
    #img_bytes = img_encode.tobytes()
  
    img_bytes = jpeg.encode(img, quality=50)
    with open(ori_imgname,'wb') as f:
        img_bytes = Image(img_bytes)
        if roi_text:
            img_bytes.software = roi_text
        if img_text:
            img_bytes.image_description = img_text
        f.write(img_bytes.get_file())   


def lxz_imwrite(pic_url, img):
    with open(pic_url,"wb") as f:
        f.write(jpeg.encode(img,quality=90))


def np2bs64(img):
    b_img = jpeg.encode(img, quality=90)
    result = str(base64.b64encode(b_img))[2:-1]
    return result

class elec_fence_py:
    '''
    python电子围栏
    '''
    def __init__(self,roi):
        self.roi = roi   #.copy()
        #self.roi.append(self.roi[0])
    
    def detect(self, point):
        px = point[0]
        py = point[1]
        sum = 0
        length = len(self.roi)-1

        for index in range(0,length):
            sx = self.roi[index][0]
            sy = self.roi[index][1]
            tx = self.roi[index+1][0]
            ty = self.roi[index+1][1]
            #点与多边形顶点重合或在多边形的边上
            if((sx - px) * (px - tx) >= 0 and (sy - py) * (py - ty) >= 0 and (px - sx) * (ty - sy) == (py - sy) * (tx - sx)):
                return True
            #点与相邻顶点连线的夹角
            angle = atan2(sy - py, sx - px) - atan2(ty - py, tx - px)

            #确保夹角不超出取值范围（-π 到 π）
            if(angle >= pi):
                angle = angle - pi * 2
            elif(angle <= -pi):
                angle = angle + pi * 2
            sum += angle

            #计算回转数并判断点和多边形的几何关系
        result = False if int(sum / pi) == 0 else True
        return result

#从文件读取json
def file2json(json_path):
    with open(json_path, "r", encoding="utf-8") as f:
        #f.flush()
        json_data = json.load(f)
    return json_data

def json2file(json_path,json_data):
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(json_data, f,ensure_ascii=False)
        f.flush()

def camera2task(camera_json):
    camera_dict = {}
    for i in camera_json:
        camera_id = i.pop("camera_id")
        camera_dict[camera_id] = i
    return camera_dict

def get_system_config_info(cameracfg_path):
    
    #cameracfg_path = "/home/super/文档/python/yh/config_file/SystemCfg.json"
    
    # 读取json文件内容,返回字典格式
    json_data = file2json(cameracfg_path)
    system_config = list(json_data.values())


def filter_algorithm(weekday_html, ai_dict, ori_algorithm_switch, ori_statistical_states):
    statistical_states = deepcopy(ori_statistical_states)
    algorithm_switch = deepcopy(ori_algorithm_switch)
    for out_label, valid_time in ai_dict.items():
        if out_label == 17:
            continue
        html_weekday = weekday_html[datetime.now().weekday()]
        start_validtime = 0 
        for ss_time in valid_time["valid_time"].get(html_weekday):
            start_time, stop_time = ss_time.split("-")
            d_start = datetime.strptime(start_time,'%H:%M').strftime('%H:%M')
            d_end = datetime.strptime(stop_time,'%H:%M').strftime('%H:%M')
            time_interval = Interval(d_start,d_end,up_closed=False)
            in_validtime = datetime.now().strftime('%H:%M:%S') in time_interval
            start_validtime |= in_validtime
        if not start_validtime:#self.not_in_validtime(d_start, d_end):
            if out_label in (0,1):
                del algorithm_switch[0][out_label]
            elif out_label in (2,3,16):
                del algorithm_switch[1][out_label]
            else:
                algorithm_switch[out_label] = 0 
                statistical_states.pop(out_label,None)
    return algorithm_switch, statistical_states


def init_args(weekday_html, group_id, group_path="/home/super/AI_Service/config_file/GroupTaskCfg.json", cameracfg_path="/home/super/AI_Service/config_file/CameraCfg.json"):
    group_json = file2json(group_path).get(group_id)
    if not group_json:
        return group_json, None
    camera_json = file2json(cameracfg_path)
    args, polling_period = mk_args(weekday_html, group_json, camera_json)
    logger.info("轮询组ID {}参数初始化完成".format(group_id))
    return args, polling_period
 
    
    
def mk_args(weekday_html, group_json, camera_json):
    is_ltfg = 0
    args = []
    ori_ai_dict = group_json.get("algorithm_cfg")
    run_status = group_json.get("run_status")
    if not ori_ai_dict or not run_status:
        return None, None
    ai_dict = {}
    for ori_k, ori_v in ori_ai_dict.items():
        ori_v['alarm_Interval_period'] = 0
        ori_v['algorithm_threshold'] = float(ori_v['algorithm_threshold']) * 0.01
        ori_v['target_threshold'] = int(ori_v['target_threshold'])
        ai_dict[int(ori_k)] = ori_v

    '''
            {'4': {'alarm_grade': '0', 'alarm_type': '0', 'alarm_period': '0', 'alarm_Interval_period': '0', 'algorithm_threshold': '75.0', 'valid_time': {'0': ['06:00-18:00'], '1': ['06:00-18:00'], '2': ['06:00-18:00'], '3': ['06:00-22:00'], '4': ['06:00-18:00'], '5': ['06:00-18:00'], '6': ['06:00-18:00']}
    
     {4: {'alarm_Interval_period': 3, 'algorithm_threshold': 0.75, 'valid_time': {'0': ['06:00-18:00'], '1': ['06:00-18:00'], '2': ['06:00-18:00'], '3': ['06:00-22:00'], '4': ['06:00-18:00'], '5': ['06:00-18:00'], '6': ['06:00-18:00']}, 'alarm_grade': '1'}} [480, 640] [{}, {}, [0, 2], 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
    '''
    statistical_states = {}
    #马莹莹修改算法种类
    # algorithm_switch = [0] * 16 #目前有15种算法
    algorithm_switch = [0] * 29   #目前有28种算法
    ai_list = [*ai_dict.keys()]
    first_classes = [0,2] if len({0,1,4,5}&set(ai_list)) > 0 else []
    for algorithm_type in ai_list:
        if algorithm_type == 16:
            continue
        if algorithm_type == 17:
            is_ltfg = 1
            continue
        algorithm_type = int(algorithm_type)
        algorithm_switch[algorithm_type] = 1
        if algorithm_type in (10,11,12,13):
            statistical_states[algorithm_type] = ai_dict.get(algorithm_type)

    yolox_ai_dict_1 = {k: v for k, v in ai_dict.items() if k in (0,1)}
    yolox_ai_dict_2 = {k: v for k, v in ai_dict.items() if k in (2,3,16)}

    algorithm_switch[0] = yolox_ai_dict_1
    algorithm_switch[1] = yolox_ai_dict_2
    #0-抽烟 1-打电话 2-明火 3-安全带 4-没安全帽 5-安全帽 6-没反光衣 7-反光衣 8-口罩 9-没口罩 10-离岗 11-单人作业 12-人员入侵 13-人员聚集 14-跌倒, 15-睡岗
    algorithm_switch[2] = first_classes
    
    #every_algorithm_switch, s_dict = filter_algorithm(weekday_html, ai_dict, algorithm_switch, statistical_states)
    polling_period = group_json["polling_period"]
    cameras = group_json["camera_info"]
    camera_dict = camera2task(camera_json)
    for camera_id, rois_dict in cameras.items():
        camera_info = camera_dict[camera_id]
        sub_type = camera_info["sub_type"]
        rtsp_url = camera_info["rtsp_url"] #main2sub(camera_info["rtsp_url"],camera_info["factory"]) if camera_info["sub_type"]== "main" else camera_info["rtsp_url"]
        camera_ip = camera_info['ip']
        ori_h,ori_w = camera_info.get('height'),camera_info.get('width')
        if not ori_h or not ori_w:
            stream_params = get_stream_params(rtsp_url)
            if not stream_params:
                continue
            ori_h, ori_w = stream_params.get("height"),stream_params.get("width")
        line1, line2, is_resize = 2,1,True
        if sub_type == "main" or (sub_type == "other" and ori_h > 720 and ori_w > 1080):
           line1*= 4
           line2*= 3
           is_resize = False
        ori_hw = [ori_h, ori_w] #yuanshi
        #1080 1920
        #camera_info["height"],camera_info["width"],camera_info["codec"] = ori_h, ori_w, codec
        #camera_dict[camera_id] = camera_info
        # gain = min(640 / ori_w, 640 / ori_h)  # gain  = old / new
        # pad = (640 - ori_w * gain) / 2, (640 - ori_h * gain) / 2 
        
        # # (invalid_rois big-> small)  size:(*x640)
        # ori_h, ori_w = ori_hw
        # p_ori_w = 640
        # gain = p_ori_w / ori_w  # gain  = old / new
        # p_ori_h = int(ori_h*gain)
        # # gain = min(p_ori_w / ori_w, p_ori_h / ori_h)  # gain  = old / new
        # # pad = int((p_ori_w - ori_w * gain) / 2), int((p_ori_h - ori_h * gain) / 2)  # wh padding
        # ori_hw = p_ori_h, p_ori_w
        every_ai_dict = deepcopy(ai_dict)
        every_s_s = deepcopy(statistical_states)
        #print(rois_dict)
        for rois_k, rois_v in rois_dict.items():
            rois_k_int = int(rois_k)
            invalid_rois = [[list(map(lambda a,b:int(a*b),roi_point,ori_hw[::-1])) for roi_point in invalid_roi[:-1]] for invalid_roi in rois_v["invalid_roi"]]
            if rois_k_int in (10,11,12,13):
                every_s_s[rois_k_int]["invalid_rois"] = invalid_rois 
            every_ai_dict[int(rois_k)]["invalid_rois"] = invalid_rois
            rois_dict[rois_k]["invalid_roi"] = invalid_rois
            rois_dict[rois_k]["camera_roi"] = [[list(map(lambda a,b:int(a*b),roi_point,ori_hw[::-1])) for roi_point in camera_roi[:-1]] for camera_roi in rois_v["camera_roi"]]
        #rtsp_url = main2sub(camera_info["rtsp_url"],camera_info["factory"]) if camera_info["sub_type"]== "main" else camera_info["rtsp_url"]
        # args.append((rtsp_url, weekday_html, every_ai_dict, rois_dict, algorithm_switch, every_s_s, camera_id, camera_ip, ori_hw, gain, pad, line1, line2, is_resize))
        
        args.append((rtsp_url, weekday_html, every_ai_dict, rois_dict, algorithm_switch, every_s_s, camera_id, camera_ip, ori_hw, line1, line2, is_resize, is_ltfg))
    ''' 
    camera_dict_list = []
    for camera_k, camera_v in camera_dict.items():
        camera_v["camera_id"] = camera_k
        camera_dict_list.append(camera_v)
    with open("/home/super/AI_Service/config_file/CameraCfg1.json","w",encoding="utf-8") as json_f:
        json.dump(camera_dict_list,json_f,ensure_ascii=False)
    '''
    return args, polling_period

def lxz_occlusion(img, row_num=5, col_num=5):
    img = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    block_row = np.array_split(img,row_num,axis=0)
    img_blocks,total_score = [],0
    for block in block_row:
        block_col = np.array_split(block,col_num,axis=1)
        for j in block_col:
             score = np.var(j)
             if score < 100:
                 total_score += 1
    is_occlusion = True if total_score > 9 else False
    return is_occlusion


def bin2np(bindata):
    arr = jpeg.decode(bindata)
    return arr


def np2bin(np_img):
    b_img = jpeg.encode(np_img, quality=50)
    return b_img

if __name__ == "__main__":
    show_apu_status()
