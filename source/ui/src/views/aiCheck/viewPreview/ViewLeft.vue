<template>
  <div class="main-tabs">
    <div class="wrapper">
      <div class="node">
        <div class="location" v-if="props.locationMsg.length > 0">
          <i class="location-icon"></i>
          <span class="text">当前分组</span>
        </div>
        <a-anchor :affix="false" class="anchor-box">
          <a-anchor-link :href="`${tab.id}`" v-for="tab in props.locationMsg" :key="tab.id"
                         @click.prevent="handleClick($event, tab.id)">
            <template #title>
              <i v-if="tab.isAlarm" class="emergency-icon"></i>
              <span :class="[tab.isRootNode ? 'rootClass' : '']" class="parent">{{
                  tab.parent
                }}</span>
            </template>
          </a-anchor-link>
        </a-anchor>
        <div class="device" v-if="props.deviceMsg.length > 0">
          <i class="device-icon"></i>
          <span class="text">当前设备</span>
        </div>
        <a-anchor :affix="false" class="anchor-box">
          <a-anchor-link :href="`${tab.id}`" :title="`${tab.parent}`" v-for="tab in props.deviceMsg"
                         :key="tab.id" @click.prevent="handleClick($event, tab.id)">
            <template #title>
              <i v-if="tab.isAlarm" class="emergency-icon"></i>
              <span :class="[tab.isRootNode ? 'rootClass' : '']"
                    class="parent">{{ tab.parent.substring(0, 8) }}</span>
            </template>
          </a-anchor-link>
        </a-anchor>
      </div>
      <div class="list">
        <div class="top">
          <template v-if="mainState.isShowList">
            <div class="left">
              <i :class="[mainState.isFold ? 'fold-icon' : 'unfold-icon']" @click="changeFold"></i>
              <p class="divider"></p>
              <span class="title">仅事件</span>
              <BaseSwitch v-model:checked="mainState.checked" @handle-change="handleChange"/>
            </div>
            <div class="right">
              <base-tooltip tooltip-title="大缩略图">
                <template #title>大缩略图</template>
                <img :src="thumbnailmidIcon" class="mid-icon" @click="changeTwo"/>
              </base-tooltip>
              <base-tooltip tooltip-title="小缩略图">
                <img :src="thumbnailminIcon" class="min-icon" @click="changeTree"/>
              </base-tooltip>
            </div>
          </template>
          <template v-else>
            <div class="left">
              <i :class="[mainState.isFold ? 'fold-icon' : 'unfold-icon']" @click="changeFold"></i>
              <div class="label"><span>AI视频监控</span></div>
              <a-select v-if="algStreamEnable" class="select" v-model:value="mainState.value"
                        @change="handleChangeStreamType">
                <a-select-option v-for="item in algDetectionList" :key="item['key']" :value="item['key']">
                  {{ item['value'] }}
                </a-select-option>
              </a-select>
            </div>
          </template>
        </div>

        <!-- 九宫格 -->
        <div class="camera-list-container" v-show="mainState.isShowList">
          <div v-for="(location, ind) in pageCameras.byLocationList" :key="ind">
            <a-row class="camera-list" :gutter="[16, 16]" :wrap="true"
                   v-if="location.cameraLocationList.length > 0">
              <a-col :span="24" :id="`${location.id}`" class="group">
                <i class="location-icon"></i>
                <span class="group-title">{{ location.cameraLocationDict }}</span>
              </a-col>
            </a-row>
            <a-row class="camera-list" :gutter="[16, 16]" :wrap="true">
              <a-col :span="mainState.isTwo ? '12' : '8'" class="camera-detail"
                     v-for="(ele, index) in location.cameraLocationList" :key="index">
                <div class="has-data">
                  <template v-if="ele.cameraStatus === '0'">
                    <OfflineCamera :abnormalName="ele.cameraName" @to-camera-edit="cameraEdit(ele)"
                                   :element="ele"/>
                  </template>
                  <template v-else>
                    <!-- :src="`${ele.pic.indexOf('?')!= -1?ele.pic.split('?')[0]:ele.pic}`" -->
                    <img class="detail-image" @dblclick="expandImg(ele)" v-if="!mainState.isImage"
                         :src="`${ele.pic}?t=${new Date().getTime()}`" @error="handleImageError"/>
                    <!-- <video :src="ele.streamAddress"></video> -->
                    <div class="modal">
                      <div class="camera-info">
                        <span class="camera-title">{{ ele.cameraName }}</span>
                        <div class="alarm-num">
                          <div class="warn" v-if="ele.allCameraCount > 0">
                            <img :src="warningIcon" alt="" class="warn-icon"/>
                            <span class="warn-num">{{ ele.allCameraCount }}</span>
                          </div>
                          <Divider type="vertical"
                                   v-if="ele.allCameraCount > 0 && ele.seriousCameraCount > 0"/>
                          <div class="serious" v-if="ele.seriousCameraCount > 0">
                            <img :src="seriousIcon" alt="" class="serious-icon"/>
                            <span class="serious-num">{{ ele.seriousCameraCount }}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </template>
                </div>
              </a-col>
            </a-row>
          </div>
          <div v-for="(ship, idx) in pageCameras.byOwnershipList" :key="idx">
            <a-row class="camera-list" :gutter="[16, 16]" :wrap="true"
                   v-if="ship.cameraOwnershipList.length > 0">
              <a-col :span="24" :id="`${ship.id}`" class="group">
                <i class="device-icon"></i>
                <span class="group-title">{{ ship.cameraOwnershipDict }}</span>
              </a-col>
            </a-row>
            <a-row class="camera-list" :gutter="[16, 16]" :wrap="true">
              <a-col :span="mainState.isTwo ? '12' : '8'" class="camera-detail"
                     v-for="(ele, index) in ship.cameraOwnershipList" :key="index">
                <div class="has-data">
                  <template v-if="ele.cameraStatus === '0'">
                    <OfflineCamera :abnormalName="ele.cameraName" @to-camera-edit="cameraEdit(ele)"
                                   :element="ele"/>
                  </template>
                  <!-- v-else -->
                  <template v-else>
                    <img class="detail-image" @dblclick="expandImg(ele)" v-if="!mainState.isImage"
                         :src="`${ele.pic}?t=${new Date().getTime()}`" @error="handleImageError"/>
                    <div class="modal">
                      <div class="camera-info">
                        <span class="camera-title">{{ ele.cameraName }}</span>
                        <div class="alarm-num">
                          <div class="warn" v-if="ele.allCameraCount > 0">
                            <img :src="warningIcon" alt="" class="warn-icon"/>
                            <span class="warn-num">{{ ele.allCameraCount }}</span>
                          </div>
                          <Divider type="vertical"
                                   v-if="ele.allCameraCount > 0 && ele.seriousCameraCount > 0"/>
                          <div class="serious" v-if="ele.seriousCameraCount > 0">
                            <img :src="seriousIcon" alt="" class="serious-icon"/>
                            <span class="serious-num">{{ ele.seriousCameraCount }}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </template>
                </div>
              </a-col>
            </a-row>
          </div>
        </div>

        <!-- 单图放大 -->
        <div class="enlarge-box" v-show="!mainState.isShowList">
          <div class="loading" v-show="mainState.isVideoLoading">
            <i class="loading-image" alt=""></i>
          </div>
          <div class="video-mask" v-show="mainState.isPlay"></div>
          <div style="width: 100%; height: 100%" class="videFullDiv">
            <video muted :id="`_expand_${d}`" :ref="(el) => (videoElement['_expand_'] = el)"
                   class="video" @Canplay="handleCanPlay"
                   @Play="mainState.isVideoLoading = false" style="background: #000000"></video>

            <i class="start-play" style="z-index:9999" @click="changeVideoStatus"
               v-show="!mainState.isPlay"></i>
            <div class="operation">
              <i class="back-icon" @click="handleShowList"></i>
              <i class="enlarge-icon" v-show="!mainState.isFullScreen" @click="handleFold"></i>
            </div>

            <real-time-modal :realTimeModalVisible="realTimeModalVisible"
                             :alarm-data="alarmData"
                             @toggleModalShow="openRealTimeModal"/>
          </div>
          <div class="mask">
            <div class="alarm">
              <span class="name">{{ mainState.enlargeMsgInfo.cameraName }}</span>
              <span class="alarm-num">
                <div class="warn" v-if="mainState.enlargeMsgInfo.allCameraCount > 0">
                  <img :src="warningIcon" alt="" class="warning-icon"/>
                  <span class="warning-num">{{ mainState.enlargeMsgInfo.allCameraCount }}</span>
                </div>
                <Divider type="vertical"
                         v-if="mainState.enlargeMsgInfo.allCameraCount > 0 && mainState.enlargeMsgInfo.seriousCameraCount > 0"/>
                <div class="serious" v-if="mainState.enlargeMsgInfo.seriousCameraCount > 0">
                  <img :src="seriousIcon" alt="" class="serious-icon"/>
                  <span class="serious-num">{{ mainState.enlargeMsgInfo.seriousCameraCount }}</span>
                </div>
                <span class="to-exam" @click="handleShowModal"
                      v-if="mainState.enlargeMsgInfo.allCameraCount > 0" v-auth="'alarm:audit'">去审核>></span>
              </span>
            </div>
          </div>
        </div>

        <div class="info" v-if="!mainState.isShowList">
          <div class="info-box">
            <div class="row">
              <span class="title">相机名称</span>
              <span class="name">{{ mainState.enlargeMsgInfo.cameraName }}</span>
            </div>
            <div class="row">
              <span class="title"> 所属设备</span>
              <span class="name">{{ mainState.enlargeMsgInfo.deviceDict }}</span>
            </div>
            <div class="row">
              <span class="title">自定义分组 </span>
              <span class="name">{{ mainState.enlargeMsgInfo.locationDict }}</span>
            </div>
            <div class="row">
              <span class="title">当前算法</span>
              <span class="name">{{
                  mainState.enlargeMsgInfo.alarmTypeDict ? mainState.enlargeMsgInfo.alarmTypeDict : '当前相机暂无算法'
                }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <alarm-modal
      v-model:visible="mainState.isShowModal"
      @val="showModal"
      @show-big-img="goImg"
      @handle-ok="handleOk"
      @handle-cancel="handleCancel"
      :detail-info="mainState.vertifyInfo"
      @handle-back="handleBack"
      @handle-commit="handleCommit"
      :detail-list="mainState.alarmDetailList"
      :index="mainState.currentIndex"
      @go-prev="handleGoPrev"
      @go-next="handleGoNext"
      @handle-filter-serious="handleFilterSerious"
    />
    <camera-modal ref="editCamera" @handleOk="handleOfflineOk"/>

  </div>
</template>
<script lang="ts" setup>
import {Anchor, AnchorLink, Col, Divider, message, Row, Select, SelectOption} from 'ant-design-vue';
import {enlargeMsg, IPageListData, params, parentMsg} from './ViewPreview.data';
import {carouselList} from '/@/components/AlarmModal/AlarmModal.data';

import {defineEmits, onMounted, onUnmounted, reactive, ref, unref} from 'vue';
import thumbnailmidIcon from '/@/assets/svg/icon/editor_btn_thumbnailmid_hover.svg';
import thumbnailminIcon from '/@/assets/svg/icon/editor_btn_thumbnailmin_hover.svg';

import warningIcon from '/@/assets/svg/icon/info_icon_warning.svg';
import seriousIcon from '/@/assets/svg/icon/info_icon_emergency.svg';
import OfflineCamera from '/@/components/OfflineCamera/OfflineCamera.vue';
import CameraModal from '../../base/cameraManager/components/CameraModal.vue';
import BaseTooltip from '/@/components/BaseTooltip/BaseTooltip.vue';
import errorIcon from '/@/assets/images/checkcode.png';
import {connectWebSocket, onWebSocket, offWebSocket} from '/@/hooks/web/useWebSocketnew';
import {
  auditsResult,
  detailsByCamera,
  enableAlgStream,
  getAlarmCount,
  getDeviceInfo,
  pageListByCamera,
  startStreamProxy
} from './ViewPreview.api';
import AlarmModal from '/@/components/AlarmModal/AlarmModal.vue';
import BaseSwitch from '/@/components/BaseSwitch/BaseSwitch.vue';
import mitt from '/@/utils/mitt/index';
import {useGlobSetting} from '/@/hooks/setting';
import {useUserStore} from '/@/store/modules/user';
import axios from 'axios';
import {list} from '/@/views/base/deviceManagement/DeviceManagement.api';
import RealTimeModal from "/@/views/aiCheck/viewPreview/components/RealTimeModal.vue";
import noDataPng from "/@/assets/images/nodata.png";
import {formatToDateTime} from "/@/utils/dateUtil";
import {getDictData} from "/@/hooks/web/useDictData";

const glob = useGlobSetting();
const userStore = useUserStore();
const ACol = Col;
const ARow = Row;
const ASelect = Select;
const ASelectOption = SelectOption;
const AAnchor = Anchor;
const AAnchorLink = AnchorLink;
const props = defineProps<{
  // fatherMsg: parentMsg[];
  locationMsg: parentMsg[];
  deviceMsg: parentMsg[];
  searchParams: params;
}>();

const emits = defineEmits(['getCameraInfo', 'getChecked']);
const mainState = reactive({
  isFullScreen: false,
  isFold: false,
  isTwo: false,
  current: 1,
  isShowList: true,
  isLoading: false,
  isShowEnlargeImg: false,
  isShowModal: false,
  isImage: false,
  //是否到底
  isAll: false,
  enlargeMsgInfo: {} as enlargeMsg,
  vertifyInfo: {} as carouselList,
  checked: false,
  alarmDetailList: [] as carouselList[],
  currentIndex: 0 as number,
  isShowModel: false as Boolean,
  cameraTitle: '编辑相机',
  element: {},
  videoUrl: '',
  cameraId: '',
  deviceIp: '',
  streamType: 0 as number,
  isTypeChoose: false,
  index: 0 as number,
  value: '实时预览' as string,
  //选择的key
  key: '0' as string,
  isVideoLoading: false as boolean,
  num: 0 as number,
  hostUrl: window.location.host,
  port: 0,
  hostIp: '' as string,
  isPlay: false as boolean,
  viewUrl: '' as string,
});
const editCamera = ref<HTMLElement | null>(null);

const videoElement = ref({});
const player = ref();
// 获取到的分页数据
let pageCameras = ref<IPageListData>({
  byLocationList: [],
  byOwnershipList: [],
});

//图片懒加载
const lazyLoad = () => {
  let imgs = document.querySelectorAll('.detail-image');
  let imgIndex = 0;
  for (let i = 0; i < imgs.length; i++) {
    if (imgs[i].getBoundingClientRect().top < window.innerHeight) {
      imgs[i].src = imgs[i].getAttribute('data-src');
      imgIndex += 1;
    }
  }
};
const listViewo = ref<any>('')
const status = reactive({
  canvasDom: null as HTMLCanvasElement | null,
  canvasCtx: null as CanvasRenderingContext2D | null,
})

//初始化数据
async function initPageData() {
  mainState.hostIp = window.location.hostname;
  const webPort = window.location.port;
  mainState.port = webPort? parseInt(webPort) : 0
  mainState.isLoading = true;
  const deviceInfo = await list('');
  let cameraRes = await pageListByCamera({});
  let count = await getAlarmCount({});
  pageCameras.value.byLocationList = cameraRes.byLocationList;
  pageCameras.value.byOwnershipList = cameraRes.byOwnershipList;
  mainState.isLoading = false;
  await splicPicUrl(pageCameras, count);
  emits('getCameraInfo', cameraRes);
  emits('getChecked', mainState.checked);
  //获取搜索出来的相机
  mitt.on('getSearchCamera', (searchCamera) => {
    pageCameras.value.byLocationList = searchCamera.byLocationList;
    pageCameras.value.byOwnershipList = searchCamera.byOwnershipList;
    splicPicUrl(pageCameras, count);
  });
  let deviceIp;
  for (const dev in deviceInfo.records) {
    deviceIp = deviceInfo.records[dev].deviceIp;
    const url = '/kyscene/refreshcover?' + deviceIp;
    await axios.get(url, {});
  }
}

async function splicPicUrl(pageCameras, count) {
  for (let i = 0; i < pageCameras.value.byLocationList.length; i++) {
    for (let n = 0; n < pageCameras.value.byLocationList[i].cameraLocationList.length; n++) {
      const picPath = pageCameras.value.byLocationList[i].cameraLocationList[n].pic;
      // const newStr = '/kyscene' + picPath + '?' + pageCameras.value.byLocationList[i].cameraLocationList[n].deviceIp;
      const newStr = 'http://' + window.location.host + picPath;
      pageCameras.value.byLocationList[i].cameraLocationList[n].pic = newStr;
      for (let t in count) {
        if (pageCameras.value.byLocationList[i].cameraLocationList[n].id === count[t].cameraId) {
          pageCameras.value.byLocationList[i].cameraLocationList[n].allCameraCount = count[t].allCount;
          pageCameras.value.byLocationList[i].cameraLocationList[n].seriousCameraCount = count[t].seriousCount;
        }
      }
    }
  }
  for (let i = 0; i < pageCameras.value.byOwnershipList.length; i++) {
    for (let n = 0; n < pageCameras.value.byOwnershipList[i].cameraOwnershipList.length; n++) {
      const picPath = pageCameras.value.byOwnershipList[i].cameraOwnershipList[n].pic;
      // const newStr = '/kyscene' + picPath + '?' + pageCameras.value.byOwnershipList[i].cameraOwnershipList[n].deviceIp;
      const newStr = 'http://' + window.location.host + picPath;
      pageCameras.value.byOwnershipList[i].cameraOwnershipList[n].pic = newStr;
      for (let t in count) {
        if (pageCameras.value.byOwnershipList[i].cameraOwnershipList[n].id === count[t].cameraId) {
          pageCameras.value.byOwnershipList[i].cameraOwnershipList[n].allCameraCount = count[t].allCount;
          pageCameras.value.byOwnershipList[i].cameraOwnershipList[n].seriousCameraCount = count[t].seriousCount;
        }
      }
    }
  }
}

//图片加载失败
const handleImageError = (e) => {
  setTimeout(() => {
    const img = e.srcElement;
    img.src = errorIcon;
    img.onerror = null;
  }, 2000);
};

//转二宫格
const changeTwo = () => {
  mainState.isTwo = true;
};
//转三宫格
const changeTree = () => {
  mainState.isTwo = false;
};

//离线相机弹窗
const cameraEdit = (ele) => {
  mainState.element = ele;
  editCamera.value?.initCameraModel({isUpdate: true, datas: ele});
};

let expandPlayer;
const createFlv = (deviceIp, cameraId) => {
  //let url = `http://${deviceIp}:8080/index/api/webrtc?app=live&stream=${cameraId}&type=play`
  let url = `http://${mainState.hostIp}:8080/index/api/webrtc?app=live&stream=${cameraId}&type=play`
  if (mainState.port == 3100) {
    url = `http://${deviceIp}:8080/index/api/webrtc?app=live&stream=${cameraId}&type=play`
  }
  console.log('当前播放链接：', url)
  expandPlayer = new ZLMRTCClient.Endpoint({
    element: videoElement.value['_expand_'],// video 标签
    debug: false, // 是否打印日志
    zlmsdpUrl: url,//流地址
    recvOnly: true,
    resolution: {w: 1280, h: 720},
    usedatachannel: false,
    videoId: '', // 不填选择默认的
    audioId: '', // 不填选择默认的
  })

  expandPlayer.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR,function(e) {
    // ICE 协商出错
    console.log('ICE 协商出错');
  })

  expandPlayer.on(ZLMRTCClient.Events.WEBRTC_ON_REMOTE_STREAMS,function(e) {
    //获取到了远端流，可以播放
    console.log('播放成功',e.streams);
  })

  expandPlayer.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED,function(e)
  {
    // offer anwser 交换失败
    console.log('offer anwser 交换失败',e);
    stopStream();
  });
  expandPlayer.on(ZLMRTCClient.Events.WEBRTC_ON_CONNECTION_STATE_CHANGE,function(state)
  {
    // RTC 状态变化 ,详情参考 https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/connectionState
    console.log('当前状态==>',state);
  });
}

const stopStream = () => {
  if(expandPlayer) {
    expandPlayer.close();
    expandPlayer = null;
    const remote = videoElement.value['_expand_'];
    if(remote) {
      remote.srcObject = null;
      remote.load();
    }
  }
}

//点击此页面外按钮自动关闭视频
async function handleCancelVideo(deviceIp) {
  if (!deviceIp) return
  stopStream()
}

const handleCanPlay = () => {
  if (!mainState.isPlay) return;
  mainState.isVideoLoading = false;
  mainState.isPlay = true;
};

//播放按钮点击
const changeVideoStatus = () => {
  //视频播放按钮消失
  mainState.isPlay = true;
  mainState.isVideoLoading = true;
  message.warning('播放准备中......', )
  const player =  videoElement.value['_expand_']
  player.play()
}

let d = new Date().getTime();

/**
 * 通过http发送打开的摄像头信息
 */
function onWebSocketMessage(data) {
  // 弹窗一直显示
  if (listViewo.value == data.camera_id) {
    if (data.type_id == 1) {
      message.warning(data.msg)
    } else {
      message.warning(data.msg)
    }
  }
}

const expandImg = async (ele) => {
  listViewo.value = ele.id || ''
  mainState.value = '实时预览';
  mainState.streamType = 0;
  mainState.enlargeMsgInfo = ele;
  mainState.isShowList = false;
  //先黑屏显示播放键，点击播放键之后转圈
  mainState.isVideoLoading = false;
  //显示播放键
  mainState.isPlay = false;
  const params = {cameraId: ele.id, streamType: mainState.streamType};
  mainState.cameraId = ele.id;
  // 获取视频流
  const res = await getDeviceInfo(params);
  if (res) {
    mainState.deviceIp = res.deviceIp;
    if (mainState.enlargeMsgInfo.streamType == null) {
      mainState.enlargeMsgInfo.streamType = '';
    }
    // 开启流代理
    const videoParams = {
      deviceIp: mainState.deviceIp,
      cameraId: mainState.cameraId,
    }
    const response =  await startStreamProxy(videoParams);
    console.log('开启视频流代理结果：', response)
    if (response && response.success) {
      message.success("视频点播成功")
      createFlv(mainState.deviceIp, ele.id);
    } else {
      message.error("视频播放失败");
    }
  }
}

//锚点点击
const handleClick = (e: Event, link: string) => {
  if (link?.charAt(0) === '0') {
    link = link.slice(1);
  }
  let anchorName = document.getElementById(link);
  if (anchorName) {
    anchorName.scrollIntoView();
  }
}

// 返回列表
const handleShowList = async () => {
  exitFullscreen()
  mainState.streamType = 0;
  mainState.value = '实时预览';
  mainState.isShowList = true;
  mainState.isShowEnlargeImg = !mainState.isShowEnlargeImg;
  mainState.isVideoLoading = false;
  mainState.isPlay = false;
  //将video的封面属性置为空
  document.querySelector<HTMLElement>('.video')!.poster = '';
  await handleCancelVideo(mainState.deviceIp);
}

// switch按钮
const handleChange = async () => {
  mainState.checked = !mainState.checked;
  let count = await getAlarmCount({});
  if (mainState.checked) {
    emits('getChecked', mainState.checked);
    let cameraRes = await pageListByCamera({
      categorys: props.searchParams.label ? props.searchParams.label.join(',') : '',
      ids: props.searchParams.ids ? props.searchParams.ids.join(',') : '',
      onlyAlarm: true,
    });
    pageCameras.value.byLocationList = cameraRes.byLocationList; // 按区域分组
    pageCameras.value.byOwnershipList = cameraRes.byOwnershipList; //按设备分组
    //筛选显示不出图片
    splicPicUrl(pageCameras, count);
  } else {
    emits('getChecked', mainState.checked);
    let cameraRes = await pageListByCamera({
      categorys: props.searchParams.label ? props.searchParams.label.join(',') : '',
      ids: props.searchParams.ids ? props.searchParams.ids.join(',') : '',
      onlyAlarm: false,
    });
    pageCameras.value.byLocationList = cameraRes.byLocationList;
    pageCameras.value.byOwnershipList = cameraRes.byOwnershipList;
    splicPicUrl(pageCameras, count);
  }
};

// 算法切换
const handleChangeStreamType = async () => {
  // const params = {
  //   deviceIp: mainState.deviceIp,
  //   cameraId: mainState.enlargeMsgInfo.id,
  //   algorithmType: Number(mainState.value)
  // }
  // await changeCameraStream(params)
};

//切换折叠图标
const changeFold = () => {
  mainState.isFold = !mainState.isFold;
  if (mainState.isFold) {
    document.querySelector<HTMLElement>('.node')!.style.display = 'none';
  } else {
    document.querySelector<HTMLElement>('.node')!.style.display = '';
  }
};

//进入全屏
function requestFullElement(element) {
  if (element.requestFullScreen) {
    element.requestFullScreen();
  } else if (element.mozRequestFullScreen) {
    element.mozRequestFullScreen();
  } else if (element.msRequestFullScreen) {
    element.msRequestFullScreen();
  } else if (element.webkitRequestFullScreen) {
    element.webkitRequestFullScreen();
  }
}

//退出全屏模式
function exitFullscreen() {
  if (!mainState.isFullScreen) return
  // 退出全屏模式
  if (document.exitFullscreen) {
    document.exitFullscreen();
  } else if (document.mozCancelFullScreen) {
    document.mozCancelFullScreen();
  } else if (document.webkitExitFullscreen) {
    document.webkitExitFullscreen();
  } else if (document.msExitFullscreen) {
    document.msExitFullscreen();
  }
}

//监听全屏事件
function listenFullScreenEvent() {
  document.addEventListener('fullscreenchange', handleFullscreenChange);
  document.addEventListener('webkitfullscreenchange', handleFullscreenChange);
  document.addEventListener('mozfullscreenchange', handleFullscreenChange);
  document.addEventListener('msfullscreenchange', handleFullscreenChange);
}

// 监听退出全屏事件
function handleFullscreenChange() {
  const fullscreenElement = document['fullscreenElement']
  const webkitFullscreenElement = document['webkitFullscreenElement']
  const mozFullScreenElement = document['mozFullScreenElement']
  const msFullScreenElement = document['msFullScreenElement']
  if (fullscreenElement || webkitFullscreenElement || mozFullScreenElement || msFullScreenElement) {
    console.log('进入全屏模式');
    mainState.isFullScreen = true
  }
  if (!fullscreenElement && !webkitFullscreenElement && !mozFullScreenElement && !msFullScreenElement) {
    console.log('退出全屏模式');
    mainState.isFullScreen = false
  }
}

//单图放大里面的放大图标
const handleFold = () => {
  //全屏播放
  const ele = document.querySelector('.videFullDiv');
  // const ele = document.querySelector('.video');
  requestFullElement(ele);

};

//离线相机点击
const handleOfflineOk = () => {
  mainState.isShowModel = false;
};
const handleOfflineCancel = () => {
  mainState.isShowModel = false;
};

// 显示审核弹窗
const handleShowModal = async () => {
  //调显示弹窗的接口
  const alarmDetailRes = await detailsByCamera({cameraId: mainState.enlargeMsgInfo.id});
  mainState.vertifyInfo = alarmDetailRes.records[0];
  //将list传给审核弹窗
  mainState.alarmDetailList = alarmDetailRes.records;
  mainState.isShowModal = true;
};
const showModal = () => {
  mainState.isShowModal = false;
};

const handleOk = () => {
  mainState.isShowModal = false;
};

const handleCancel = () => {
  mainState.isShowModal = false;
};

const handleBack = () => {
  mainState.isShowModal = false;
};

const handleCommit = async (auditParams) => {
  //将结果给后端
  await auditsResult({
    auditReason: auditParams.auditReason,
    auditResult: auditParams.auditResult,
    alarmId: mainState.vertifyInfo.id
  });
  mainState.isShowModal = false;
};

//上一张
const handleGoPrev = () => {
  //获取到当前的是第几张,才可以对index进行++ --
  mainState.currentIndex = mainState.alarmDetailList.findIndex((item) => item.id === mainState.vertifyInfo.id);
  if (mainState.currentIndex > 0) {
    mainState.currentIndex--;
    mainState.vertifyInfo = mainState.alarmDetailList[mainState.currentIndex];
  }
};
//下一张
const handleGoNext = () => {
  mainState.currentIndex = mainState.alarmDetailList.findIndex((item) => item.id === mainState.vertifyInfo.id);
  if (mainState.currentIndex < mainState.alarmDetailList.length - 1) {
    mainState.currentIndex++;
    mainState.vertifyInfo = mainState.alarmDetailList[mainState.currentIndex];
  }
};

//筛选仅严重
const handleFilterSerious = () => {
  mainState.alarmDetailList = mainState.alarmDetailList.filter((item) => {
    return item.alarmLevel === '4';
  });
};
const goImg = (el) => {
  mainState.alarmDetailList.forEach((item) => {
    if (item.id === el.id) {
      mainState.vertifyInfo = item;
    }
  });
};

// ============= 实时告警 ==============
const realTimeModalVisible = ref(false)
let timeOutNum = undefined

function openRealTimeModal() {
  realTimeModalVisible.value = true
  timeOutNum = setTimeout(() => {
    realTimeModalVisible.value = false
    clearTimeout(timeOutNum)
    timeOutNum = undefined
  }, 3 * 1000)
}

const alarmData = reactive({
  cameraName: '未知',
  alarmEventType: '暂无事件',
  alarmEventImg: noDataPng,
  alarmEventTime: formatToDateTime(new Date())
})

//websocket消息回调函数
//msg:{"cameraId": "123456","cameraName": "haha","algorithmName":"未传反光衣", "eventImg":"", "eventTime":"2024-4-8 10:18:35"}
// 事件弹窗
const showEventDialog = ref(true);
function alarmEventWebsocketCallback(msg) {
  console.log('接收到算法报警事件：', msg)
  if (!msg) return
  if (msg.event === 'alarm_persisted') {
    mitt.emit('refreshViewRightData')
    return
  }
  if (msg.event && msg.event !== 'alarm_received') return
  if (!showEventDialog.value || !mainState.isPlay) return
  const cameraIdEquals = msg.cameraId === mainState.cameraId
  if (cameraIdEquals) {
    const {algorithmName, eventImg, eventTime, cameraName} = msg
    const target = {
      alarmEventType: formatAlgorithmName(algorithmName),
      alarmEventImg: formatImgUrl(eventImg),
      alarmEventTime: formatToDateTime(eventTime),
      cameraName: cameraName
    }
    Object.assign(alarmData, target)
    if (timeOutNum) clearTimeout(timeOutNum)
    openRealTimeModal()
  }
}

function formatAlgorithmName(code: string): string {
  const target = algorithmArray.value.find(item => item.value === code)
  if (target) return target['label']
  return code
}

function formatImgUrl(imgUrl) {
  const prefix = '/alarmImg'
  return prefix + imgUrl
}

// ============= 实时告警结束 ==============

// ============= 初始化websocket ==============
function initWebsocket() {
  // const url = 'ws://127.0.0.1:8443/ws1'
  let userId = unref(userStore.getUserInfo).id;
  const hostIp = window.location.hostname;
  const urlViWebSocket = `ws://${hostIp}:8088/kyscene` + '/ViWebSocket/' + userId
  connectWebSocket(urlViWebSocket);
  const urlAlarmWebSocket = `ws://${hostIp}:8088/kyscene` + '/alarmToWebSocket/' + userId
  connectWebSocket(urlAlarmWebSocket)
}

// ============= 初始化websocket结束 ==============

// ============= 获取字典数据 ==============
const algDetectionList = [
  {key: '0', value: '实时预览'},
  {key: '1', value: '安全帽检测'},
  {key: '2', value: '反光衣检测'},
  {key: '3', value: '打电话检测'},
  {key: '4', value: '吸烟检测'},
  {key: '5', value: '未戴口罩'},
  {key: '6', value: '跌倒'},
  {key: '7', value: '睡岗'},
  {key: '8', value: '烟火检测'}
]
const algorithmArray = ref(algDetectionList)

function getKyCategoryData() {
  getDictData().then(res => {
    if (res) algorithmArray.value = res
    console.log('当前数据字典：', algorithmArray.value)
  })
}

// ============= 获取字典数据结束 ==============

// ============= 生命周期挂载时候 ==============

const keyDown = (e) => {
  if (e.keyCode === 27) {
    showEventDialog.value = !showEventDialog.value
    if (!showEventDialog.value && realTimeModalVisible.value) {
      realTimeModalVisible.value = false
    }
    console.log('弹窗事件改变：', showEventDialog.value)
    message.warn(showEventDialog.value? "开启事件弹窗": "关闭事件弹窗")
  }
}

// 是否开启算法预览
const algStreamEnable = ref(false)
const getSysSettingForAlgStream = async () => {
  algStreamEnable.value = await enableAlgStream()
}

//生命周期到挂载时
onMounted(() => {
  // 初始化数据
  initPageData();
  // 获取字典数据
  getKyCategoryData()
  // 初始化websocket连接，注册回调事件
  initWebsocket()
  onWebSocket(alarmEventWebsocketCallback)
  onWebSocket(onWebSocketMessage);

  listenFullScreenEvent()

  //监听space空格键,开启或者关闭事件弹窗
  window.addEventListener('keydown', keyDown, true)

  getSysSettingForAlgStream()

  /*浏览器关闭、浏览器标签页关闭、浏览器刷新均会触发"beforeunload"事件*/
  window.addEventListener('beforeunload', e => handleCancelVideo(mainState.deviceIp))

})

// ============= 生命周期卸载时候 ==============
onUnmounted(() => {
  console.log('onUnmounted: 生命周期卸载')
  // 删除定时任务
  if (timeOutNum) clearTimeout(timeOutNum)

  // 销毁播放器
  handleCancelVideo(mainState.deviceIp);

  window.removeEventListener('resize', lazyLoad);

  // 移除键盘事件监听
  window.removeEventListener('keydown', keyDown, false)

  /*remove掉上面对应的"beforeunload"事件监听*/
  window.removeEventListener('beforeunload', e => handleCancelVideo(mainState.deviceIp))

  // 取消订阅
  mitt.off('getSearchCamera', (searchCamera) => {
    pageCameras.value.byLocationList = searchCamera.byLocationList;
    pageCameras.value.byOwnershipList = searchCamera.byOwnershipList;
  });
  mitt.all.clear();

  // 关闭websocket
  offWebSocket(alarmEventWebsocketCallback)
  offWebSocket(onWebSocketMessage)
})

</script>
<style>
.video::-webkit-media-controls-mute-button {
  display: none !important;
}

.video::-webkit-media-controls-volume-slider {
  display: none;
}
</style>
<style lang="less" scoped>
.main-tabs {
  height: calc(100vh - 184px);
  margin: 0;

  :deep .ant-anchor-wrapper {
    overflow: hidden;

    border-right: 1px solid var(--input-text-color);
    padding-top: 12px;
    width: 184px;
  }

  :deep .ant-anchor-link-title {
    font-family: Source Han Sans CN;
    color: var(--cancel-text-color);
    font-size: 13px;
    position: relative;
  }

  :deep .ant-anchor-link-title:only-child {
    margin-bottom: none;
  }

  //todo
  // .rootClass {
  //   font-size: 12px;
  //   color: var(--ash-text-color);
  // }

  :deep .ant-anchor .ant-anchor-link {
    padding: 5px !important;
    display: flex;
  }

  :deep .ant-anchor-link-title:only-child {
    margin-bottom: 2px;
  }

  :deep .ant-anchor-link-active > .ant-anchor-link-title {
    color: #7089ed;
  }

  :deep .ant-anchor-ink {
    display: none;
  }

  :deep .ant-anchor .ant-anchor-link {
    padding: 4px 0 4px 16px;
    display: flex;
  }

  :deep .ant-anchor .ant-anchor-link-active {
    border-right: 2px solid #7089ed;
  }

  :deep .ant-anchor-ink-ball {
    display: none;
    background-color: #fff;
    transform: translateX(-50%);
    transition: top 0.3s ease-in-out;
  }

  :deep .ant-anchor .ant-anchor-ink-ball {
    display: none;
  }

  .emergency-icon {
    display: inline-block;
    width: 16px;
    height: 16px;
    background: url('/@/assets/svg/icon/info_icon_emergencyinlist.svg');
    position: absolute;
    left: 0px;
  }

  .parent {
    padding: 16px;

    overflow: hidden;
    text-overflow: ellipsis;
  }

  .wrapper {
    display: flex;
  }

  .select {
    display: flex;
    align-items: center;
  }

  .label {
    margin: 0 0 0 8px;
    background-color: var(--netural-color);
    border: 1px solid #ccc;
    height: 32px;
    display: flex;
    align-items: center;
    padding: 0 4px 0 4px;
  }

  .camera-icon,
  .hemlet-icon,
  .gesture-icon {
    width: 16px;
    height: 16px;
    display: inline-block;
    padding: 4px;
  }

  .camera-icon {
    background: url('/@/assets/svg/icon/info_icon_camera.svg');
  }

  .hemlet-icon {
    background: url('/@/assets/svg/icon/info_icon_helmet.svg');
  }

  .gesture-icon {
    background: url('/@/assets/svg/icon/info_icon_action.svg');
  }

  .node {
    margin-left: 24px;

    .text {
      margin-top: 0px;
    }

    .location {
      display: flex;
      align-items: center;
      opacity: 60%;
      border-right: 1px solid var(--input-text-color);

      .location-icon {
        color: #dce0ed;
        width: 16px;
        height: 16px;
        display: inline-block;
        margin-right: 4px;
        // margin-top: -7px;
        background: url('/@/assets/svg/icon/info_icon_location.svg');
      }
    }

    .device {
      display: flex;
      align-items: center;

      .device-icon {
        color: #dce0ed;
        width: 16px;
        height: 16px;
        display: inline-block;
        margin-right: 4px;
        margin-top: -7px;
        background: url('/@/assets/svg/icon/info_icon_device.svg');
      }
    }
  }

  .list {
    margin: 0 24px 0 16px;
    width: 100%;
  }

  .tab {
    width: 200px;
  }

  .text {
    font-family: Source Han Sans CN;
    color: var(--secondary-text-color);
    font-size: 13px;
    margin-top: 18px;
  }

  .top {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;

    .title {
      font-family: Source Han Sans CN;
      color: var(--secondary-text-color);
      font-size: 13px;
      margin: 0 4px 0 0px;
    }

    .left {
      display: flex;
      align-items: center;

      .divider {
        border-right: 1px solid var(--input-text-color);
        height: 10px;
        margin: 2px 8px 2px 8px;
      }

      .fold-icon {
        display: inline-block;
        width: 24px;
        height: 24px;
        background: url('/@/assets/svg/icon/editor_btn_menufold_default.svg');
        cursor: pointer;

        &:hover {
          background: url('/@/assets/svg/icon/editor_btn_menufold_hover.svg');
        }
      }

      .unfold-icon {
        display: inline-block;
        width: 24px;
        height: 24px;
        background: url('/@/assets/svg/icon/editor_btn_menuunfold_actived.svg');
        cursor: pointer;

        &:hover {
          background: url('/@/assets/svg/icon/editor_btn_menuunfold_hover.svg');
        }
      }
    }

    .right {
      display: flex;
      justify-content: flex-end;

      .mid-icon {
        margin-right: 8px;
        cursor: pointer;
      }

      .min-icon {
        margin-right: 0;
        cursor: pointer;
      }
    }
  }

  .alarm-num {
    display: flex;
    align-items: center;
    margin-right: 12px;

    .warn {
      display: flex;

      .warn-num {
        margin-left: 4px;
        color: #ffffff;
      }
    }

    .serious {
      display: flex;

      &-num {
        margin-left: 4px;
        color: var(--serious-text-color);
      }
    }
  }

  .camera-list-container {
    height: 720px;
    overflow-y: auto;
    justify-content: flex-start;

    margin-right: 1px !important;

    .location-icon {
      width: 16px;
      height: 16px;
      display: inline-block;
      background: url('/@/assets/svg/icon/info_icon_location.svg');
      margin-right: 4px;
    }

    .device-icon {
      width: 16px;
      height: 16px;
      display: inline-block;
      background: url('/@/assets/svg/icon/info_icon_device.svg');
      margin-right: 4px;
    }

    .group-title {
      font-family: Source Han Sans CN;
      color: var(--cancel-text-color);
      font-size: 13px;
    }

    .has-data {
      position: relative;
    }
  }

  .camera-list {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-start;
    margin-right: 1px !important;

    .group {
      display: flex;
      align-items: center;
      margin: 0px 0 8px 0;
    }

    .camera-detail {
      .detail-image {
        border: 1px solid #ccc;
        width: 100%;
        height: 100%;
        border-radius: 4px;
        aspect-ratio: 16 / 9;

        &:hover {
          border: 2px solid #7089ed;
        }
      }

      .modal {
        opacity: 80%;
        height: 34px;
        background-color: #0b0b0d;
        border-radius: 4px;
        position: absolute;
        bottom: 8px;
        left: 8px;
        width: 95%;
        height: 35px;
        font-size: 13px;

        .camera-info {
          width: 97%;
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

        .ant-divider-vertical {
          border-left: 1px solid #ffffff;
          top: 1px;
          background-color: rgba(211, 216, 230, 0.6);
        }
      }
    }
  }

  .enlarge-box {
    width: 100%;
    height: 578px;
    background: #000000;
    position: relative;
    margin-top: 19px;

    .video {
      height: 100%;
      width: 100%;
      object-fit: fill;
      position: absolute;
      top: 0;
    }

    .loading {
      // height: 100%;
      // width: 100%;
      &-image {
        width: 48px;
        height: 48px;
        position: absolute;
        top: 48%;
        left: 48%;
        background: url('../../../assets/svg/icon/loading.gif') transparent;
        display: inline-block;
      }

      &-pic {
        height: 100%;
        width: 100%;
        // object-fit: fill;
      }
    }

    .video-mask {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: linear-gradient(to bottom, rgba(0, 0, 0, 0.1), rgba(92, 92, 92, 0.3), 50%, rgba(255, 255, 255, 0.1););
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
      justify-content: space-between;
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
      height: 36.41px;
      background-color: #0b0b0d;
      border-radius: 4px;
      position: absolute;
      bottom: 8px;
      left: 8px;
    }

    .alarm {
      display: flex;
      height: 36.41px;
      align-items: center;
      justify-content: space-between;

      .name {
        margin-left: 12px;
        color: #ffffff;
      }

      .warning-num {
        color: #ffffff;
        margin-left: 4px;
      }

      .ant-divider-vertical {
        border-left: 1px solid #ffffff;
        top: 1px;
      }

      .to-exam {
        font-family: Source Han Sans CN;
        color: rgba(255, 255, 255, 0.6);
        font-size: 13px;
        margin-right: 4px;
        margin-left: 4px;
        cursor: pointer;

        &:hover {
          color: rgba(255, 255, 255, 0.95);
        }
      }
    }
  }

  .info {
    // width: 1054px;
    background-color: var(--netural-color);
    position: relative;
    border-radius: 4px;
    margin-top: 8px;
    height: 140px;

    &-box {
      padding-top: 24px;
      padding-bottom: 24px;
    }

    .title {
      padding-left: 24px;
      font-family: Source Han Sans CN;
      font-weight: 500;
      color: #5c5e66;
      font-size: 13px;
    }

    .name {
      text-align: center;
      position: absolute;
      left: 92px;
      padding-top: 2px;
      font-family: Source Han Sans CN;
      color: var(--cancel-text-color);
      font-size: 13px;
    }
  }

  .operation {
    display: flex;
    justify-content: flex-end;
  }
}
</style>
