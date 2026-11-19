<template>
  <div class="alarm-modal" ref="alarmModal">
    <Modal
      v-model:visible="dialogVisible"
      width="80%"
      :centered="true"
      @ok="handleOk"
      @cancel="handleCancel"
      title="事件详情"
      dialogClass="dialog"
      :get-container="getContainer"
      :footer="null"
      v-if="dialogVisible"
      :destroy-on-close="true"
    >
      <div class="header">
        <!-- <span class="live">实况</span>
        <BaseSwitch v-model:checked="mainState.isLiveChecked" @handle-change="handleChange" />
        <span class="show-roi">显示ROI</span>
        <BaseSwitch v-model:checked="mainState.isROIChecked" @handle-change="handleROIChange" /> -->
      </div>
      <div class="top">
        <div class="prev" @click="goPrev">
          <span class="prev-icon"></span>
        </div>
        <div class="preview-img">
          <!-- 传过来的图片props -->
          <img :src="path === '/viewPreview/view' ? '/kyscene' + detailInfo?.picPath + '?' + detailInfo.alarmId : detailInfo?.picPath" alt="" class="big-image" @error="handleImageError" />
        </div>
        <div class="next" @click="goNext">
          <span class="next-icon"></span>
        </div>
        <div class="message">
          <div class="left">
            <div class="right"></div>
            <div class="name">
              <span class="carema-name">相机名称</span>
              <span class="info">{{ detailInfo?.cameraName }}</span>
            </div>
            <div class="area">
              <span class="capture-location">自定义分组</span>
              <span class="info">{{ detailInfo?.alarmAreaDict ? detailInfo?.alarmAreaDict : '-' }}</span>
            </div>
            <div class="date-time">
              <span class="capture-time">抓拍时间</span>
              <span class="info">{{ detailInfo?.alarmTime }}</span>
            </div>
            <div>
              <span class="alarm-type">事件类型</span>
              <span class="info"> {{ detailInfo?.alarmTypeDict }} </span>
            </div>
            <div class="level">
              <span class="alarm-level">事件等级</span>
              <BaseLabel :label="levelIndexI2V[detailInfo?.alarmLevel]" />
            </div>
            <div class="count">
              <div v-if="detailInfo.alarmTypeDict == '裸土覆盖' ">
                <span class="target-count">覆盖比例</span>
                <span class="info">{{ detailInfo?.targetCount }} %</span>
              </div>
              <div class="count" v-else-if="detailInfo && detailInfo?.carNumber !=null">
                <span class="target-count">车牌号</span>
                <span class="info">{{ detailInfo?.carNumber }}</span>
              </div>
              <div v-else>
                <span class="target-count">目标数量</span>
                <span class="info">{{ detailInfo?.targetCount }}</span>
              </div>
            </div>

            <div class="status">
              <span class="audit-status">审核状态</span>
              <template v-if="detailInfo?.auditStatus === '0' && detailInfo?.auditResult == null">
                <BaseLabel :label="'pendingReview'" />
              </template>
              <template v-if="detailInfo?.auditStatus === '1' && detailInfo?.auditResult === '0'">
                <BaseLabel :label="'cancel'" />
              </template>
              <template v-if="detailInfo?.auditStatus === '1' && detailInfo?.auditResult === '1'">
                <BaseLabel :label="'adopt'" />
              </template>
            </div>
            <div class="comments">
              <span class="review-comments">审核意见</span>
              <div class="radio-box">
                <template v-if="detailInfo?.auditResult === '1'">
                  <BaseRadio :value="mainState.value" @handle-change="handleRadioChange" />
                </template>
                <template v-else-if="detailInfo?.auditResult === '0'">
                  <BaseRadio :value="mainState.value" @handle-change="handleRadioChange" />
                </template>
                <template v-else>
                  <BaseRadio :value="mainState.value" @handle-change="handleRadioChange" />
                </template>
              </div>
            </div>

            <template v-if="mainState.value === 2">
              <Divider type="horizontal" class="divider" />
              <div class="reason-info">
                <i class="error-icon"></i>
                <!-- 审核作废原因 -->
                <span class="reason">请补充错误原因</span></div
              >

              <Row>
                <Col :span="24">
                  <a-checkbox v-model:checked="mainState.isPictureBlur" @change="handleChangePicture"><span class="reson-item">图片模糊</span></a-checkbox></Col
                ></Row
              >
              <Col :span="24"
                ><a-checkbox v-model:checked="mainState.isTargetBlocked" @change="handleChangeTarget"><span class="reson-item"> 目标被遮挡</span></a-checkbox></Col
              >
              <Col :span="24">
                <a-checkbox v-model:checked="mainState.isBackgroundBlocked" @change="handleChangeBackground"><span class="reson-item">背景色遮挡</span></a-checkbox></Col
              >
              <Col :span="24"
                ><a-checkbox v-model:checked="mainState.isOther" @change="handleChangeOther"><span class="reson-item">其他</span></a-checkbox></Col
              >
            </template>
            <a-Textarea v-model:value="cancelReason" placeholder="请说明原因" v-if="mainState.isOther" />
          </div>

          <div v-if="detailInfo?.faceUrl" class="right">
            <img :src="detailInfo?.faceUrl" alt="" class="face-img" />
            <div class="face-info">
              <div class="face-name">
                <span class="title">姓名</span>
                <span class="info">
                  <a-tooltip :title="detailInfo?.faceDetailDo.faceName" placement="top">{{ detailInfo?.faceDetailDo.faceName }}</a-tooltip>
                </span>
              </div>
              <div class="face-id-card">
                <span class="title">证件号</span>
                <span class="info">
                  <a-tooltip :title="detailInfo?.faceDetailDo.faceIdCard" placement="top">{{ detailInfo?.faceDetailDo.faceIdCard }}</a-tooltip>
                </span>
              </div>
              <div class="face-phone">
                <span class="title">手机号</span>
                <span class="info">
                  <a-tooltip :title="detailInfo?.faceDetailDo.facePhone" placement="top">{{ detailInfo?.faceDetailDo.facePhone }}</a-tooltip>
                </span>
              </div>
              <div class="face-remark">
                <span class="title">备注</span>
                <span class="info">
                  <a-tooltip :title="detailInfo?.faceDetailDo.remark" placement="top">{{ detailInfo?.faceDetailDo.remark }}</a-tooltip>
                </span>
              </div>
            </div>
          </div>
          <!-- 暂时不显示,根据后期需求添加   添加了一推1111111111111就是为了不显示 -->
          <div class="vertical-divider" v-if="detailInfo.carNumber111111111111111111111111111111111111111"></div>
          <div class="right" v-if="detailInfo.carNumber111111111111111111111111111111111111111">
            <div class="number">
              <span class="title">车牌号</span>

              <span class="info">{{ detailInfo?.carNumber111111111111111111111111111111111111111 }}</span>
            </div>
            <div class="color">
              <span class="title">车牌颜色</span>

              <span class="info">{{ detailInfo?.carColor }}</span>
            </div>
            <div class="car-type">
              <span class="title">车辆类型</span>
              <span class="info">{{ detailInfo?.carType }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="operation">
        <BaseButton :type="'back'" class="back-btn" @click="handleBack" second-button="返回" />
        <BaseButton :type="'sure'" class="confirm-btn" @click="handleCommit" primary-buttton="确认" />
      </div>
      <div class="bottom">
        <!--        <span class="progress">审核进度</span>-->
        <!--        <span class="num">{{ props.detailList.filter((item) => item.auditStatus !== '0').length }} / {{ props.detailList.length }}</span>-->
        <span @click="handleFilterSerious" :class="[props.detailList.filter((item) => item.alarmLevel === '4').length > 0 ? 'text' : 'no-num']">仅严重</span>
        <!-- 数量 -->
        <div class="carousel">
          <!-- :autoplay="true" -->
          <Carousel ref="carousel" :dots="false" :slides-to-show="props.detailList.length < 7 ? props.detailList.length : 7" :slides-to-scroll="1" :infinite="false" arrows>
            <template #prevArrow>
              <!-- <div class="custom-slick-arrow" style="left: 10px; z-index: 1"> -->
              <div class="prev-carousel">
                <span class="prev-icon"></span>
              </div>
              <!-- </div> -->
            </template>
            <template #nextArrow>
              <!-- <div class="custom-slick-arrow" style="right: 10px"> -->
              <div class="next-carousel">
                <span class="next-icon"></span>
              </div>
              <!-- </div> -->
            </template>

            <div v-for="(item, id) in mainState.detailList" :key="id" class="card-box">
              <!-- <div class="label"> -->
              <div class="thumbnail-item" @dblclick="handleshowBigImg(item)">
                <img
                  :src="path === '/viewPreview/view' ? '/kyscene' + item?.thumbnailPath + '?' + item?.alarmId : item?.thumbnailPath"
                  class="thumbnail-img"
                  :class="{ isActive: item.id === detailInfo.id }"
                  alt=""
                  @error="handleImageError"
                />
                <div class="label">
                  <BaseLabel :label="getThumbnailLabel(item)" />
                </div>
              </div>
            </div>
            <!-- </div> -->
          </Carousel>
        </div>
      </div>
    </Modal>
  </div>
</template>
<script lang="ts" setup>
  import { Modal, Carousel, Divider, Checkbox, Row, Col } from 'ant-design-vue';
  import { computed, defineEmits, ref, reactive, onMounted, watch, unref } from 'vue';
  import BaseButton from '/@/components/BaseButton/BaseButton.vue';
  // import BaseSwitch from '../BaseSwitch/BaseSwitch.vue';
  import BaseLabel from '/@/components/BaseLabel/BaseLabel.vue';
  import BaseRadio from '/@/components/BaseRadio/BaseRadio.vue';
  import { carouselList } from './AlarmModal.data';
  import errorIcon from '/@/assets/images/error.png';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useRouter } from 'vue-router';
  const { currentRoute } = useRouter();
  const path = unref(currentRoute).path;

  const ACheckbox = Checkbox;
  // const ATextarea = Textarea;
  const emit = defineEmits(['val', 'handleOk', 'handleCancel', 'goPrev', 'goNext', 'handleBack', 'handleCommit', 'handleFilterSerious', 'showBigImg']);
  const props = defineProps<{
    visible: Boolean;
    detailInfo: carouselList;
    detailList: carouselList[];
    index: number;
    // seriousNum: number;
  }>();
  const { createMessage } = useMessage();
  const cancelCode = '0';
  const passCode = '1';
  const mainState = reactive({
    isLiveChecked: false,
    isROIChecked: false,
    isPass: true, //默认进来是true 否则通过存false，需要重新点击
    isCancel: false,
    isPictureBlur: false,
    isTargetBlocked: false,
    isBackgroundBlocked: false,
    isOther: false,
    temp: true,
    newVal: [] as carouselList[],
    detailList: [] as carouselList[],
    value: 1,
    isCurrent: false,
    current: '0',
    reasonArr: [] as string[],

    // detailList: [] as carouselList[],
  });
  const detailList = ref(props.detailList);
  const alarmModal = ref<HTMLElement | string | false | undefined>();
  const carousel = ref<HTMLElement | null>(null);

  const cancelReason = ref<string>('');

  const levelIndexI2V = {
    '0': 'normal',
    '1': 'remind',
    '2': 'common',
    '3': 'warning',
    '4': 'serious',
  };
  onMounted(() => {});

  const dialogVisible = computed({
    get() {
      return props.visible;
    },
    set(val) {
      emit('val', val);
    },
  });
  watch(
    () => props.index,
    (val) => {
      if (val && carousel.value) {
        carousel?.value.goTo(val);
      }
      // if (val && val > 6) {
      //   carousel?.value.next();
      // } else if (val && val <= 6) {
      //   carousel?.value.prev();
      // }
    },
    { deep: true, immediate: true }
  );

  watch(
    () => props.visible,
    (v) => {
      if (v) {
        detailList.value = props.detailList;
      }
    },
    { deep: true, immediate: true }
  );
  watch(
    () => props.detailList,
    (val) => {
      mainState.detailList = val;
    },
    { deep: true, immediate: true }
  );

  watch(
    () => props.detailInfo.auditResult,
    (val) => {
      if (val === '1' || val === null) {
        mainState.isBackgroundBlocked = false;
        mainState.isPictureBlur = false;
        mainState.isTargetBlocked = false;
        mainState.isOther = false;
        mainState.value = 1;
      } else {
        mainState.isBackgroundBlocked = false;
        mainState.isPictureBlur = false;
        mainState.isTargetBlocked = false;
        mainState.isOther = false;
        mainState.value = 2;
      }
    },
    { deep: true, immediate: true }
  );

  watch(
    () => props.detailInfo.auditReason,
    (val) => {
      if (props.detailInfo.auditResult === cancelCode && props.detailInfo.auditStatus === passCode) {
        if (val.includes('图片模糊')) {
          mainState.isPictureBlur = true;
        } else {
          mainState.isPictureBlur = false;
        }
        if (val.includes('目标被遮挡')) {
          mainState.isTargetBlocked = true;
        } else {
          mainState.isTargetBlocked = false;
        }
        if (val.includes('背景色遮挡')) {
          mainState.isBackgroundBlocked = true;
        } else {
          mainState.isBackgroundBlocked = false;
        }
        if (!val.includes('背景色遮挡') && !val.includes('目标被遮挡') && !val.includes('图片模糊')) {
          mainState.isOther = true;
          cancelReason.value = val;
        } else {
          mainState.isOther = false;
        }
      }
    },
    { deep: true, immediate: true }
  );

  //审核弹窗轮播图加载失败
  const handleImageError = (e) => {
    setTimeout(() => {
      const img = e.srcElement;
      img.src = errorIcon;
      img.onerror = null;
    }, 100);
  };

  // 点击确定后的回调
  const handleOk = () => {
    emit('handleOk');
  };
  // 点击x的回调
  const handleCancel = () => {
    emit('handleCancel');
  };
  // 制定modal挂载的节点
  const getContainer = () => {
    return alarmModal.value;
  };
  // 上一个
  const goPrev = () => {
    emit('goPrev');
  };
  // 下一个
  const goNext = () => {
    emit('goNext');
  };
  //双击显示大图
  const handleshowBigImg = (item) => {
    emit('showBigImg', item);
  };
  const handleChangePicture = (e) => {
    mainState.isPictureBlur = e.target.checked;
  };

  const handleChangeTarget = (e) => {
    mainState.isTargetBlocked = e.target.checked;
  };

  const handleChangeBackground = (e) => {
    mainState.isBackgroundBlocked = e.target.checked;
  };
  const handleChangeOther = (e) => {
    mainState.isOther = e.target.checked;
    cancelReason.value = ''; //每次选择其他原因 进来需要清空一下原因框 否则会保留上一次的内容
    if (mainState.isOther) {
      if (cancelReason.value === '') {
        createMessage.warn('作废原因不能为空');
      }
    }
  };

  //点击返回
  const handleBack = () => {
    emit('handleBack');
  };
  //点击确定
  const handleCommit = async () => {
    //审核结果
    //审核状态
    //作废原因
    let reasonArr: string[] = [];
    if (mainState.isPictureBlur) {
      reasonArr.push('图片模糊');
    }
    if (mainState.isTargetBlocked) {
      reasonArr.push('目标被遮挡');
    }
    if (mainState.isBackgroundBlocked) {
      reasonArr.push('背景色遮挡');
    }
    if (mainState.isOther) {
      reasonArr.push(cancelReason.value);
    }

    const auditParams = {
      auditReason: reasonArr.join(','),
      auditResult: mainState.value === 1 ? passCode : cancelCode,
    };
    //避免审核作废后再选通过 还包含作废原因
    if (auditParams.auditResult === '1') {
      auditParams.auditReason = '';
    }

    emit('handleCommit', auditParams);
  };

  // // switch按钮
  // const handleChange = () => {
  //   mainState.isLiveChecked = !mainState.isLiveChecked;
  // };
  // const handleROIChange = () => {
  //   mainState.isROIChecked = !mainState.isROIChecked;
  // };
  // const onChange = (current: number) => {
  //   // console.log(current);
  // };
  //审核radio
  const handleRadioChange = (e) => {
    if (e.target.value === 1) {
      mainState.value = 1;
      mainState.isOther = false;
    } else {
      mainState.value = 2;
    }
  };

  //筛选仅严重
  const handleFilterSerious = () => {
    emit('handleFilterSerious');
  };

  // 新增获取缩略图标签类型的方法
  const getThumbnailLabel = (item): string => {
    // 未审核状态：auditStatus为'0'或为空且auditResult为null
    if ((item?.auditStatus == '0' || !item?.auditStatus) && item?.auditResult == null) {
      return 'pendingReview';
    }
    // 通过状态
    if (item?.auditResult == passCode) {
      return 'adopt';
    }
    // 作废状态
    if (item?.auditResult == cancelCode) {
      return 'cancel';
    }
    // 默认返回未审核状态
    return 'pendingReview';
  };
</script>
<style lang="less" scoped>
  .divider {
    width: 348px !important;
    height: 1px !important;
    background-color: rgba(211, 216, 230, 0.6);
    margin-top: 10px !important;
  }
  :deep .ant-modal-close {
    position: absolute;
    top: 0;
    right: 17px;
    z-index: 10;
  }
  :deep .ant-modal-content {
    min-width: 1080px;
    height: 100%;
    background-color: var(--background-Color);
    border-radius: 4px;
    overflow: auto;
  }
  :deep .ant-modal-header {
    background-color: var(--background-Color);
  }
  .header {
    margin-left: 36px;
    font-family: Source Han Sans CN;
    font-weight: 700;
    color: var(--headerline-text-color);
    font-size: 13px;
    margin-bottom: 12px;
    margin-top: 8px;
    .live,
    .show-roi {
      margin-right: 4px;
    }
    .show-roi {
      margin-left: 24px;
    }
  }
  .top {
    display: flex;
  }
  .card-box {
    display: none;
  }
  .preview-img {
    margin-left: 3%;
    margin-right: 3%;

    .big-image {
      width: 100%;
      aspect-ratio: 16/9;
    }
  }

  .prev {
    min-width: 40px;
    height: 200px;
    background-color: var(--netural-color);
    border-radius: 24px;
    display: flex;
    align-items: center;
    margin-top: 8%;
    margin-left: 2%;

    &-icon {
      width: 16px;
      height: 16px;
      border-left: 2px solid #dadde6;
      border-bottom: 2px solid #dadde6;
      transform: translate(170%, 170%) rotate(45deg);
      cursor: pointer;
      margin-left: -10px;
      margin-bottom: 70px;
    }
    &:hover {
      background-color: #dde2ed;
      .prev-icon {
        width: 16px;
        height: 16px;
        border-left: 2px solid #dadde6;
        border-bottom: 2px solid #dadde6;
        transform: translate(170%, 170%) rotate(45deg);
        cursor: pointer;
        margin-left: -10px;
        margin-bottom: 70px;
        border-left: 2px solid #6b7499;
        border-bottom: 2px solid #6b7499;
      }
    }
  }
  .next {
    min-width: 40px;
    height: 200px;
    background-color: var(--netural-color);
    border-radius: 24px;
    display: flex;
    align-items: center;
    margin-top: 8%;
    margin-right: 2%;

    &:hover {
      background-color: #dde2ed;
      .next-icon {
        width: 16px;
        height: 16px;
        border-left: 2px solid #6b7499;
        border-bottom: 2px solid #6b7499;
        transform: translate(170%, 170%) rotate(225deg);
        cursor: pointer;
        margin-left: -15px;
        margin-bottom: 70px;
      }
    }
    &-icon {
      width: 16px;
      height: 16px;
      border-left: 2px solid #dadde6;
      border-bottom: 2px solid #dadde6;
      transform: translate(170%, 170%) rotate(225deg);
      cursor: pointer;
      margin-left: -15px;
      margin-bottom: 70px;
    }
  }
  .message {
    width: 100%;
    display: flex;
    justify-content: space-between;
    padding-left: 5px;

    .vertical-divider {
      background: #d3dae8;
      width: 1px;
      margin: 0 16px 0 16px;
    }

    .right{

      .face-img{
        margin: 0 auto;
        max-height: 100px;
        max-width: 100px;
        height: 120px;
        width: 100px;
      }

      .face-info{
        margin-top: 10px;
        line-height: 35px;
        display: flex;
        flex-direction: column;

        .face-name,
        .face-phone,
        .face-id-card,
        .face-remark {
          display: inline-flex;
        }

        .title{
          font-family: Source Han Sans CN;
          font-weight: 500;
          color: var(--headerline-text-color);
          font-size: 15px;
          font-weight: 600;
          margin-right: 15px;
        }

        .info{
          font-family: Source Han Sans CN;
          color: var(--headerline-text-color);
          font-size: 15px;
          width: 150px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

    }

    .name,
    .area,
    .date-time {
      padding-bottom: 16px;
    }
    .carema-name,
    .capture-location,
    .capture-time,
    .alarm-type,
    .alarm-level,
    .target-count,
    .audit-status,
    .review-comments {
      margin-right: 24px;
      font-family: Source Han Sans CN;
      font-weight: 500;
      color: var(--headerline-text-color);
      font-size: 15px;
      font-weight: 600;
    }
    .number,
    .color,
    .car-type {
      .title {
        margin-right: 24px;
        font-family: Source Han Sans CN;
        font-weight: 500;
        color: var(--headerline-text-color);
        font-size: 15px;
        font-weight: 600;
      }
    }
    .number,
    .color,
    .car-type {
      padding-bottom: 16px;
    }
    .review-comments {
      margin-top: -16px;
    }
    .number,
    .color,
    .car-type {
      .info {
        font-family: Source Han Sans CN;
        color: var(--headerline-text-color);
        font-size: 15px;
      }
    }

    .info {
      font-family: Source Han Sans CN;
      color: var(--headerline-text-color);
      font-size: 15px;
    }
    .type,
    .level {
      margin-top: 16px;
    }
    .level,
    .status,
    .comments {
      display: flex;
      align-items: center;
    }
    .count {
      margin-top: 16px;
      margin-bottom: 16px;
    }
    .status {
      padding-bottom: 16px;
    }
    .radio-box {
      align-items: center;
    }
  }

  .reason-info {
    display: flex;
    align-items: center;
    margin-top: -8px;

    .error-icon {
      width: 16px;
      height: 16px;
      background: url('/@/assets/svg/icon/info_icon_warning.svg');
      display: inline-block;
      margin: 0px 4px 0 0;
    }
    .reason {
      font-family: Source Han Sans CN;
      color: #adb4cc;
      font-size: 13px;
    }
  }
  :deep .ant-checkbox-wrapper {
    height: 28px;
    margin-left: 2px;
    .reson-item {
      font-family: Source Han Sans CN;
      color: #575b66;
      font-size: 13px;
    }
  }
  :deep .ant-checkbox-wrapper:hover .ant-checkbox-inner,
  .ant-checkbox:hover .ant-checkbox-inner,
  .ant-checkbox-input:focus + .ant-checkbox-inner {
    border-color: #7089ed;
  }
  :deep .ant-checkbox-checked .ant-checkbox-inner {
    background-color: #7089ed;
    border-color: #7089ed;
  }

  :deep .ant-checkbox-indeterminate .ant-tree-checkbox-inner::after {
    background-color: #7089ed;
  }
  :deep .ant-input,
  .ant-input-focused {
    &:focus,
    &:hover {
      border-color: #7089ed;
      // box-shadow: 0 0 0 2px rgb(24 144 255 / 20%);
      box-shadow: none;
      border-right-width: 1px !important;
      outline: 0;
    }
  }
  .operation {
    display: flex;
    justify-content: flex-end;
    margin-right: 1%;
    margin-top: 1%;
    .back-btn {
      margin-right: 12px;
    }
    .confirm-btn {
      margin-right: 36px;
    }
  }
  :deep .ant-carousel {
    margin-left: 40px;
    margin-right: 40px;
  }
  .ant-carousel :deep(.slick-arrow.custom-slick-arrow) {
    width: 25px;
    height: 25px;
    font-size: 25px;
    color: #fff;
    background-color: rgba(31, 45, 61, 0.11);
    opacity: 0.3;
    z-index: 1;
    top: 25px;
  }
  .ant-carousel :deep(.custom-slick-arrow:before) {
    display: none;
  }
  .ant-carousel :deep(.custom-slick-arrow:hover) {
    opacity: 0.5;
  }

  .ant-carousel :deep(.slick-slide img) {
    width: 214px;
    height: 120px;
    // background-color: #eff2fc;
    border-radius: 4px;
    // border-radius: 4px;

    margin-top: 8px;
  }
  :deep .ant-carousel .slick-arrow.custom-slick-arrow {
    width: 25px;
    height: 25px;
    font-size: 25px;
    color: #fff;
    background-color: rgba(31, 45, 61, 0.11);
    opacity: 0.3;
    z-index: 1;
    top: 75px;
  }
  :deep .ant-carousel .slick-slide {
    width: 214px;
    height: 120px;
    border-radius: 4px;
    // margin-left: 12px;
    margin-top: 8px;
    margin-bottom: 40px;
  }

  .thumbnail-item {
    position: relative;
    margin-left: 15px;
    .isActive {
      border: 4px dashed #7089ed;
    }

    .thumbnail-img {
      width: 214px;
      height: 120px;
      border-radius: 4px;
      &:hover {
        border: 2px solid;
        border-color: rgba(126, 150, 232, 0.4);
        border-radius: 4px;
      }
      &:active {
        border-color: #738ce6;
        border-radius: 4px;
      }
    }
    .label {
      position: absolute;
      top: 8px;
      left: 8px;
    }
  }
  .bottom {
    .progress {
      font-family: Source Han Sans CN;
      color: var(--headerline-text-color);
      font-size: 15px;
      padding: 24px 12px 16px 36px;
      font-weight: 600;
    }
    .num {
      font-family: DIN Condensed;
      font-weight: 700;
      color: #575b66;
      font-size: 24px;
      text-align: right;
    }
    .text {
      font-family: Source Han Sans CN;
      color: #adb4cc;
      font-size: 15px;
      text-decoration: underline;
      margin-left: 16px;
      cursor: pointer;
    }
    .no-num {
      font-family: Source Han Sans CN;
      color: #dadde6;
      font-size: 15px;
      text-decoration: underline;
      margin-left: 4%;
    }
    .prev-carousel {
      width: 24px;
      height: 72px;
      background-color: var(--netural-color);
      border-radius: 24px;
      display: flex;
      align-items: center;
      position: absolute;
      // right: -30px;
      top: 50px;
      z-index: 1;
      .prev-icon {
        width: 12px;
        height: 12px;
        border-left: 2px solid #dadde6;
        border-bottom: 2px solid #dadde6;
        transform: translate(170%, 170%) rotate(45deg);
        cursor: pointer;
        display: inline-block;
        margin-top: 8px;
      }
      &:hover {
        background-color: #dde2ed;
        .prev-icon {
          width: 12px;
          height: 12px;
          border-left: 2px solid #dadde6;
          border-bottom: 2px solid #dadde6;
          transform: translate(170%, 170%) rotate(45deg);
          cursor: pointer;
          border-left: 2px solid #6b7499;
          border-bottom: 2px solid #6b7499;
          display: inline-block;
          margin-top: 8px;
        }
      }
    }
    .next-carousel {
      width: 24px;
      height: 72px;
      background-color: var(--netural-color);
      border-radius: 24px;
      display: flex;
      align-items: center;
      position: absolute;
      right: -30px;
      top: 50px;
      &:hover {
        background-color: #dde2ed;
        .next-icon {
          width: 12px;
          height: 12px;
          border-left: 2px solid #6b7499;
          border-bottom: 2px solid #6b7499;
          transform: translate(170%, 170%) rotate(225deg);
          cursor: pointer;
          display: inline-block;
          margin-top: 8px;
        }
      }
      .next-icon {
        width: 12px;
        height: 12px;
        border-left: 2px solid #dadde6;
        border-bottom: 2px solid #dadde6;
        transform: translate(170%, 170%) rotate(225deg);
        cursor: pointer;
        display: inline-block;
        margin-top: 8px;
      }
    }
  }
</style>
