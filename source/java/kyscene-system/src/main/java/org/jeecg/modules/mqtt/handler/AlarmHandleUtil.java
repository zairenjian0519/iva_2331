package org.jeecg.modules.mqtt.handler;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.service.IKyCategoryService;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息处理工具
 */
@Slf4j
public class AlarmHandleUtil {

    /**
     * 读取图片转为base64
     */
    public static String getAlarmImgBase64(String picPath) {
        String base64_head = "data:image/jpg;base64,";
        try {
            BufferedImage image = null;
            if (picPath.contains("http")) {
                if (isFileAccessible(picPath)) {
                    URL url = new URL(picPath);
                    image = ImgUtil.read(url);
                }
            } else {
                image = ImgUtil.read(picPath);
            }
            if (image != null) {
                String base64 = ImgUtil.toBase64(image, ImgUtil.IMAGE_TYPE_JPG);
                return base64_head + base64;
            }
        } catch (Exception e) {
            log.error("告警图片转base64失败：{}", picPath);
        }
        return base64_head;
    }

    /**
     * 获取相机分组信息
     */
    public static String getGroupNames(String cameraLocation) {
        IKyCategoryService categoryService = SpringContextUtils.getBean(IKyCategoryService.class);
        List<KyCategory> kyCategories = categoryService.getTreeLocationById(cameraLocation);
        String groupName = kyCategories.stream().map(KyCategory::getName).collect(Collectors.joining(","));
        if (StrUtil.isNotBlank(groupName)) return groupName;
        return "";
    }

    /**
     * 获取相机分组信息
     * 通过cameraId
     */
    public static String getGroupNamesByCameraId(String cameraId) {
        ICameraManagerService cameraManagerService = SpringContextUtils.getBean(ICameraManagerService.class);
        CameraManager cameraManager = cameraManagerService.getById(cameraId);
        return getGroupNames(cameraManager.getLocation());
    }

    /**
     * 检查http文件地址是否可以访问，判断文件是否存在
     */
    public static boolean isFileAccessible(String httpUrl) {
        try {
            // 创建 URL 对象
            URL url = new URL(httpUrl);

            // 打开连接
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("HEAD"); // 只请求头部信息
            httpURLConnection.connect();

            // 获取响应代码
            int responseCode = httpURLConnection.getResponseCode();

            // 如果响应码是 200 则说明文件存在且可访问
            return responseCode == HttpURLConnection.HTTP_OK;

        } catch (IOException e) {
            // 如果抛出异常，则说明无法访问
            return false;
        }
    }

    /**
     * 简单判断是否是文件路径
     */
    public static boolean isFilePath(String str) {
        return StrUtil.isNotBlank(str) && (str.contains("/") || str.contains("\\"))
                && str.matches(".*\\.[a-zA-Z0-9]{1,5}$");
    }

    // public static void main(String[] args) {
    //     String path = "/home/super/AI_Service/alarm_picture/download/image/192.168.200.164/295a647f-5c78-4e78-8b30-a1344625689b.jpg";
    //     boolean filePath = isFilePath(path);
    //     System.out.println(filePath);
    // }
}
