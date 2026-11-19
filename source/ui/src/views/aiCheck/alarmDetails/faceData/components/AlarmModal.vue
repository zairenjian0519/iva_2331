<template>
  <div class="alarm-modal" ref="alarmModal">
    <Modal
      v-model:visible="dialogVisible"
      width="80%"
      :centered="true"
      @ok="handleOk"
      title="事件详情"
      dialogClass="dialog"
      :get-container="getContainer"
      :footer="null"
      v-if="dialogVisible"
      :destroy-on-close="true"
      :body-style="{ paddingTop: '10px' }"
    >

      <div class="top">
        <div class="prev" @click="goPrev">
          <span class="prev-icon"></span>
        </div>
        <div class="preview-img">
          <!-- 传过来的图片props -->
          <img
            :src="path === '/viewPreview/view' ? '/kyscene' + detailInfo?.picPath + '?' + detailInfo.alarmId : detailInfo?.picPath"
            alt="" class="big-image" @error="handleImageError"/>
        </div>
        <div class="next" @click="goNext">
          <span class="next-icon"></span>
        </div>
        <div class="message">
          <div class="left">
            <div class="itemContainer">
              <span class="labelFont">相机名称：</span>
              <span class="info">{{ detailInfo?.cameraName }}</span>
            </div>
            <div class="itemContainer">
              <span class="labelFont">抓拍时间：</span>
              <span class="info">{{ detailInfo?.alarmTime }}</span>
            </div>

            <div class="itemContainer">
              <span class="labelFont">是否在库：</span>
<!--              <span class="info"></span>-->
              <a-tag style="height: 25px; margin-top: 5px" :color="detailInfo.vectorId ? 'green' : 'red'">{{ detailInfo.vectorId ? '在库' : '不在库' }}</a-tag>
            </div>

            <div class="itemContainer">
              <span class="labelFont">人员组名：</span>
              <span class="info">{{detailInfo.groupName? detailInfo.groupName : '无'}}</span>
            </div>

            <div class="itemContainer">
              <span class="labelFont">人员姓名：</span>
              <span class="info">{{ detailInfo.faceName? detailInfo.faceName : '无' }}</span>
            </div>

            <div class="itemContainer">
              <span class="labelFont">证件号码：</span>
              <span class="info">{{ detailInfo.faceIdCard? detailInfo.faceIdCard : '无' }}</span>
            </div>

            <div class="itemContainer">
              <span class="labelFont">手机号码：</span>
              <span class="info">{{ detailInfo.facePhone? detailInfo.facePhone : '无' }}</span>
            </div>

            <div class="itemContainer">
              <span class="labelFont">备注信息：</span>
              <span class="info">{{ detailInfo.remark? detailInfo.remark : '无' }}</span>
            </div>

            <div class="itemContainer">
              <span class="labelFont">库内照片：</span>
              <img v-if="detailInfo.vectorId" class="faceImg" :src="detailInfo.faceUrl" alt="" />
              <span class="info" v-else>无</span>
            </div>
          </div>
        </div>
      </div>
      <div class="bottom">
        <!-- 数量 -->
        <div class="carousel">
          <Carousel ref="carousel" :dots="false"
                    :slides-to-show="props.detailList.length < 7 ? props.detailList.length : 7"
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

            <div v-for="(item, id) in mainState.detailList" :key="id" class="card-box">
              <div class="thumbnail-item" @dblclick="handleshowBigImg(item)">
                <img
                  :src="path === '/viewPreview/view' ? '/kyscene' + item?.thumbnailPath + '?' + item?.alarmId : item?.thumbnailPath"
                  class="thumbnail-img"
                  :class="{ isActive: item.id === detailInfo.id }"
                  alt=""
                  @error="handleImageError"/>
              </div>
            </div>
          </Carousel>
        </div>
      </div>
    </Modal>
  </div>
</template>
<script lang="ts" setup>
import {Carousel, Modal} from 'ant-design-vue';
import {computed, defineEmits, onMounted, reactive, ref, unref, watch} from 'vue';
import errorIcon from '/@/assets/images/error.png';
import {useMessage} from '/@/hooks/web/useMessage';
import {useRouter} from 'vue-router';
import {tableItem} from "/@/views/face/faceData/AlarmDetails.data";

const {currentRoute} = useRouter();
const path = unref(currentRoute).path;

const emit = defineEmits(['val', 'handleOk', 'handleCancel', 'goPrev', 'goNext', 'handleCommit', 'showBigImg']);
const props = defineProps<{
  visible: Boolean;
  detailInfo: tableItem;
  detailList: tableItem[];
  index: number;
}>();

const {createMessage} = useMessage();

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
  newVal: [] as tableItem[],
  detailList: [] as tableItem[],
  value: 1,
  isCurrent: false,
  current: '0',
  reasonArr: [] as string[],
});
const detailList = ref(props.detailList);
const alarmModal = ref<HTMLElement | string | false | undefined>();
const carousel = ref<HTMLElement | null>(null);

onMounted(() => {
});

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
  },
  {deep: true, immediate: true}
);

watch(
  () => props.visible,
  (v) => {
    if (v) {
      detailList.value = props.detailList;
    }
  },
  {deep: true, immediate: true}
);
watch(
  () => props.detailList,
  (val) => {
    mainState.detailList = val;
  },
  {deep: true, immediate: true}
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


//点击确定
const handleCommit = async () => {

  emit('handleCommit', auditParams);
};

</script>
<style lang="less" scoped>

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
    min-width: 700px;
    min-height: 400px;
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

  .left{

    line-height: 35px;

    .itemContainer {
      display: flex;

      .labelFont {
        margin-right: 24px;
        font-family: Source Han Sans CN;
        font-weight: 500;
        color: var(--headerline-text-color);
        font-size: 15px;
        font-weight: 600;
      }

      .info {
        font-family: Source Han Sans CN;
        color: var(--headerline-text-color);
        font-size: 15px;
        max-width: 400px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .faceImg{
        margin-top: 10px;
        max-height: 100px;
        min-height: 100px;
        min-width: 100px;
        max-width: 100px;
      }
    }
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
  border-radius: 4px;

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

  .text {
    font-family: Source Han Sans CN;
    color: #adb4cc;
    font-size: 15px;
    text-decoration: underline;
    margin-left: 16px;
    cursor: pointer;
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
