<template>
  <div class="root">
    <div class="main">
      <Row type="flex">
        <Col :span="18">
          <div class="left">
            <span class="view">
              <span class="title">实时预览</span>
              <!-- 相机运行异常 -->
              <template v-if="abnormal.length > 0">
                <div class="abnormal"></div>
                <span class="text">最近时间{{ currentDate }}</span>
                <div class="abnormal-name"
                  ><span>离线</span> <div class="num">{{ abnormal.length }}</div>
                </div>
              </template>
              <template v-else>
                <div class="dot" v-if="!isNormal"></div> <span class="text" v-if="!isNormal">当前时间{{ currentDate }}</span></template
              >
            </span>
            <div style="margin-left: 24px">
              <base-search :back-style="true" @getParentNode="getParentNode" @handle-search="handleSearch" @handle-reset="handleReset" :is-search-btn="true" />
            </div>
            <p class="divider"></p>
            <ViewLeft :location-msg="locationNode" :device-msg="deviceNode" :search-params="mainState.searchParams" @get-camera-info="getCameraInfo" @get-checked="getChecked" />
          </div>
        </Col>
        <Col :span="6">
          <div class="right"><ViewRight /> </div>
        </Col>
      </Row>
    </div>
  </div>
</template>
<script lang="ts" setup>
import {Col, Row} from 'ant-design-vue';
import ViewRight from './ViewRight.vue';
import {onMounted, reactive, ref} from 'vue';
import BaseSearch from '/@/components/BaseSearch/BaseSearch.vue';
import {params, parentMsg} from './ViewPreview.data';
import ViewLeft from './ViewLeft.vue';
import {pageListByCamera} from './ViewPreview.api';
import mitt from '/@/utils/mitt/index';

const currentDate = ref('');
  // 单相机异常
  const isNormal = ref<Boolean>(false);
  //有异常的相机数组
  const abnormal = ref<string[]>([]);
  const parentList = ref<parentMsg[]>([]);
  const deviceNode = ref<parentMsg[]>([]);
  const locationNode = ref<parentMsg[]>([]);

  const mainState = reactive({
    isChecked: '',
    searchParams: {} as params,
  });

  const getCameraInfo = (val) => {
    abnormal.value.length = 0;
    val.byLocationList.forEach((item) => {
      item.cameraLocationList.forEach((ele) => {
        if (ele.cameraStatus === '0') {
          abnormal.value.push(ele.cameraName);
        }
      });
    });
    //两组数据相机内容相同，离线的统计一组长度就可以
    // val.byOwnershipList.forEach((item) => {
    //   item.cameraOwnershipList.forEach((ele) => {
    //     if (ele.cameraStatus === '0') {
    //       abnormal.value.push(ele.cameraName);
    //     }
    //   });
    // });
  };
  onMounted(() => {
    setInterval(() => {
      //   当前时间
      const date: Date = new Date();
      currentDate.value = date.toLocaleString();
    }, 1000);
    // getCameraInfo();
  });

  const getParentNode = (val) => {
    parentList.value = val;
    //引用类型删除数组比较快，length = 0,不能用[]
    locationNode.value.length = 0;
    deviceNode.value.length = 0;
    parentList.value.forEach((item) => {
      if (item.type === 'area') {
        locationNode.value.push(item);
        //由于默认放入的值是当前区域的值，如果再选当前区域，需要把以前的给清除
        // let map = new Map();
        // for (let item of locationNode.value) {
        //   if (item.id !== null) {
        //     if (!map.has(item.id)) {
        //       map.set(item.id, item);
        //     }
        //   }
        // }
        // locationNode.value = [...map.values()];
      } else if (item.type === 'device') {
        deviceNode.value.push(item);
        // let deviceMap = new Map();
        // for (let item of deviceNode.value) {
        //   if (item.id !== null) {
        //     if (!deviceMap.has(item.id)) {
        //       deviceMap.set(item.id, item);
        //     }
        //   }
        // }
        // deviceNode.value = [...deviceMap.values()];
        // console.log(deviceNode.value);
      }
    });
  };

  //获取是否选中仅事件
  const getChecked = (isChecked: boolean) => {
    mainState.isChecked = isChecked;
  };

  const handleSearch = async (val) => {
    mainState.searchParams = val;
    const searchRes = await pageListByCamera({
      categorys: val.label.join(','),
      ids: val.ids.join(','),
      onlyAlarm: mainState.isChecked,
    });
    mitt.emit('getSearchCamera', searchRes);
  };

  const handleReset = async (data) => {
    data.camera.length = 0;
    data.alarm.length = 0;
    const searchRes = await pageListByCamera({});
    mitt.emit('getSearchCamera', searchRes);
  };
</script>
<style lang="less" scoped>
  .root {
    height: calc(100vh - 48px);
    overflow-x: auto;
  }
  .main {
    min-width: 1366px;
  }
  .left {
    background: var(--background-Color);
    margin: 12px 12px 12px 12px;
    overflow: auto;
    .view {
      display: flex;
      align-items: center;
    }
    .title {
      font-family: Source Han Sans CN;
      font-weight: 600;
      color: var(--headerline-text-color);
      font-size: 14px;
      margin: 20px 16px 0px 24px;
    }
    .dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background-color: var(--adopt-text-color);
      margin-right: 8px;
      margin-top: 20px;
    }
    .abnormal {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background-color: var(--serious-text-color);
      margin-right: 8px;
      margin-top: 20px;
    }
    .abnormal-name {
      opacity: 20%;
      width: 56px;
      height: 20px;
      padding: 1px;
      text-align: center;
      background-color: #adb4cc;
      border-radius: 4px;
      display: flex;
      justify-content: center;
      margin: 20px 0 0 12px;

      .num {
        margin-left: 2px;
        width: 16px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
    .text {
      font-family: Source Han Sans CN;
      color: var(--cancel-text-color);
      font-size: 13px;
      margin-top: 20px;
    }

    .divider {
      border-bottom: 1px solid var(--input-text-color);
      opacity: 60%;
      margin: 4px 16px 16px 16px;
    }
  }
</style>
