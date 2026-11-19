<template>
  <div class="alarm-table">
    <a-table
      :columns="columnData"
      :data-source="tableData"
      :scroll="{ y: 560 }"
      :pagination="false"
      :custom-row="onClickRow"
      :row-class-name="rowClass"
      rowKey="id"
      @change="handleRowChange"
      :row-selection="{ onChange: onSelectChange }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'operation'">
          <button class="exam" @click.stop="handleTableAudit(record)">查看</button>
          <button class="delete" @click.stop="handleTableDelete(record)">删除</button>
        </template>
      </template>
    </a-table>
  </div>
  <div v-show="tableData.length > 0"><slot></slot></div>
</template>
<script lang="ts" setup>
import {Table} from 'ant-design-vue';
import {AlarmLevelI2V, AuditResult, AuditStatus, columnData, tableItem} from '../AlarmDetails.data';
import {defineEmits, reactive} from 'vue';

import {useStore} from '/@/store/modules/alarmTable';

const emits = defineEmits(['handleChange', 'handleShowSizeChange', 'handleSortChange', 'selectedRows', 'handleTableGoNext', 'handleTableGoPrev', 'handleTableAudit', 'handleTableDelete']);
  const ATable = Table;
  // const ATooltip = Tooltip;
  const store = useStore();
  defineProps<{
    tableData: tableItem[];
    // total: number;
    // currentPage: number;
    // pageSize: number;
  }>();
  type Key = string | number;
  const state = reactive<{
    selectedRowKeys: Key[];
    loading: boolean;
  }>({
    selectedRowKeys: [], // Check here to configure the default column
    loading: false,
  });
  const mainState = reactive({
    isShowModal: false,
    vertifyInfo: {},
    field: '',
    order: '',
  });

  //选中行
  const onSelectChange = (selectedRowKeys: Key[]) => {
    state.selectedRowKeys = selectedRowKeys;
    emits('selectedRows', selectedRowKeys);
  };

  // 点击行
  const onClickRow = (record, index) => {
    return {
      onClick: (event) => {
        event.currentTarget.getElementsByClassName('ant-checkbox-wrapper')[0].click();
        store.setState(record.uid);
      },
    };
  };
  // 行颜色
  const rowClass = (record, index) => {
    return index === store.$state.index ? 'clickStyle' : '';
  };

  //点击审核
  const handleTableAudit = (item) => {
    emits('handleTableAudit', item);
  };
  //点击删除
  const handleTableDelete = (item) => {
    emits('handleTableDelete', item);
  };

  //分页、排序、筛选变化时触发
  const handleRowChange = (...args) => {
    mainState.field = args[2].field;
    mainState.order = args[2].order;
    emits('handleSortChange', mainState.field, mainState.order);
  };

  // 返回等级标签
  const getLevelLabel = (record) => {
    return AlarmLevelI2V[record.alarmLevel] || '';
  };

  // 返回结果标签
  const getResultLabel = (record) => {
    if (record.auditStatus === AuditStatus.NoAudit) {
      return 'pendingReview';
    }
    if (record.auditResult === AuditResult.FAIL) {
      return 'cancel';
    }
    if (record.auditResult === AuditResult.PASS) {
      return 'adopt';
    }
  };
</script>
<style>
  .overlay-class .ant-tooltip-inner {
    color: #000 !important;
  }
</style>
<style lang="less" scoped>
  .alarm-table {
    margin: 0px 24px 0px 24px;
    font-family: Source Han Sans CN;
    min-width: 980px;
    :deep .ant-table-tbody > tr:hover:not(.ant-table-expanded-row):not(.ant-table-row-selected) > td {
      background-color: var(--fill-four-color) !important;
    }
    :deep .ant-table-tbody > tr.ant-table-row-selected > td {
      background-color: var(--stripe-color);
    }
    :deep .ant-table-header {
      height: 45px;
      display: flex;
      border-bottom: 1px solid var(--ash-text-color);
      border-top: 1px solid transparent;
      border-left: 1px solid transparent;
      border-right: 1px solid transparent;
      border-top-left-radius: 4px;
      border-top-right-radius: 4px;
    }
    :deep .ant-table-thead > tr > th {
      font-weight: 600;
      color: var(--thead-text-color);
      font-size: 13px;
      padding: 12px;
      background-color: var(--fill-four-color);
    }
    :deep .ant-table-tbody > tr:first-child > td {
      border-bottom: 1px solid var(--weak-split-line-color);
      font-size: 13px;
      cursor: pointer;
      padding: 0px !important;
    }

    :deep th.ant-table-cell .operation {
      margin-right: 16px;
      display: flex;
    }
    :deep .ant-table-tbody > tr > td {
      border-bottom: 1px solid var(--weak-split-line-color);
      font-size: 13px;
      cursor: pointer;
      padding: 11px !important;
    }

    :deep .ant-table-thead > tr > th:not(:last-child):not(.ant-table-selection-column):not(.ant-table-row-expand-icon-cell):not([colspan])::before {
      position: absolute;
      top: 50%;
      right: 0;
      width: 1px;
      height: 0;
      background-color: rgba(0, 0, 0, 0.06);
      transform: translateY(-50%);
      transition: background-color 0.3s;
      content: '';
    }
    :deep .ant-table-column-sorter-up.active,
    :deep .ant-table-column-sorter-down.active {
      color: #7089ed;
    }

    // :deep  .ant-table-tbody > tr:hover {
    //   background-color: var(--fill-four-color)
    //   box-shadow: 0px 0px 6px rgba(51, 63, 102, 0.2);
    // }
    :deep .ant-checkbox-indeterminate .ant-checkbox-inner {
      /* background-color: #fff; */
      /* border-color: #000000; */

      background-color: #7089ed;
      border-color: #7089ed;
      &::after {
        width: 6px;
        height: 1.5px;
        background-color: #ffffff;
        border-radius: 0.4px;
      }
    }

    :deep .ant-checkbox-checked .ant-checkbox-inner {
      background-color: #738ce6;
      border-color: #738ce6;
    }
    :deep .ant-checkbox-wrapper:hover .ant-checkbox-inner,
    :deep .ant-checkbox:hover .ant-checkbox-inner,
    :deep .ant-checkbox-input:focus + .ant-checkbox-inner {
      border-color: #738ce6;
    }
    .audit-people {
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .no-setting {
      color: var(--title-tips-text-color);
      font-size: 12px;
    }
    .info-icon {
      display: inline-block;
      width: 16px;
      height: 16px;
      background: url('/@/assets/svg/icon/info_icon_tips.svg');

      &:hover {
        background: url('/@/assets/svg/icon/info_icon_tips_actived.svg');
      }
    }
    :deep .ant-tooltip-inner {
      color: var(--headerline-text-color);
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
      margin-left: 12px;
      background-color: transparent;
      font-family: Source Han Sans CN;
      color: var(--serious-text-color);
      font-size: 13px;
      &:hover {
        background-color: rgba(204, 102, 102, 0.2);
        border-radius: 4px;
      }
    }
  }
</style>
