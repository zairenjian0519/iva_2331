<template>
  <div ref="ROIModal">
    <a-modal
      :title="titleModal"
      width="1290px"
      v-if="dialogVisible"
      :ok-text="okTextModal"
      centered
      v-model:visible="dialogVisible"
      @ok="handleOk"
      @cancel="handleCancel"
      @open="handleOpen"
      :get-container="getContainer"
    >
      <a-row>
        <a-col :span="4">
          <div class="divider">
            <div class="sum">轮询组相机数{{ sum }}台</div>
            <tab-control :title="label" @click-item="handleClickItem" class="tabs" />
            <ROITree class="tree" :tree-data="treeData" :selected-key="selectedKeysVal" :auto-expand-parent="autoExpandParent" @select="handleSelect" />
          </div>
        </a-col>

        <a-col :span="20">
          <span class="name">{{ cameraName }}</span>
          <div class="dot"></div> <span class="text">当前时间{{ currentDate }}</span>
          <slot></slot>
          <!-- <a-row>
            <a-col v-if="cameraId">
              <a-row>
                <a-col span="24" class="container">
                  <a-button class="full" :class="roiAreaType === ROIAreaType.Full ? 'roi-btn roi-btn-active roi-btn-poly-active' : 'roi-btn'" size="small" @click="allImgBtnClick"
                    ><img style="float: left; margin-top: 4px; margin-right: 4px" :src="allBtnIcon" alt="" /> 全屏</a-button
                  >
                  <a-button :class="roiAreaType === ROIAreaType.Rect ? 'roi-btn roi-btn-active roi-btn-poly-active' : 'roi-btn'" size="small" @click="rectBtnClick"
                    ><img style="float: left; margin-top: 4px; margin-right: 4px" :src="rectangleBtnIcon" alt="" /> 矩形</a-button
                  >
                  <a-button :class="roiAreaType === ROIAreaType.Polygon ? 'roi-btn roi-btn-active roi-btn-poly-active' : 'roi-btn'" size="small" @click="mulBtnClick"
                    ><img style="float: left; margin-top: 4px; margin-right: 4px" :src="polygonBtnIcon" alt="" />多边形</a-button
                  >
                  <a-button class="reset-btn" size="small" @click="clearROI"><img style="float: left; margin-top: 4px; margin-right: 4px" :src="resetBtnIcon" alt="" /> 清空屏幕</a-button>
                </a-col>
              </a-row>
              <a-row style="margin-top: 10px; margin-bottom: 10px">
                <a-col span="24" class="wrap">
                  <p class="tips">
                    <i class="tips-icon"></i>
                    <span class="tips-text">当前最小识别像素 91×54</span>
                  </p>
                  <template v-if="loading">
                    <div class="loading">
                      <i class="loading-icon"></i>
                      <div>图像加载中，请稍后…</div>
                    </div>
                  </template>
                  <template v-else>
                    <div class="operation-area">
                      <div class="prev" @click="goPrev">
                        <span class="prev-icon"></span>
                      </div>
                      <img :src="roiImgSrc" style="display: none" class="image" @onload="loadROIImg" />
                      <canvas ref="cameraCanvas" width="800" height="495" style="border: 1px solid"> 您的浏览器不支持 HTML5 canvas 标签。 </canvas>
                      <canvas ref="cameraSaveCanvas" width="800" height="495" @mousedown="canvasMouseDown($event)" @mouseup="canvasMouseUp($event)" @mousemove="canvasMouseMove($event)"></canvas>

                      <div class="next" @click="goNext">
                        <span class="next-icon"></span>
                      </div>
                    </div>
                  </template>
                </a-col>
              </a-row>
            </a-col>
            <a-col v-else span="24" style="text-align: -webkit-center; margin-bottom: 10px">
              <div style="background-color: grey; text-align: center; width: 80%; height: 400px; padding-top: 200px">
                <label>暂未选定相机</label>
              </div>
            </a-col>
          </a-row> -->
        </a-col>
      </a-row>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';
  import { Col, Row } from 'ant-design-vue';
  import { Modal } from 'ant-design-vue';
  import TabControl from '/@/components/TabControl/TabControl.vue';
  import { TreeNodeData } from '../CameraGroup.data';

  import ROITree from './ROITree.vue';

  const AModal = Modal;
  const ARow = Row;
  const ACol = Col;

  const props = defineProps<{
    title: string;
    visible: boolean | undefined;
    okText: string;
    label: Array<string>;
    treeData: TreeNodeData[];
    autoExpandParent: boolean;
    cameraName: string;
    cameraId: string;
    // roiImgSrc: string;
    // loading: boolean;
    sum: number;
    selectedKeys: string[];
  }>();
  const emits = defineEmits(['val', 'selectedKey', 'handleOk', 'handleCancel', 'title', 'okText', 'cancelText', 'handleClickItem', 'handleSelect', 'handleOpen']);
  //   当前时间
  const date: Date = new Date();
  const currentDate = date.toLocaleString();
  const ROIModal = ref<HTMLElement | string | false>();

  const dialogVisible = computed({
    get() {
      return props.visible;
    },
    set(val) {
      emits('val', val);
    },
  });

  const titleModal = computed({
    get() {
      return props.title;
    },
    set(val) {
      emits('title', val);
    },
  });

  const selectedKeysVal = ref<string[]>();
  watch(
    () => props.selectedKeys,
    () => {
      selectedKeysVal.value = props.selectedKeys;
    },
    { immediate: true, deep: true }
  );

  const okTextModal = computed({
    get() {
      return props.okText;
    },
    set(val) {
      emits('okText', val);
    },
  });

  // 制定modal挂载的节点
  const getContainer = () => {
    return ROIModal.value;
  };

  //新建
  const handleOk = () => {
    emits('handleOk');
  };

  //取消
  const handleCancel = () => {
    emits('handleCancel');
  };
  const handleOpen = () => {
    emits('handleOpen');
  };
  //tab点击
  const handleClickItem = (index: number) => {
    emits('handleClickItem', index);
  };

  //单个树节点点击
  const handleSelect = async (item) => {
    emits('handleSelect', item);
  };
</script>
<style lang="less" scoped>
  :deep .ant-modal {
    height: 723px;
    font-family: Source Han Sans CN;
    .sum {
      margin: 16px 0 12px 24px;
      color: var(--tips-text-color);
      font-size: 12px;
    }
    .info {
    }
    .divider {
      border-right: 1px solid var(--weak-split-line-color);
    }
    .tree {
      margin: 12px 0 0 0;
      height: 350px;
      overflow-y: scroll;
    }
    .name {
      margin: 16px 8px 12px 24px;
      font-weight: 550;
    }
    .text {
      color: #7f8599;
      font-size: 12px;
    }

    .dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background-color: var(--adopt-text-color);
      margin-right: 8px;
      margin-top: 20px;
      display: inline-block;
    }

    .roi-btn {
      margin: 12px 12px 0 0;
      border-radius: 6px;
    }
    .full {
      margin: 0 12px 0 24px;
    }
    .reset-btn {
      float: right;
      border-radius: 6px;
      border: 0;
    }

    .roi-btn-active {
      color: var(--primary-Color);
      border-color: var(--primary-Color);
    }
    .tips {
      font-family: Source Han Sans CN;
      color: #7f8599;
      font-size: 11px;
      display: flex;
      height: 20px;

      &-text {
        padding: 2px 0;
      }

      &-icon {
        background: url('/@/assets/svg/icon/info_icon_tips.svg');
        width: 16px;
        height: 16px;
        display: inline-block;
        margin: 2px 4px 0 24px;
      }
    }
    .operation-area {
      display: flex;
    }

    .prev {
      min-width: 40px;
      height: 200px;
      background-color: var(--netural-color);
      border-radius: 24px;
      display: flex;
      align-items: center;
      margin-top: 8%;

      margin: 8% 2% 0 2%;

      &-icon {
        width: 16px;
        height: 16px;
        border-left: 2px solid var(--ash-text-color);
        border-bottom: 2px solid var(--ash-text-color);
        transform: translate(170%, 170%) rotate(45deg);
        cursor: pointer;
        margin-left: -10px;
        margin-bottom: 70px;
      }
      &:hover {
        background-color: var(--fill-color);
        .prev-icon {
          width: 16px;
          height: 16px;
          border-left: 2px solid var(--ash-text-color);
          border-bottom: 2px solid var(--ash-text-color);
          transform: translate(170%, 170%) rotate(45deg);
          cursor: pointer;
          margin-left: -10px;
          margin-bottom: 70px;
          border-left: 2px solid var(--icon-hover);
          border-bottom: 2px solid var(--icon-hover);
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
      margin-left: 2%;

      &:hover {
        background-color: var(--fill-color);
        .next-icon {
          width: 16px;
          height: 16px;
          border-left: 2px solid var(--icon-hover);
          border-bottom: 2px solid var(--icon-hover);
          transform: translate(170%, 170%) rotate(225deg);
          cursor: pointer;
          margin-left: -15px;
          margin-bottom: 70px;
        }
      }
      &-icon {
        width: 16px;
        height: 16px;
        border-left: 2px solid var(--ash-text-color);
        border-bottom: 2px solid var(--ash-text-color);
        transform: translate(170%, 170%) rotate(225deg);
        cursor: pointer;
        margin-left: -15px;
        margin-bottom: 70px;
      }
    }
    .image {
      width: 880px;
      height: 496px;
    }
    .ant-modal-footer {
      display: flex !important;
      justify-content: flex-end !important;
      align-items: center;
    }
    .ant-modal-footer .ant-btn {
      width: 64px;
      height: 24px;
      background-color: var(--fill-color);
      border-radius: 4px;
      color: var(--secondary-text-color);
      cursor: pointer;
      padding-top: 2px;
    }
    .ant-modal-footer .ant-btn:hover {
      background-color: var(--fill-two-color);
      color: var(--primary-Color);
    }
    .ant-modal-footer .ant-btn:active {
      background-color: var(--input-text-color);
      border-color: #7089ed;
      color: var(--primary-Color-actived);
    }
    .ant-modal-footer .ant-btn-primary {
      margin-left: 8px;
      width: 64px;
      height: 24px;
      background-color: var(--primary-Color);
      border-radius: 4px;
      color: rgba(255, 255, 255, 0.95);
      border: none;
      cursor: pointer;
    }

    .ant-modal-footer .ant-btn-primary:hover {
      background-color: var(--primary-Color-hover);
      color: rgba(255, 255, 255, 0.95);
    }
    .ant-modal-footer .ant-btn-primary:active {
      background-color: var(--primary-Color-actived);
    }
  }
</style>
