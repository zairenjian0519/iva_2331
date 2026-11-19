<template>
  <div class="alarm-search">
    <div class="vertify">
      <span class="text">审核结果</span>
      <div class="vertify-result" v-for="(item, index) of mainState.vertify" :key="index">
        <template v-if="index === 0">
          <span class="vertify-result-item-zero" :class="{ actived: mainState.currentIndex === index }" @click="clickVertify(index)">{{ item.title }} </span>
          <span class="divider"></span>
        </template>

        <template v-else>
          <span class="vertify-result-item" :class="{ active: mainState.currentIndex === index }" @click="clickVertify(index)">{{ item.title }}</span>
        </template>
      </div>
    </div>
    <div class="alarm-level">
      <div class="num">
        <span class="text">事件等级</span>
        <div v-for="(item, ind) of mainState.level" :key="ind" class="alarm">
          <template v-if="ind === 0">
            <span class="alarm-level-item-zero" :class="{ isActive: mainState.currentLevelIndex === ind }" @click="clickLevel(ind)">{{ item.title + ' ' + props.allCount }}</span>
            <span class="divider"></span>
          </template>
          <template v-else>
            <div :class="[item.type, mainState.currentLevelIndex === ind ? 'isActive' : '']" @click="clickLevel(ind)">
              <span class="block"></span>
              <span class="alarm-level-item" :class="{ isSpanActive: mainState.currentLevelIndex === ind }">{{ item.title + ' ' + props[item.type + 'Count'] }}</span></div
            >
          </template>
        </div>
      </div>
      <div class="operation">
        <!-- <div class="delete" @click="handleBatchDelete">
          <i class="delete-icon"></i>
          删除
        </div>
        <div class="export" @click="handleExport" v-auth="'alarm:exportXls'">
          <i class="export-icon"></i>
          导出
        </div> -->
        <secondary-button label="一键清除" @click="clearAllAlarm"> <i class="delete-icon"></i></secondary-button>
        <secondary-button label="批量删除" class="delete-action" v-auth="'alarm:delete'" @click="handleBatchDelete">
          <i class="delete-icon"></i>
        </secondary-button>
        <secondary-button label="导出" class="export" v-auth="'alarm:exportXls'" @click="handleExport">
          <i class="export-icon"></i>
        </secondary-button>
        <Divider type="vertical" />
        <template v-if="props.isShowTable">
          <base-tooltip tooltip-title="切换到卡片页">
            <i class="change-card" @click="handleShowTable"></i>
          </base-tooltip>
        </template>
        <template v-else>
          <base-tooltip tooltip-title="切换到表格页">
            <i class="change-table" @click="handleShowTable"></i>
          </base-tooltip>
        </template>
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import {defineEmits, reactive} from 'vue';
import {Divider} from 'ant-design-vue';
import BaseTooltip from '/@/components/BaseTooltip/BaseTooltip.vue';
import SecondaryButton from '/@/components/BaseButton/SecondaryButton.vue';
import {AlarmLevel, AlarmLevelIndex, AuditResult, AuditStatus} from '../AlarmDetails.data';

const props = defineProps<{
    isShowTable: Boolean;
    allCount: number;
    seriousCount: number;
    warningCount: number;
    commonCount: number;
    remindCount: number;
    normalCount: number;
  }>();
  const emits = defineEmits(['handleShowTable', 'clickLevel', 'clickAudit', 'handleExport', 'handleBatchDelete', 'clearAllAlarm']);
  // const ATooltip = Tooltip;
  const mainState = reactive({
    vertify: [
      {
        title: '全部',
        status: '',
        result: '',
      },
      {
        title: '待审核',
        status: AuditStatus.NoAudit,
        result: '',
      },
      {
        title: '已通过',
        status: AuditStatus.Audited,
        result: AuditResult.PASS,
      },
      {
        title: '已作废',
        status: AuditStatus.Audited,
        result: AuditResult.FAIL,
      },
    ],
    level: [
      {
        title: '全部',
        type: AlarmLevel.All,
      },

      {
        title: '提醒',
        type: AlarmLevel.Remind,
      },
      {
        title: '一般',
        type: AlarmLevel.Common,
      },
      {
        title: '警告',
        type: AlarmLevel.Warning,
      },
      {
        title: '严重',
        type: AlarmLevel.Serious,
      },
      {
        title: '正常',
        type: AlarmLevel.Normal,
      },
    ],
    currentIndex: 0,
    currentLevelIndex: 0,
    selectedKeys: [] as string[],
  });

  const clickVertify = async (index: number) => {
    mainState.currentIndex = index;
    emits('clickAudit', mainState.vertify[index]);
  };

  const clickLevel = async (ind: number) => {
    mainState.currentLevelIndex = ind;
    emits('clickLevel', AlarmLevelIndex[mainState.level[ind].type]);
  };

  const handleShowTable = () => {
    emits('handleShowTable');
  };

  //一键清除
  const clearAllAlarm = () => {
    emits('clearAllAlarm');
  };

  //多选删除
  const handleBatchDelete = () => {
    emits('handleBatchDelete');
  };
  //导出
  const handleExport = async () => {
    emits('handleExport');
  };
</script>
<style lang="less" scoped>
  .alarm-search {
    .divider {
      width: 1px;
      height: 12px;
      background-color: rgba(211, 216, 230, 0.6);
      margin-left: 8px;
    }
    .vertify {
      display: flex;
      .text {
        padding-top: 24px;
        margin-right: 16px;
        margin-bottom: 16px;
      }
      .vertify-result {
        display: flex;
        margin-right: 8px;
        text-align: center;
        align-items: center;
        height: 22px;
        line-height: 22px;
        margin-top: 24px;
        .active,
        .actived {
          background: var(--netural-color);
          font-family: Source Han Sans CN;
          color: var(--cancel-text-color);
          font-size: 12px;
          border: 1px solid transparent;
          border-radius: 4px;
        }
        &-item {
          width: 80px;
          height: 22px;
          background: var(--background-Color);
          border: 1px solid #d3d9e8;
          cursor: pointer;
          font-family: Source Han Sans CN;
          color: var(--secondary-text-color);
          font-size: 12px;
          border-radius: 4px;
        }
        &-item-zero {
          width: 96px;
          height: 22px;
          background: var(--background-Color);
          border: 1px solid #d3d9e8;
          cursor: pointer;
          font-family: Source Han Sans CN;
          color: var(--secondary-text-color);
          font-size: 12px;
          border-radius: 4px;
        }
      }
    }
    .alarm-level {
      display: flex;
      justify-content: space-between;
      cursor: pointer;
      min-width: 1070px;

      .num {
        display: flex;
      }

      .text {
        margin-right: 16px;
        margin-bottom: 16px;
      }
      .alarm {
        display: flex;
        margin-right: 8px;
        text-align: center;
        align-items: center;
        height: 22px;
        line-height: 22px;
        .isActive {
          background: var(--netural-color);
          font-family: Source Han Sans CN;
          border: none;
          color: var(--cancel-text-color);
          font-size: 12px;
        }
      }
      &-item {
        margin-left: 8px;
        cursor: pointer;
        background-color: var(--background-Color);
        border-color: #d3d9e8;
        font-family: Source Han Sans CN;
        color: var(--secondary-text-color);
        font-size: 12px;
        text-align: center;
        &-zero {
          border: 1px solid #d3d9e8;
          width: 96px;
          height: 22px;
          border-radius: 4px;
        }
      }
      .serious,
      .common,
      .warning,
      .remind,
      .normal {
        .isSpanActive {
          width: 72px;
          height: 22px;
          background: var(--netural-color);
          border: 1px solid var(--netural-color);
          color: var(--cancel-text-color);
          font-size: 12px;
        }
      }

      .serious {
        width: 112px;
        height: 22px;
        background-color: var(--background-Color);
        border: 1px solid #d3d9e8;
        border-radius: 4px;
        .block {
          width: 20px;
          height: 10px;
          background: var(--serious-text-color);
          border-radius: 2px;
          display: inline-block;
          border-radius: 2px;
        }
      }

      .warning {
        width: 112px;
        height: 22px;
        background-color: var(--background-Color);
        border: 1px solid #d3d9e8;
        border-radius: 4px;

        .block {
          width: 20px;
          height: 10px;
          background: var(--warning-text-color);
          border-radius: 2px;
          display: inline-block;
        }
      }
      .common {
        width: 112px;
        height: 22px;
        background-color: var(--background-Color);
        border: 1px solid #d3d9e8;
        border-radius: 4px;

        .block {
          width: 20px;
          height: 10px;
          background: var(--common-text-color);
          border-radius: 2px;
          display: inline-block;
        }
      }
      .remind {
        width: 112px;
        height: 22px;
        background-color: var(--background-Color);
        border: 1px solid #d3d9e8;
        border-radius: 4px;

        .block {
          width: 20px;
          height: 10px;
          background: var(--remind-text-color);
          border-radius: 2px;
          display: inline-block;
        }
      }
      .normal {
        width: 112px;
        height: 22px;
        background-color: var(--background-Color);
        border: 1px solid #d3d9e8;
        border-radius: 4px;

        .block {
          width: 20px;
          height: 10px;
          background: var(--adopt-text-color);
          border-radius: 2px;
          display: inline-block;
        }
      }
      .operation {
        display: flex;
        margin-top: -2px;
        .ant-divider-vertical {
          width: 1px;
          height: 14px;
          background-color: rgba(211, 216, 230, 0.9);
          border-radius: 1px;
          top: 5px;
          left: 3px;
        }

      }
      .delete-action,
      .export {
        margin-left: 12px;

        &:hover {
          .delete-icon {
            background: url('/@/assets/svg/icon/editor_btn_delete_hover.svg');
            width: 14px;
            height: 14px;
          }
          .export-icon {
            background: url('/@/assets/svg/icon/editor_btn_export_hover.svg');
          }
          .import-icon {
            background: url('/@/assets/svg/icon/editor_btn_import_hover.svg');
          }
        }
      }

      .export-icon {
        display: inline-block;
        width: 14px;
        height: 14px;
      }
      .delete-icon {
        background: url('/@/assets/svg/icon/editor_btn_delete_default.svg');
        width: 20px;
        height: 20px;
        display: inline-block;
      }
      .export-icon {
        background: url('/@/assets/svg/icon/editor_btn_export_default.svg');
      }
      .import-icon {
        background: url('/@/assets/svg/icon/editor_btn_import_default.svg');
      }
      .change-table {
        background: url('/@/assets/svg/icon/editor_btn_cardlist_default.svg');
        display: inline-block;
        width: 24px;
        height: 24px;
        margin-right: 28px;
        &:hover {
          background: url('/@/assets/svg/icon/editor_btn_cardlist_hover.svg');
        }
      }
      .change-card {
        background: url('/@/assets/svg/icon/editor_btn_list_default.svg');
        display: inline-block;
        width: 24px;
        height: 24px;
        margin-right: 28px;
        &:hover {
          background: url('/@/assets/svg/icon/editor_btn_list_hover.svg');
        }
      }
    }
  }
</style>
