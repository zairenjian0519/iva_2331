<template>
  <div class="right">
    <div class="right-top">
      <p class="title">事件速览(24小时)</p>
      <div class="num">
        <div class="lastest">
          <div class="lastest-title">
            {{ mainState.lastestNum > 100000 ? '99999+' : mainState.lastestNum }}
          </div>
          <div class="lastest-label">最新</div>
        </div>

        <a-divider type="vertical"/>
        <div class="serious">
          <div class="serious-title">
            {{ mainState.seriousNum > 100000 ? '99999+' : mainState.seriousNum }}
          </div>
          <div class="serious-label">严重</div>
        </div>
      </div>
    </div>
    <div class="right-bottom">
      <div class="alarm-detail">
        <div class="title">事件详情</div>
        <div class="more">
          <!-- <i class="setting-icon"></i> -->
          <span class="text" @click="toAlarmDetail">更多>></span>
        </div>
      </div>
      <div class="alarm-tab">
        <tab-control :title="mainState.title" @click-item="clickItem"/>
      </div>
      <template v-if="mainState.loading">
        <div class="loading"><img src="../../../assets/svg/icon/loading_onwhite.gif" alt=""
                                  class="loading-image"/></div>
      </template>
      <template v-else-if="mainState.alarmList.length === 0">
        <div class="empty">
          <a-empty description="暂无数据"/>
        </div>
      </template>
      <template v-else>
        <div class="container">
          <a-timeline>
            <a-timeline-item v-for="(item, index) of mainState.alarmList" :key="index" color="gray">
              <template #dot v-if="item.alarmLevel === '4'">
                <i class="emergency-icon"></i>
              </template>
              <div class="item">
                <span class="date-time">{{ item.alarmTime.substring(10, 16) }}</span>
                <div class="label">
                  <BaseLabel :label="levelIndexI2V[item.alarmLevel]"/>
                </div>
                <div class="alarm-list">
                  <div class="alarm-area">
                    <i class="location-icon"></i>
                    <span class="area">{{ item.alarmAreaDict ? item.alarmAreaDict : '-' }}</span>
                  </div>

                  <div class="alarm-type">
                    <i class="warning-icon"></i>
                    <span class="test-type">{{ item.alarmTypeDict }}</span>
                  </div>
                </div>
                <button class="vertify" @click.stop="toAudit(item)">审核</button>
              </div>
            </a-timeline-item>
          </a-timeline>
        </div>
      </template>
      <div class="see-more" v-show="mainState.alarmList.length" :span="8"><i
        :class="[mainState.isAll ? 'all-icon' : 'see-more-icon']" @click="handleMore"></i></div>
    </div>

    <alarm-modal
      v-model:visible="mainState.isShowModal"
      @val="showModal"
      @handle-ok="handleOk"
      @show-big-img="goImg"
      @handle-cancel="handleCancel"
      :detail-info="mainState.vertifyInfo"
      :detail-list="mainState.alarmList"
      @handle-back="handleBack"
      @handle-commit="handleCommit"
      @go-next="handleGoNext"
      @go-prev="handleGoPrev"
      :index="mainState.currentIndex"
    />
  </div>
</template>
<script lang="ts" setup>
import BaseLabel from '/@/components/BaseLabel/BaseLabel.vue';
import {alarmList, alarmCount, auditsResult} from './ViewPreview.api';
import TabControl from '/@/components/TabControl/TabControl.vue';
import {Timeline, TimelineItem} from 'ant-design-vue';
import {alarmDetail} from './ViewPreview.data';
import {reactive, onMounted, onUnmounted} from 'vue';
import {router} from '/@/router';
import AlarmModal from '/@/components/AlarmModal/AlarmModal.vue';
import {carouselList} from '/@/components/AlarmModal/AlarmModal.data';
import mitt from '/@/utils/mitt/index';

const ATimelineItem = TimelineItem;
const ATimeline = Timeline;

const mainState = reactive({
  title: ['全部', '严重'],
  lastestNum: 0,
  seriousNum: 0,
  currentNum: 1,
  activeTab: 0,
  isAll: false,
  alarmList: [] as alarmDetail[],
  seriousList: [] as alarmDetail[],
  seriousListAdd: [] as alarmDetail[],
  alarmDetailInfo: [] as alarmDetail[],
  alarmDetailAdd: [] as alarmDetail[],
  isShowModal: false,
  vertifyInfo: {} as carouselList,
  field: '' as string,
  order: '' as string,
  currentIndex: 0 as number,
  loading: true,
});
const levelIndexI2V = {
  '0': 'normal',
  '1': 'remind',
  '2': 'common',
  '3': 'warning',
  '4': 'serious',
};

let refreshRunning = false;
let refreshPending = false;

//初始化数据函数
async function initViewRightData() {
  if (refreshRunning) {
    refreshPending = true;
    return;
  }
  refreshRunning = true;
  try {
    const [alarmCountRes, alarmListRes] = await Promise.all([
      alarmCount({pageSize: 9}),
      alarmList({pageSize: 9}),
    ]);
    mainState.lastestNum = alarmCountRes.todayCount;
    mainState.seriousNum = alarmCountRes.seriousCount;
    mainState.alarmDetailInfo = alarmListRes.records;
    mainState.seriousList = alarmCountRes.seriousList;
    mainState.alarmList = mainState.activeTab === 1
      ? mainState.seriousList
      : mainState.alarmDetailInfo;
    mainState.currentNum = 1;
    mainState.isAll = false;
    mainState.loading = false;
  } finally {
    refreshRunning = false;
    if (refreshPending) {
      refreshPending = false;
      void initViewRightData();
    }
  }
}

const refreshViewRightData = () => void initViewRightData();

onMounted(() => {
  void initViewRightData()
  mitt.on('refreshViewRightData', refreshViewRightData)
});

onUnmounted(() => {
  mitt.off('refreshViewRightData', refreshViewRightData)
});

const clickItem = (index: number) => {
  mainState.activeTab = index;
  if (index === 1) {
    mainState.alarmList = mainState.seriousList;
    mainState.isAll = false;
    mainState.currentNum = 1;
  } else {
    mainState.alarmList = mainState.alarmDetailInfo;
    //严重切换回全部 需要重置
    mainState.currentNum = 1;
    mainState.isAll = false;
  }
};
//加載更多
const handleMore = async () => {
  mainState.currentNum += 1;
  if (document.querySelector('.tab-control .active')?.getElementsByTagName('span')[0].innerHTML === '全部') {
    const alarmAllRes = await alarmList({pageNo: mainState.currentNum, pageSize: 9});
    mainState.alarmDetailAdd = alarmAllRes.records;
    // // 向数组里追加新数据
    mainState.alarmList = mainState.alarmList.concat(mainState.alarmDetailAdd);
    if (mainState.alarmList.length >= alarmAllRes.total) {
      mainState.isAll = true;
    }
  } else {
    const seriousListRes = await alarmCount({pageNo: mainState.currentNum, pageSize: 9});
    mainState.seriousListAdd = seriousListRes.seriousList;
    mainState.alarmList = mainState.seriousList.concat(mainState.seriousListAdd);
    if (mainState.alarmList.length >= mainState.seriousList.length) {
      mainState.isAll = true;
    }
  }
};

//点击审核
const toAudit = (item) => {
  mainState.isShowModal = true;
  mainState.vertifyInfo = item;
};
//
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
  handleGoNext();
  mainState.isShowModal = true;
};

//上一张
const handleGoPrev = () => {
  //获取到当前的是第几张,才可以对index进行++ --
  mainState.currentIndex = mainState.alarmList.findIndex((item) => item.id === mainState.vertifyInfo.id);
  if (mainState.currentIndex > 0) {
    mainState.currentIndex--;
    mainState.vertifyInfo = mainState.alarmList[mainState.currentIndex];
  }
};
//下一张
const handleGoNext = () => {
  mainState.currentIndex = mainState.alarmList.findIndex((item) => item.id === mainState.vertifyInfo.id);
  if (mainState.currentIndex < mainState.alarmList.length - 1) {
    mainState.currentIndex++;
    mainState.vertifyInfo = mainState.alarmList[mainState.currentIndex];
  }
};
const goImg = (el) => {
  mainState.alarmList.forEach((item) => {
    if (item.id === el.id) {
      mainState.vertifyInfo = item;
    }
  });
};

//去事件详情
const toAlarmDetail = () => {
  router.push('/alarmDetails/alarm');
};
</script>
<style lang="less" scoped>
.right {
  margin: 12px 12px 0 0;

  .loading {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    width: 100%;

    &-image {
      width: 32px;
      height: 32px;
    }
  }

  &-top {
    margin: 12px 0 12px 0;
    height: 144px;
    background-color: var(--background-Color);

    .title {
      font-family: Source Han Sans CN;
      font-weight: 500;
      color: var(--headerline-text-color);
      font-size: 13px;
      padding: 20px 0 0 24px;
    }

    .num {
      display: flex;
      justify-content: center;

      .lastest {
        align-items: center;
        text-align: center;

        &-title {
          font-family: 'BebasNeue-Regular';
          font-weight: 700;
          color: #7089ed;
          font-size: 36px;
        }

        &-label {
          color: var(--cancel-text-color);
          margin-left: 5px;
          font-family: Source Han Sans CN;
          font-size: 13px;
        }
      }

      .serious {
        margin-left: 75px;
        text-align: center;

        &-title {
          font-family: 'BebasNeue-Regular';
          color: var(--serious-text-color);
          font-weight: bold;
          font-size: 36px;
        }

        &-label {
          color: var(--cancel-text-color);
          font-family: Source Han Sans CN;
          color: var(--cancel-text-color);
          font-size: 13px;
        }
      }
    }

    .ant-divider-vertical {
      width: 0px;
      height: 60px;
      border: 1px solid;
      border-color: #e0e4ef;
      left: 35px;
    }
  }

  .empty {
    width: 100%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  &-bottom {
    background: var(--background-Color);
    height: calc(100vh - 224px);
    overflow-x: auto;
    overflow-y: hidden;

    .alarm-detail {
      display: flex;
      justify-content: space-between;

      .title {
        font-family: Source Han Sans CN;
        font-weight: 500;
        color: var(--headerline-text-color);
        font-size: 13px;
        margin: 24px 0 12px 24px;
      }

      .more {
        margin: 24px 36px 12px 24px;
        display: flex;
        align-items: center;

        .setting-icon {
          background: url('/@/assets/svg/icon/info_btn_pushsettings_default.svg');
          width: 16px;
          height: 16px;
          display: inline-block;
          margin-right: 8px;
          cursor: pointer;
        }

        .text {
          font-family: Source Han Sans CN;
          color: #adb4cc;
          font-size: 13px;
          cursor: pointer;
        }
      }
    }

    .container {
      width: 100%;
      position: relative;
    }

    .item {
      display: flex;
      align-items: center;
      width: 100%;
      min-width: 331px;
    }

    :deep .ant-timeline-item-head {
      position: absolute;
      width: 16px;
      height: 16px;
      background-color: var(--fill-color);
      border: none;
      margin: 8px 0 0 0px;
    }

    :deep .ant-timeline-item-head-gray {
      // color: rgba(0, 0, 0, 0.25);
      background: url('/@/assets/svg/icon/icon_timeline.svg');
      width: 16px;
      height: 16px;
    }

    :deep .ant-timeline-item-tail {
      left: 7px;
      opacity: 40%;
      height: 60px;
      margin: 16px 0 0 0;
    }

    :deep .ant-timeline {
      // position: absolute;
      left: 0;
      right: 0;
      top: 12px;
      height: 650px;
      overflow-y: auto;
      margin-right: 32px !important;
    }

    :deep .ant-timeline-item-content {
      margin: 3px 0 0 12px;
    }

    .ant-timeline-item {
      margin: 16px 0px 0 24px;
      padding: 0 0 12px 0 !important;

      &:hover {
        background: var(--netural-color);
      }

      &:first-child {
        margin-top: 0;
      }
    }

    .emergency-icon {
      display: block;
      background: url('/@/assets/svg/icon/info_icon_emergency.svg');
      width: 16px;
      height: 16px;
    }

    .date-time {
      font-family: Source Han Sans CN;
      color: var(--secondary-text-color);
      font-size: 13px;
      margin: -8px 12px 0 4px;
      width: 34px;
    }

    .label {
      margin-top: -10px;
    }

    .alarm-list {
      margin: 12px 42px 0 16px;
      opacity: 79%;
      font-family: Source Han Sans CN;
      color: #5c5e66;
      font-size: 13px;

      .alarm-area,
      .alarm-type {
        display: flex;
        align-items: center;
      }

      .area,
      .test-type {
        width: 80px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .location-icon,
    .warning-icon {
      width: 16px;
      height: 16px;
      display: inline-block;
      margin-right: 4px;
    }
  }

  .location-icon {
    background: url('/@/assets/svg/icon/info_icon_location.svg');
  }

  .warning-icon {
    background: url('/@/assets/svg/icon/info_icon_warning.svg');
  }

  .vertify {
    font-family: Source Han Sans CN;
    color: #7089ed;
    font-size: 13px;
    width: 42px;
    height: 20px;
    background-color: transparent;
    margin: -6px 16px 0 9px;
    cursor: pointer;

    &:hover {
      background-color: var(--fill-three-color);
      border-radius: 4px;
    }
  }

  .see-more {
    display: flex;
    justify-content: center;
    align-items: center;
    background: var(--background-Color);
    position: absolute;
    bottom: 20px;
    height: 35px;
    width: 86%;
    text-align: center;

    &-icon {
      width: 32px;
      height: 16px;
      background: url('/@/assets/svg/icon/icon_seemore.svg');
      width: 32px;
      height: 16px;
      cursor: pointer;

      &:hover {
        background: url('/@/assets/svg/icon/icon_seemore_hover.svg');
      }
    }

    .all-icon {
      width: 32px;
      height: 16px;
      background: url('/@/assets/svg/icon/icon_end.svg');
    }
  }
}
</style>
