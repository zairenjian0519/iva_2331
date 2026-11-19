<template>
  <div class="video-container">
    <a-row class="video-list" type="flex" justify="start">
      <a-col :span="4">
        <div class="title">实时监控</div>
        <div class="area">
          <div class="select-num">已选择相机{{ mainState.selectCamera?.length }}/{{ mainState.cameraSum }}</div>
          <tab-control :title="mainState.tab" @click-item="handleClickItem" />
          <left-tree v-if="mainState.treeData.length > 0" :tree-data="mainState.treeData" type="type" :is-disable="mainState.isDisable" :is-auto-expand="isAutoExpand" @handle-check="handleCheckTree">
            <span class="tips">
              <base-tooltip tooltip-title="实时监控路数已达上限(16路)" v-if="mainState.disabled"> <i class="warn-icon"></i> </base-tooltip>
            </span>
          </left-tree>
        </div>
      </a-col>
      <a-col :span="20">
        <secondary-button class="refresh-btn" @click="handleRefresh" label="刷新">
          <i class="refresh-icon"></i>
        </secondary-button>
        <template v-if="mainState.selectCamera.length === 0">
          <a-empty description="暂无数据" />
        </template>
        <template v-else>
          <div class="camera-container" v-show="mainState.isShowList">
            <a-row :wrap="true" class="camera-list">
              <a-col :span="6" class="detail-box" v-for="(item, index) in mainState.selectCamera" :key="item.key" @dblclick="handleExpandImg(item)">
                <template v-if="item.runStatus === '0'">
                  <OfflineCamera :abnormalName="item.title" @to-camera-edit="cameraEdit(item)" :element="item" />
                </template>
                <template v-else>
                  <div v-show="mainState.isLoading" class="loading">
                    <!-- <img :src="item.pic" alt="" class="loading-pic" /> -->
                    <i class="loading-image" alt=""></i>
                  </div>
                  <video
                    muted
                    :id="'ve_' + item.key"
                    :ref="(el) => (videoElement[item.key] = el)"
                    class="video"
                    v-show="!mainState.isLoading"
                    :poster="item.pic"
                    @Play="mainState.isLoading = false"
                  ></video>
                  <div class="modal">
                    <div class="camera-info">
                      <span class="camera-title">{{ item.title }}</span>
                    </div>
                  </div>
                </template>

                <camera-modal ref="editCamera" @handleOk="handleOfflineOk" />
              </a-col>
            </a-row>
          </div>
        </template>
        <!-- 单图放大 -->
        <div class="enlarge-box" v-show="!mainState.isShowList">
          <!-- <div v-show="mainState.isEnlargeLoading" class="loading">
            <img :src="mainState.enlargeMsgInfo.pic" alt="" class="loading-pic" />
            <i class="loading-image" alt=""></i>
          </div>
          <video muted id="_expand_" :ref="(el) => (videoElement['_expand_'] = el)" class="video" v-show="!mainState.isEnlargeLoading"></video> -->
          <div class="loading" v-show="mainState.isEnlargeLoading">
            <i class="loading-image" alt=""></i>
          </div>
          <div class="video-mask" v-show="!mainState.isPlay"></div>
          <video
            muted
            :id="`_expand_${d}`"
            :ref="(el) => (videoElement['_expand_'] = el)"
            class="video-expand"
            :poster="mainState.element.pic"
            @Canplay="handleCanPlay"
            @Play="mainState.isEnlargeLoading = false"
          ></video>
          <i class="start-play" @click="changeVideoStatus" v-show="!mainState.isPlay"></i>
          <div class="operation">
            <i class="back-icon" @click="handleshowList"></i>
            <i class="enlarge-icon" @click="handleFold"></i>
          </div>
          <div class="mask">
            <div class="info">
              <span class="name">{{ mainState.enlargeMsgInfo.title }}</span>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
  import LeftTree from './components/LeftTree.vue';
  import { Col, Row, message } from 'ant-design-vue';
  import { reactive, onMounted, ref, onUnmounted } from 'vue';
  import TabControl from '../../../components/TabControl/TabControl.vue';
  import OfflineCamera from '/@/components/OfflineCamera/OfflineCamera.vue';
  import CameraModal from '../../base/cameraManager/components/CameraModal.vue';
  import BaseTooltip from '../../../components/BaseTooltip/BaseTooltip.vue';
  import { cameraTreeData } from './playVideoList.api';
  import { CameraTreeData, handleRawData, TreeNodeData, backField, IRawTreeNodeData } from './playVideoList.data';
  import SecondaryButton from '../../../components/BaseButton/SecondaryButton.vue';
  import { showView } from './playVideoList.api';
  import axios from 'axios';
  import { JSWebrtc, Player } from '/@/utils/player/jswebrtc';
  import { fork } from 'child_process';

  const ACol = Col;
  const ARow = Row;
  const isAutoExpand = ref(true);
  const videoElement = ref({});

  const editCamera = ref<HTMLElement | null>(null);
  let playerInterval: NodeJS.Timer | null = null;
  let heartInterval: NodeJS.Timer | null = null;

  const playerMap = new Map<string, Player>();

  const mainState = reactive({
    tab: ['超脑', '区域'],
    treeObj: {} as CameraTreeData,
    treeData: [] as TreeNodeData[],
    //已选择相机
    selectCamera: [] as TreeNodeData[],
    //总的相机数
    cameraSum: 0 as number,
    videoUrl: '',
    cameraId: '',
    deviceIp: '',
    isShowModel: false as Boolean,
    cameraTitle: '编辑相机',
    element: {} as IRawTreeNodeData,
    //是否放大
    isShowList: true as Boolean,
    streamType: '0' as string,
    enlargeMsgInfo: {} as TreeNodeData,
    currentIndex: 0 as number,
    isLoading: true as Boolean,
    isEnlargeLoading: false as Boolean,
    num: 0 as number,
    //webrtc视频流地址
    webrtcUrl: '' as string,
    isDisable: false as boolean,
    allCamera: [] as TreeNodeData[],
    //是否禁用
    disabled: false as boolean,
    isPlay: false as boolean,
    viewUrl: '' as string,
    hostUrl: window.location.host,
    hostIp: '' as string,
  });

  //从环境变量里取url 非本地测试取浏览器地址栏ip 本地测试取配置文件ip
  let ip = '';
  //非本地测试 取浏览器地址栏ip
  if (window.location.host != 'localhost:3100') {
    mainState.hostIp = window.location.hostname;
    ip = window.location.hostname;
  } else {
    //本地测试取配置文件
    const hostIp: any = import.meta.env.VITE_GLOB_DOMAIN_URL;
    let startIndex: number = hostIp.lastIndexOf('//');
    let lastIndex: number = hostIp.lastIndexOf(':');
    //得到ip
    ip = hostIp.substring(startIndex + 2, lastIndex);
  }

  //保存最后一次选中的节点
  let selectItem: IRawTreeNodeData = {};
  //放大后播放
  let expandPlayer: Player = {};
  //所有相机的map
  let allCameraSet = new Set();
  let selectCameraSet = new Set();
  onMounted(async () => {
    const cameraTreeRes = await cameraTreeData({});
    mainState.treeObj = handleRawData(cameraTreeRes);
    mainState.treeData = mainState.treeObj.deviceTree;

    console.log(mainState.treeObj.deviceTree, 222);
    for (let i = 0; i < mainState.treeObj.deviceTree.length; i++) {
      for (let j = 0; j < mainState.treeObj.deviceTree[i].children?.length; j++) {
        const picPath = mainState.treeObj.deviceTree[i].children[j].pic;
        const newStr = '/kyscene' + picPath + '?' + mainState.treeObj.deviceTree[i].children[j].deviceIp;
        mainState.treeObj.deviceTree[i].children[j].pic = newStr;
        // console.log(mainState.treeObj.deviceTree[i].children[j].pic, 444);
      }
    }
    for (let i = 0; i < mainState.treeObj.locationTree.length; i++) {
      for (let j = 0; j < mainState.treeObj.locationTree[i].children?.length; j++) {
        const picPath = mainState.treeObj.locationTree[i].children[j].pic;
        const newStr = '/kyscene' + picPath + '?' + mainState.treeObj.locationTree[i].children[j].deviceIp;
        mainState.treeObj.locationTree[i].children[j].pic = newStr;
        // console.log(mainState.treeObj.locationTree[i].children[j].pic, 555);
      }
    }
    if (mainState.currentIndex === 0) {
      mainState.treeObj.deviceTree.map((item) => {
        mainState.cameraSum += item.cameraCount;
      });
    } else {
      mainState.treeObj.locationTree.map((item) => {
        mainState.cameraSum += item.cameraCount;
      });
    }
    mainState.treeData.forEach((item) => {
      if (!item.parentKey) {
        item.disabled = true;
      }
    });
    mainState.allCamera.length = 0;
    //筛选出所有相机
    mainState.treeData.forEach((item) => {
      if (item.children) {
        item.children.forEach((ele) => {
          if (ele.type === 'camera') {
            mainState.allCamera.push(ele);
            allCameraSet.add(ele.key);
          }
        });
      }
    });
    //打开十六分屏
    firstScreen();
  });

  onUnmounted(() => {
    if (playerInterval) {
      clearInterval(playerInterval);
    }
    if (heartInterval) {
      clearInterval(heartInterval);
    }
  });

  //进入页面，每两秒发一次心跳
  const initHeartBeat = () => {
    if (heartInterval) {
      clearInterval(heartInterval);
    }
    heartInterval = setInterval(() => {
      heartBeat(ip);
      // console.log('时间：'+new Date())
    }, 500);
    //不知道具体播放时间加延时
    setTimeout(() => {
      mainState.isLoading = false;
      mainState.isEnlargeLoading = false;
    }, 5000);
  };

  //打开十六分屏实时监控页面
  const firstScreen = async () => {
    // try {
    const splitUrl = 'http://' + ip + ':8899/firstscreen';
    try {
      const playRes: backField = await axios.get(splitUrl, {});
      if (playRes.data.code === 2000) {
        initHeartBeat();
        // message.success(playRes.data.msg);
      } else if (playRes.data.code === 2005) {
        //不能再进行勾选相机操作
        mainState.isDisable = true;
        message.error(playRes.data.msg);
      }
    } catch (e) {
      console.log('请求失败');
    }
  };

  const handleCanPlay = () => {
    if (!mainState.isPlay) return;
    mainState.isEnlargeLoading = false;
    mainState.isPlay = true;
    expandPlayer.play();
  };
  let d = new Date().getTime();

  const changeVideoStatus = () => {
    //视频播放按钮消失
    mainState.isPlay = true;
    if (expandPlayer.options.video.readyState === 4) {
      expandPlayer.play();
    } else {
      //开始出现loading
      mainState.isEnlargeLoading = true;
    }
  };

  //点击tab切换树
  const handleClickItem = (index: number) => {
    mainState.currentIndex = index;
    if (index === 0) {
      mainState.treeData = mainState.treeObj.deviceTree;
    } else {
      //切换树
      mainState.treeData = mainState.treeObj.locationTree;
      mainState.treeData.forEach((item) => {
        if (!item.parentKey) {
          item.disabled = true;
        }
      });
    }
  };
  //离线相机弹窗
  const cameraEdit = (ele) => {
    mainState.element = ele;
    mainState.isShowModel = true;
    editCamera.value?.initCameraModel({ isUpdate: true, datas: ele });
  };
  //离线相机点击
  const handleOfflineOk = () => {
    mainState.isShowModel = false;
  };

  //刷新
  const handleRefresh = () => {
    playerMap.forEach((val, key) => {
      val.play();
    });
  };
  const createFlv = (deviceIp, cameraId) => {
    let url = 'webrtc://' + deviceIp + '/live/page' + cameraId;
    if (mainState.hostUrl != 'localhost:3100') {
      url = 'webrtc://' + ip + ':1985/live/page' + cameraId;
    }
    const option = {
      video: videoElement.value[cameraId],
      autoplay: true,
    };

    let player = new JSWebrtc.Player(url, option);

    //创建一次player,构建一个map
    playerMap.set(cameraId, player);
    initHeartBeat();
  };

  async function heartBeat(deviceIp) {
    // const url = 'http://' + deviceIp + ':8899/heartbeat';
    const url = '/kyscene/heartbeat?' + deviceIp;
    mainState.num++;
    try {
      const res: backField = await axios.post(url, {
        heart_num: mainState.num,
      });
      if (res.data.code === 2000) {
        // message.success('请求成功');
      } else if (res.data.code === 2001) {
        console.log(res.data.msg);
      }
    } catch (error) {
      console.log('请求失败');
    }
  }

  //处理公共的方法
  const playVideo = () => {
    mainState.selectCamera.forEach(async (ele) => {
      if (selectItem.key === ele.key) {
        mainState.cameraId = ele.key;
        createFlv(mainState.deviceIp, mainState.cameraId);
      }
    });
  };

  //开启视频流
  const startScreen = async (item) => {
    const res = await showView({ cameraId: item.key, streamType: mainState.streamType });
    if (res) {
      mainState.deviceIp = res.deviceIp;
    }
    const videoParams = {
      camera_id: item.key,
      rtsp_url: item.streamAddress,
    };

    // const url = 'http://' + mainState.deviceIp + ':8899/startscreen';
    const url = '/kyscene/startscreen?' + mainState.deviceIp;

    try {
      const rtspRes: backField = await axios.post(url, videoParams);

      if (rtspRes.data.code == 2000) {
        // message.success('正在加载...');
        mainState.isLoading = false;
      } else if (rtspRes.data.code === 2006) {
        message.error(rtspRes.data.msg);
      }
    } catch (error) {
      console.log('请求失败');
    }
  };

  //树的复选框选中
  const handleCheckTree = async (checkedKeys, e) => {
    // mainState.isLoading = true;
    mainState.selectCamera = e.checkedNodes.filter((item) => {
      return item.type === 'camera';
    });

    if (!e.checked) {
      closeInterval(e.node.key);
      // if (playerInterval) {
      //   clearInterval(playerInterval);
      // }
    } else {
      //非全选
      if (e.checkedNodes[e.checkedNodes.length - 1].type === 'camera') {
        selectItem = e.checkedNodes[e.checkedNodes.length - 1];
        await startScreen(e.checkedNodes[e.checkedNodes.length - 1]);
      }

      await playVideo();
    }

    //先清空集合
    selectCameraSet.clear();
    //差集集合
    let difference;
    mainState.selectCamera.forEach((s) => {
      selectCameraSet.add(s.key);
    });
    //如果选中16个相机，把所有相机和选中相机的差集置灰
    if (selectCameraSet.size === 16) {
      //找差集
      difference = [...new Set([...allCameraSet].filter((x) => !selectCameraSet.has(x)))];
      mainState.selectCamera.forEach((item, index) => {
        if (index === 15) {
          //第16个勾选置灰，且出现提示
          item.disabled = true;
          mainState.disabled = true;
        }
      });
      mainState.allCamera.forEach((ele) => {
        if (difference) {
          for (const i of difference.values()) {
            if (ele.key === i) {
              //差集不能勾选
              ele.disableCheckbox = true;
            }
          }
        } else {
          ele.disableCheckbox = false;
        }
      });
    } else if (selectCameraSet.size < 16) {
      //选中的小于16随便勾
      mainState.selectCamera.forEach((item, index) => {
        if (index < 15) {
          item.disabled = false;
        }
      });
      mainState.allCamera.forEach((ele) => {
        ele.disableCheckbox = false;
      });
    }
  };

  //单图放大
  const handleExpandImg = async (ele) => {
    mainState.streamType = '0';
    mainState.enlargeMsgInfo = ele;
    mainState.isShowList = false;
    // const params = { cameraId: ele.key, streamType: mainState.streamType };
    mainState.cameraId = ele.key;
    //进入放大页面，先把十六分屏的视频停止
    playerMap.forEach((val, key) => {
      val.pause();
    });
    //loading
    mainState.isEnlargeLoading = true;
    //播放按钮
    mainState.isPlay = true;

    const option = {
      video: videoElement.value['_expand_'],
      // autoplay: true,
    };
    const res = await showView({ cameraId: ele.key, streamType: mainState.streamType });
    if (res) {
      mainState.deviceIp = res.deviceIp;
    }

    // const url = 'webrtc://' + mainState.deviceIp + '/live/page' + ele.key;
    const url = 'webrtc://' + ip + ':1985/live/page' + ele.key;
    expandPlayer = new JSWebrtc.Player(url, option);
    initHeartBeat();
  };

  // 返回列表
  const handleshowList = async () => {
    //销毁之前创建的expandPlayer
    expandPlayer.destroy();
    mainState.isShowList = true;
    //将video的封面属性置为空
    document.querySelector<HTMLElement>('.video-expand')!.poster = '';
    expandPlayer.destroy();
    mainState.isEnlargeLoading = false;
    mainState.isPlay = false;
    //之前选中的相机,全部播
    playerMap.forEach((val, key) => {
      val.play();
    });
  };
  async function closeInterval(key) {
    // const url = 'http://' + mainState.deviceIp + ':8899/stopscreen';
    const url = '/kyscene/stopscreen?' + mainState.deviceIp;

    try {
      const res: backField = await axios.post(url, {
        camera_id: key,
      });
      if (res.data.code === 2000) {
        message.success(res.data.msg);
      } else if (res.data.code === 2007) {
        message.error(res.data.msg);
      }
    } catch (error) {
      console.log('请求失败');
    }
  }
  //全屏
  const requestFullElement = (element) => {
    if (element.requestFullScreen) {
      element.requestFullScreen();
    } else if (element.mozRequestFullScreen) {
      element.mozRequestFullScreen();
    } else if (element.msRequestFullScreen) {
      element.msRequestFullScreen();
    } else if (element.webkitRequestFullScreen) {
      element.webkitRequestFullScreen();
    }
  };

  //单图放大里面的放大图标
  const handleFold = () => {
    //全屏播放
    const ele = document.querySelector('.enlarge-box');
    requestFullElement(ele);
  };
</script>
<style lang="less" scoped>
  .video-container {
    overflow-x: auto;
  }
  .video-list {
    height: calc(100vh - 72px);
    overflow-x: auto;
    min-width: 1200px;
    overflow-y: hidden;
    background-color: var(--background-Color);
    margin: 12px 12px 12px 12px;
    font-family: Source Han Sans CN;
    .area {
      border-right: 1px solid var(--input-text-color);
    }
    .title {
      font-weight: 600;
      color: var(--headerline-text-color);
      font-size: 15px;
      margin: 24px 0 16px 24px;
    }
    .select-num {
      color: var(--thead-text-color);
      font-size: 13px;
      margin: 0 0 12px 24px;
    }
    .divider {
      border-right: 1px solid var(--input-text-color);
      opacity: 60%;
      margin: 4px 16px 16px 16px;
    }
    .refresh-btn {
      margin: 44px 0 12px 24px;
    }
    .refresh-icon {
      background: url('/@/assets/svg/icon/editor_btn_resetxs_default.svg');
      display: inline-block;
      width: 12px;
      height: 12px;
    }
    .ant-empty {
      position: absolute;
      top: 50%;
      left: 40%;
    }
    .tips {
      display: inline-block;
      margin: 0 0 0 4px;
    }
    .warn-icon {
      width: 16px;
      height: 16px;
      display: inline-block;
      background: url('/@/assets/svg/icon/info_icon_tips_red.svg');
    }
    .camera-container {
      padding: 0 16px 0 24px;
    }

    .camera-list {
      display: flex;
      flex-wrap: wrap;
      justify-content: flex-start;
    }
    .detail-box {
      position: relative;
      height: 180px;
    }
    .loading {
      height: 100%;
      width: 100%;
      &-image {
        width: 48px;
        height: 48px;
        position: absolute;
        top: 40%;
        left: 40%;
        background: url('../../../assets/svg/icon/loading.gif');
        display: inline-block;
      }
      &-pic {
        height: 100%;
        width: 100%;
        object-fit: fill;
        filter: blur(2px);
      }
    }
    .enlarge-box {
      height: calc(100vh - 170px);
      background: #000000;
      position: relative;
      margin: 0 32px 80px 24px;

      .video-expand {
        height: 100%;
        width: 100%;
        object-fit: fill;
      }
      .loading {
        height: 100%;
        width: 100%;
        &-image {
          width: 48px;
          height: 48px;
          position: absolute;
          top: 50%;
          left: 50%;
          background: url('../../../assets/svg/icon/loading.gif');
          display: inline-block;
        }
        &-pic {
          height: 100%;
          width: 100%;
          object-fit: fill;
          filter: blur(4px);
        }
      }
      // .loading {
      //   // height: 100%;
      //   // width: 100%;
      //   &-image {
      //     width: 48px;
      //     height: 48px;
      //     position: absolute;
      //     top: 48%;
      //     left: 48%;
      //     background: url('../../../assets/svg/icon/loading.gif') transparent;
      //     display: inline-block;
      //   }
      //   &-pic {
      //     height: 100%;
      //     width: 100%;
      //     // object-fit: fill;
      //   }
      // }
      .video-mask {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: linear-gradient(to bottom, rgba(0, 0, 0, 0.1), rgba(92, 92, 92, 0.3), 50%, rgba(255, 255, 255, 0.1) ;);
        opacity: 0.3;
      }
      .start-play {
        cursor: pointer;
        width: 63px;
        height: 63px;
        border: 3px solid #fff;
        opacity: 0.7;
        border-radius: 50%;
        display: block;
        position: absolute;
        margin: auto;
        top: 0;
        bottom: 0;
        left: 0;
        right: 0;
        &::after {
          content: '';
          width: 0;
          height: 0;
          border: 12px solid transparent;
          border-left: 19px solid #fff;
          position: absolute;
          top: 30%;
          left: 38%;
        }
      }
    }
    .video {
      width: 100%;
      height: 100%;
      border: 1px solid #ccc;
      aspect-ratio: 16/9;
      object-fit: fill;
    }
    .modal {
      opacity: 80%;
      height: 34px;
      background-color: #0b0b0d;
      border-radius: 4px;
      height: 35px;
      font-size: 13px;
      position: absolute;
      width: 90%;
      bottom: 8px;
      left: 18px;

      .camera-info {
        display: flex;
        align-items: center;
        justify-content: space-between;
        z-index: 999;
        height: 34px;
        flex-shrink: 1;
        flex-grow: 0;
        align-items: center;
        justify-content: space-between;
        z-index: 999;
        cursor: pointer;
      }

      .camera-title {
        font-family: Source Han Sans CN;
        font-weight: 500;
        color: rgba(255, 255, 255, 0.95);
        font-size: 13px;
        margin-left: 12px;
      }
    }
    // .enlarge-box {
    //   position: relative;
    //   margin: 0 32px 80px 24px;
    //   width: 100%;
    //   background: var(--netural-color);
    // }
    .operation {
      opacity: 80%;
      width: 80px;
      height: 36px;
      background-color: #0b0b0d;
      border-radius: 4px;
      position: absolute;
      top: 12px;
      left: 12px;
      display: flex;
      align-items: center;
      .back-icon,
      .enlarge-icon {
        display: inline-block;
        width: 24px;
        height: 24px;
        margin-left: 8px;
        margin-right: 8px;
        cursor: pointer;
      }
      .back-icon {
        background: url('/@/assets/svg/icon/media_btn_back.svg');
        &:hover {
          background-color: rgba(255, 255, 255, 0.2);
          border-radius: 4px 0px 0px 4px;
        }
      }
      .enlarge-icon {
        background: url('/@/assets/svg/icon/media_btn_fullscreen.svg');
        &:hover {
          background-color: rgba(255, 255, 255, 0.2);
          border-radius: 4px 0px 0px 4px;
        }
      }
    }
    .mask {
      opacity: 80%;
      width: 98%;
      height: 36px;
      background-color: #0b0b0d;
      border-radius: 4px;
      position: absolute;
      bottom: 8px;
      left: 14px;
      color: #fff;
    }
    .info {
      display: flex;
      align-items: center;
      height: 36px;
      .name {
        margin: 0 0 0 12px;
      }
    }
  }
</style>
