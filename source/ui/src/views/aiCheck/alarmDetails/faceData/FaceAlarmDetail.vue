<template>
  <a-modal title="详细信息"
           :width="800"
           v-model:visible="visible"
           :get-container="getContainer" :body-style="{margin: 0 , padding: 0 }">
    <div class="top">
      <div class="prev" @click="goPrev">
        <span class="prev-icon"></span>
      </div>
      <div class="preview-img">
        <img :src="getImgUrl(0)" alt="" class="big-image" @error="handleImageError" />
      </div>
      <div class="next" @click="goNext">
        <span class="next-icon"></span>
      </div>
      <div class="message">
        <div class="left">
          <div class="right"></div>
          <div class="name">
            <span class="carema-name">相机名称</span>
            <span class="info">相机名称</span>
          </div>
          <div class="area">
            <span class="capture-location">自定义分组</span>
            <span class="info">自定义分组</span>
          </div>
          <div class="date-time">
            <span class="capture-time">抓拍时间</span>
            <span class="info">自定义分组</span>
          </div>
          <div>
            <span class="alarm-type">事件类型</span>
            <span class="info"> 事件类型 </span>
          </div>
          <div class="level">
            <span class="alarm-level">事件类型</span>

          </div>
          <div class="count">
            <div >
              <span class="target-count">目标数量</span>
              <span class="info">目标数量</span>
            </div>
          </div>

        </div>
      </div>
    </div>
    <Carousel ref="carousel" :dots="false"
              :slides-to-show="detailList.length < 7 ? detailList.length : 7"
              :slides-to-scroll="1" :infinite="false" arrows>
      <template #prevArrow>
        <div class="prev-carousel">
          <span class="prev-icon"></span>
        </div>

      </template>
      <template #nextArrow>
        <div class="next-carousel">
          <span class="next-icon"></span>
        </div>
      </template>

      <template v-for="(item, index) in detailList" :key="index">
        <div class="thumbnail-item" @dblclick="handleshowBigImg(item)">
          <img
            :src="item.img"
            class="thumbnail-img"
            alt=""
            @error="handleImageError"
          />
          <div class="label"> <BaseLabel :label="'pendingReview'" /></div>
        </div>
      </template>
    </Carousel>

    <template #footer>
      <a-button type="primary" @click="visible = false">关闭</a-button>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import {ref} from "vue";
import {LeftCircleOutlined, RightCircleOutlined} from "@ant-design/icons-vue";
import {Carousel, Col, Divider, Row} from "ant-design-vue";
import BaseLabel from "/@/components/BaseLabel/BaseLabel.vue";
import BaseRadio from "/@/components/BaseRadio/BaseRadio.vue";

defineProps(['getContainer'])
const visible = ref(false)


const getImgUrl = (i: number) => {
  console.log(i)
  return `http://192.168.1.180:9980//face/group20250319103049/159cd8604961418289a7fcda934914ee.jpg`;
}

const detailList = ref([
  {
    id: 1,
    name: '张三',
    img: getImgUrl(1),
  },
  {
    id: 2,
    name: '李四',
    img: getImgUrl(2),
  },
  {
    id: 3,
    name: '王五',
    img: getImgUrl(3),
  },
  {
    id: 4,
    name: '赵六',
    img:getImgUrl(4),
  }
])
;

const open = () => {
  visible.value = true
}

defineExpose({
  open,
})

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

  .vertical-divider {
    background: #d3dae8;
    width: 1px;
    margin: 0 16px 0 16px;
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
  margin-right: 10%;
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
    margin-left: 16px;
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
