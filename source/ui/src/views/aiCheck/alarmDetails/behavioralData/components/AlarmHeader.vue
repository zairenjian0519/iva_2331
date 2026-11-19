<template>
  <div class="header">
    <div class="time">
      <span class="text">起止时间</span>
      <div class="time-tab">
        <div class="time-select" v-for="(item, index) of mainState.option" :key="item.index">
          <span :class="{ itemTwo: index === 2, item: index === 1, itemZero: index === 0, active: mainState.currentIndex === item.index }" @click="handleClick(item.index, item.day)">{{
            item.title
          }}</span>
        </div></div
      >
      <div>
        <a-range-picker
          ref="rangePickerComp"
          v-model:value="timeValue"
          format="YYYY-MM-DD HH:mm"
          showTime
          :open="mainState.isOpen"
          :trigger="['click']"
          :get-popup-container="
            (triggerNode) => {
              return triggerNode.parentNode;
            }
          "
          @openChange="handleOpenChange"
          :allow-clear="false"
          @ok="handleDateOk"
        />
      </div>
      <div class="detection-mode" v-show="!isStatistic">
        <div class="detection-mode-title">检测模式</div>
        <div class="detection-mode-select">
          <Select v-model:value="mainState.selectValue" :options="mainState.options" @change="handleChange" style="width: 120px">
            <template #suffixIcon><img :src="downIcon" alt="" /></template>
          </Select>
        </div>
      </div>
    </div>
    <div class="base-search">
      <base-search :back-style="false" @handle-search="handleAlarmSearch" @handle-reset="handleAlarmReset" :is-search-btn="isStatistic === mainState.isBtn" />
    </div>
  </div>
</template>
<script lang="ts" setup>
  import BaseSearch from '/@/components/BaseSearch/BaseSearch.vue';
  import { Select } from 'ant-design-vue';
  import { reactive, defineEmits, onMounted, ref } from 'vue';
  import dayjs from 'dayjs';
  import { timeFormat } from '/@/utils/date/dateFormat';
  import downIcon from '/@/assets/svg/icon/icon_arrowdown_default.svg';
  import { detectionMode } from '../AlarmDetails.data';
  defineProps({
    isStatistic: Boolean,
  });
  const emits = defineEmits(['handleAlarmSearch', 'handleAlarmReset']);
  const timeValue = reactive<dayjs.Dayjs[]>([]);
  const rangePickerComp = ref<HTMLElement | string | false | undefined>();
  const mainState = reactive({
    option: [
      { title: '近一天', day: 1, index: 1 },
      { title: '近一周', day: 7, index: 0 },
      { title: '近一月', day: 30, index: 2 },
    ],
    currentIndex: 0,
    currentPage: 1 as number,
    pageSize: 10 as number,
    isBtn: false,
    //0-实时，1-轮询
    selectValue: '',
    options: [
      {
        value: 0,
        label: '实时',
      },
      {
        value: 1,
        label: '轮询',
      },
    ] as detectionMode[],
    isOpen: false as Boolean,
  });

  onMounted(() => {
    handleClick(0, 7);
  });

  const handleClick = (index: number, deltaDay: number) => {
    mainState.currentIndex = index;
    timeValue.length = 0;
    const datetime = new Date();
    timeValue.push(dayjs(datetime).subtract(deltaDay, 'day'));
    timeValue.push(dayjs(datetime));
  };

  const handleOpenChange = (value, mode) => {
    if (value) {
      mainState.isOpen = true;
    } else {
      mainState.isOpen = false;
    }
  };
  //日期自定义
  const handleDateOk = (dates: [dayjs.Dayjs, dayjs.Dayjs] | [string, string]) => {
    mainState.isOpen = false;
    timeValue.length = 0;
    timeValue.push(dayjs(dates[0]));
    timeValue.push(dayjs(dates[1]));
  };

  //检测模式
  const handleChange = (value: string) => {
    mainState.selectValue = value;
  };

  //检索
  const handleAlarmSearch = async (searchData) => {
    const searchParams = {
      cameraNames: searchData.ids.join(','),
      categorys: searchData.label.join(','),
      startTime: timeFormat(timeValue[0]),
      endTime: timeFormat(timeValue[1]),
      isRotate: mainState.selectValue,
    };
    emits('handleAlarmSearch', searchParams);
  };

  //重置
  const handleAlarmReset = (data) => {
    handleClick(0, 7);
    mainState.selectValue = '';
    emits('handleAlarmReset', data);
  };
</script>
<style lang="less" scoped>
  .header {
    background-color: var(--background-Color);
    min-width: 1200px;

    .time {
      display: flex;
      align-items: center;
      :deep .ant-select-selector{
        height: 28px;
      }
      :deep .ant-picker-range {
        margin-left: 8px;
        height: 28px;
        border-color: var(--weak-split-line-color);
      }
      :deep .ant-picker-suffix {
        align-self: center;
        /* margin-right: -5px; */
        color: rgba(0, 0, 0, 0.25);
        line-height: 1;
        pointer-events: none;
        left: 10px;
        position: absolute;
        background: url('/@/assets/svg/icon/filter_icon_datepicker.svg');
      }
      :deep .ant-picker-input {
        position: relative;
        display: inline-flex;
        align-items: center;
        width: 100%;
        left: 32px;
      }
      :deep .ant-picker {
        border-radius: 4px;
        &:hover {
          border-color: #738ce6 !important;
          border-right-width: 1px !important;
        }
      }
      :deep
        .ant-picker-cell:hover:not(.ant-picker-cell-selected):not(.ant-picker-cell-range-start):not(.ant-picker-cell-range-end):not(.ant-picker-cell-range-hover-start):not(.ant-picker-cell-range-hover-end)
        .ant-picker-cell-inner {
        height: 24px;
        background: var(--netural-color);
      }
      :deep .ant-picker-cell-in-view.ant-picker-cell-today .ant-picker-cell-inner::before {
        border-bottom: 4px solid #738ce6;
        border-top: none;
        border-left: none;
        border-right: none;
      }
      :deep .ant-picker-cell-in-view.ant-picker-cell-selected .ant-picker-cell-inner,
      .ant-picker-cell-in-view.ant-picker-cell-range-start .ant-picker-cell-inner,
      .ant-picker-cell-in-view.ant-picker-cell-range-end .ant-picker-cell-inner {
        color: #fff;
        background-color: #b7c4f2;
      }
      :deep .ant-picker-time-panel-column > li.ant-picker-time-panel-cell-selected .ant-picker-time-panel-cell-inner {
        border-color: #00b3ff;
      }
      :deep .ant-btn-primary {
        width: 64px;
        height: 24px;
        background-color: #738ce6;
        border-radius: 4px;
        &:hover {
          background-color: #7089ed;
        }
        span {
          color: #ffffff;
        }
      }

      .detection-mode {
        display: flex;
        align-items: center;
        &-title {
          font-family: Source Han Sans CN;
          color: var(--secondary-text-color);
          font-size: 13px;
          margin: 0 12px 0 24px;
        }
        &-select {
          height: 28px;
        }
      }
      .text {
        margin-right: 8px;
      }
      .time-tab {
        display: flex;
      }

      &-select {
        display: flex;
        text-align: center;
        align-items: center;
        height: 28px;
        line-height: 28px;
      }
      .active {
        width: 80px !important;
        height: 28px !important;
        background-color: #ffffff !important;
        border: 1px solid #d3d9e8 !important;
        cursor: pointer !important;
        font-family: Noto Sans Kannada !important;
        color: var(--cancel-text-color) !important;
        font-size: 13px !important;
        text-align: center !important;
      }

      .item {
        width: 80px;
        height: 28px;
        background-color: var(--netural-color);
        border: 1px solid #d3d9e8;
        cursor: pointer;
        font-family: Noto Sans Kannada;
        color: var(--secondary-text-color);
        font-size: 13px;
        text-align: center;
      }
      .itemZero {
        width: 80px;
        height: 28px;
        background-color: var(--netural-color);
        border: 1px solid #d3d9e8;
        cursor: pointer;
        font-family: Noto Sans Kannada;
        color: var(--secondary-text-color);
        font-size: 13px;
        text-align: center;
        border-radius: 4px 0 0 4px;
      }
      .itemTwo {
        width: 80px;
        height: 28px;
        background-color: var(--netural-color);
        border: 1px solid #d3d9e8;
        cursor: pointer;
        font-family: Noto Sans Kannada;
        color: var(--secondary-text-color);
        font-size: 13px;
        text-align: center;
        border-radius: 0px 4px 4px 0px;
      }
    }
    .custom-quick {
      font-family: Source Han Sans CN;
      color: #adb4cc;
      font-size: 13px;
      .add-icon {
        background: url('/@/assets/svg/icon/editor_btn_addxs_default.svg');
        width: 12px;
        height: 12px;
        display: inline-block;
        margin-left: 12px;
        &:hover {
          background: url('/@/assets/svg/icon/editor_btn_addxs_hover.svg');
          width: 12px;
          height: 12px;
          display: inline-block;
          margin-left: 12px;
        }
      }
    }
    .base-search {
      .search .reset-btn {
        width: 64px;
        height: 24px;
        background-color: #e0e4ef;
        border-color: #e0e4ef;
        border-radius: 4px;
        margin-right: 0 !important;
      }
    }
  }
</style>
