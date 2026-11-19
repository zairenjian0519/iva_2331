<template>
  <div class="alarm-card">
    <div class="alarm-list">
      <a-row>
        <a-col :span="6" v-for="item in cardList" :key="item.id" :class="{ active: currentIndex.indexOf(item.id) > -1 }" @click="handleSelected(item, item.id)">
          <div class="box">
            <div class="wrap">
              <div class="card">
                <img :src="item.thumbnailPath" alt="" @error="handleImageError" />
                <img v-if="item.faceUrl" class="faceImg" :src="item.faceUrl" alt="">
              </div>
              <div class="mesage">
                <div class="top">
                  <div class="verify-result">
                    <BaseLabel :label="getResultLabel(item)" />
                  </div>
                  <div class="alarm-level">
                    <BaseLabel :label="getLevelLabel(item)" />
                  </div>
                </div>
                <div class="bottom">
                  <div class="camera">
                    <i class="camera-icon"></i>
                    {{ item.cameraName ? item.cameraName : '-' }}
                  </div>
                  <div class="target">
                    <i class="target-icon"></i>
                    <template v-if="item.alarmType == '17'">
                      覆盖比例 <span class="num">[{{ item?.targetCount }}%]</span>
                    </template>
                    <template v-else-if="item.alarmType == '18'">
                      车牌号 <span class="num">[{{ item?.carNumber }}]</span>
                    </template>
                    <template v-else>
                      目标数量 <span class="num">[{{ item?.targetCount }}]</span>
                    </template>
                  </div>
                  <div class="test-type">
                    <i class="test-type-icon"></i>
                    {{ item.alarmTypeDict }}
                    <span style="color: #a3a7b5; margin-left: 5px">[{{ item.isRotate == 0 ? '实时' : item.isRotate == 1 ? '轮询' : '实时' }}] </span>
                  </div>
                  <div class="time">
                    <i class="time-icon"></i>
                    {{ item.alarmTime ? item.alarmTime : '-' }}
                  </div>
                </div>
              </div>
            </div>
            <p class="divider"></p>
            <div class="operation">
              <button class="delete" @click.stop="handleCardDelete(item)">删除</button>
              <button class="exam" @click.stop="handleCardAudit(item)" v-auth="'alarm:audit'">审核</button>
            </div>
          </div>
          <div class="selected">
            <div class="right"><i class="check-icon"></i></div>
          </div>
        </a-col>
      </a-row>
    </div>
  </div>
  <div> <slot></slot></div>
</template>
<script lang="ts" setup>
  import { Col, Row } from 'ant-design-vue';
  import { cardData } from '../AlarmDetails.data';
  import { reactive, defineEmits, onMounted } from 'vue';
  import BaseLabel from '/@/components/BaseLabel/BaseLabel.vue';
  import { AlarmLevelI2V, AuditResult, AuditStatus } from '../AlarmDetails.data';
  import errorIcon from '/@/assets/images/error.png';
  defineProps<{
    cardList: cardData[];
  }>();

  // const mainState = reactive({
  //   isShowModal: false,
  //   vertifyInfo: {},
  //   curIndex: 0 as number,
  // });
  const ACol = Col;
  const ARow = Row;
  const emits = defineEmits(['handleChange', 'handleShowSizeChange', 'handleSelected', 'handleCardGoPrev', 'handleCardGoNext', 'handleCardAudit', 'handleCardDelete']);

  onMounted(() => {
    // console.log(cardList, 'cardList');
  });

  let currentIndex: string[] = reactive([]);

  //缩略图加载失败
  const handleImageError = (e) => {
    setTimeout(() => {
      const img = e.srcElement;
      img.src = errorIcon;
      img.onerror = null;
    }, 100);
  };
  // 返回等级标签
  const getLevelLabel = (item) => {
    return AlarmLevelI2V[item.alarmLevel] || '';
  };

  // 返回结果标签
  const getResultLabel = (item) => {
    if (item.auditStatus === AuditStatus.NoAudit) {
      return 'pendingReview';
    }
    if (item.auditResult === AuditResult.FAIL) {
      return 'cancel';
    }
    if (item.auditResult === AuditResult.PASS) {
      return 'adopt';
    }
  };

  // 选中
  const handleSelected = (item, id) => {
    // //再次点击取消选中
    //获取点击的索引
    let arrIndex = currentIndex.indexOf(id);
    //如果没有则push,有就取消
    if (arrIndex > -1) {
      currentIndex.splice(arrIndex, 1);
    } else {
      currentIndex.push(id);
    }
    emits('handleSelected', currentIndex);
  };

  //点击审核
  const handleCardAudit = (item) => {
    emits('handleCardAudit', item);
  };
  //点击删除
  const handleCardDelete = (item) => {
    emits('handleCardDelete', item);
  };
</script>
<style lang="less" scoped>
  .alarm-card {
    margin: 0px 16px 0px 16px;
    height: 588px;
    overflow: auto;
    .alarm-list :deep(.ant-row > div) {
      background-color: transparent;
      padding: 0px 8px 16px 8px;
    }
    .alarm-list :deep(.ant-row > div:nth-last-child(-n + 4)) {
      background-color: transparent;
      padding: 0px 8px 0 8px;
      background: var(--background-Color);
    }

    .active {
      .box {
        background-color: rgba(115, 140, 230, 0.2);
        border: 1px solid rgba(115, 140, 230, 0.4);
      }
      .selected {
        display: block !important;
        background-color: #738ce6 !important;
      }
    }

    .box {
      height: 184px;
      position: relative;
      border: 1px solid var(--fill-color);
      border-radius: 4px;
      .wrap {
        display: flex;
      }
      .mesage {
        min-width: 140px;
        flex: 1;
      }
      .card {
        position: relative;
        min-width: 0;
        margin: 16px 16px 0 16px;
        img {
          width: 200px;
          height: 116px;
          border-radius: 4px;
          object-fit: fill;
          flex-shrink: 0;
        }
        .faceImg {
          position: absolute;
          top: 0;
          right: 0;
          border-bottom-left-radius: 5px;
          border-top-right-radius: 5px;
          width: 50px;
          max-width: 50px;
          height: 50px;
          max-height: 50px;
        }
      }
      &:hover {
        border-color: rgba(126, 150, 232, 0.4);
        .selected {
          display: block;
        }
      }
      .bottom {
        margin: 0px 0 0 0;
        flex: 1;
      }
      .selected {
        width: 16px;
        height: 16px;
        background-color: rgba(126, 150, 232, 0.4);
        border-radius: 50%;
        position: absolute;
        top: 60%;
        left: 3%;
        display: none;
        .check-icon {
          width: 8.4px;
          height: 8.7px;
          display: inline-block;
          background: url('/@/assets/svg/icon/icon_check.svg');
          position: absolute;
          top: 14%;
          left: 18%;
        }
      }
    }

    .top {
      display: flex;
      padding-top: 16px;
      // padding-bottom: 12px;
      .verify-result {
        margin-right: 8px;
      }
    }
    .target,
    .test-type,
    .camera,
    .time {
      opacity: 79%;
      font-family: Source Han Sans CN;
      color: var(--secondary-text-color);
      font-size: 12px;
      display: flex;
      align-items: center;
    }
    .test-type,
    .camera,
    .target {
      margin-bottom: 6px;
    }

    .target {
      &-icon {
        background: url('/@/assets/svg/icon/info_icon_location.svg');
        display: inline-block;
        width: 16px;
        height: 16px;
        margin-right: 4px;
      }
      .num {
        color: rgba(204, 102, 102, 1);
        margin: 0 0 0 4px;
      }
    }
    .camera {
      &-icon {
        background: url('/@/assets/svg/icon/info_icon_camera.svg');
        display: inline-block;
        width: 16px;
        height: 16px;
        margin-right: 4px;
      }
    }
    .test-type {
      &-icon {
        background: url('/@/assets/svg/icon/info_icon_warning.svg');
        display: inline-block;
        width: 16px;
        height: 16px;
        margin-right: 4px;
      }
    }
    .time {
      &-icon {
        background: url('/@/assets/svg/icon/info_icon_time.svg');
        display: inline-block;
        width: 16px;
        height: 16px;
        margin-right: 4px;
      }
    }
  }
  .divider {
    width: 100%;
    height: 0;
    margin-top: 13px;
    margin-bottom: 0;
    border-bottom: 1px solid var(--weak-split-line-color);
  }
  .operation {
    display: flex;
    justify-content: flex-end;
    padding: 8px 16px 8px 0;
  }
  .exam {
    font-family: Source Han Sans CN;
    color: var(--icon-Color);
    font-size: 13px;
    width: 42px;
    height: 20px;
    background-color: transparent;
    &:hover {
      background-color: var(--fill-three-color);
      border-radius: 4px;
    }
  }

  .delete {
    width: 42px;
    height: 20px;
    background-color: transparent;
    font-family: Source Han Sans CN;
    color: var(--serious-text-color);
    font-size: 13px;
    &:hover {
      background-color: rgba(204, 102, 102, 0.2);
      border-radius: 4px;
    }
  }
  .pagination {
    height: 40px;
  }
</style>
