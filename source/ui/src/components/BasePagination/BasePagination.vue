<template>
  <div class="base-pagination">
    <a-pagination
      :total="total"
      show-size-changer
      :default-page-size="defaultPageSize"
      :show-total="(total) => `共${total} 条`"
      @change="handleChange"
      @showSizeChange="handleShowSizeChange"
      :page-size="pageSize"
      :page-size-options="pageSizeOptions"
      :current="current"
    />
  </div>
</template>
<script lang="ts" setup>
  import { Pagination } from 'ant-design-vue';
  const APagination = Pagination;
  defineProps<{
    pageSizeOptions?: string[];
    total?: number;
    pageSize: number;
    defaultPageSize: number;
    current: number;
  }>();
  // const props = withDefaults(defineProps<Props>(), {
  //   pageSizeOptions: ,
  //   total: 0,
  // });
  const emit = defineEmits(['handleChange', 'handleShowSizeChange']);
  //   页码变化的回调
  const handleChange = (page, pageSize) => {
    const changeObj = { page: page, pageSize: pageSize };
    emit('handleChange', changeObj);
  };
  //   页大小变化回调
  const handleShowSizeChange = (current, size) => {
    const sizeObj = { current: current, size: size };
    emit('handleShowSizeChange', sizeObj);
  };
</script>
<style lang="less" scoped>
  // .base-pagination {
  //   font-family: Noto Sans Kannada;
  //   color: #2b2d33;
  //   font-size: 13px;
  //   position: absolute;
  //   bottom: 0px;
  //   width: calc(100% - 60px);
  //   background-color: #fff;
  //   z-index: 333;
  //   text-align: right;
  //   height: 40px;
  // }
  :deep .ant-pagination-item-active {
    border: none;
  }
  :deep .ant-pagination-item {
    width: 36px;
    height: 28px;
    border-radius: 4px;
    line-height: 28px;
    min-width: 28px;
    &:hover {
      border-color: #738ce6;
    }
  }
  :deep .ant-pagination-item a {
    padding: 0;
  }
  :deep .ant-pagination-item-active a {
    width: 36px;
    height: 28px;
    background-color: #738ce6;
    border-color: #738ce6;
    border-radius: 4px;
    color: var(--background-Color);
  }
  :deep .ant-pagination-prev .ant-pagination-item-link,
  :deep .ant-pagination-next .ant-pagination-item-link {
    width: 28px;
    height: 28px;
    padding: 0;
    font-size: 12px;
    min-width: 28px;
  }
  :deep .ant-pagination-prev,
  :deep .ant-pagination-next,
  :deep .ant-pagination-jump-prev,
  :deep .ant-pagination-jump-next,
  :deep .ant-pagination-jump-prev {
    display: inline-block;
    min-width: 28px;
    height: 28px;
  }
  :deep .ant-pagination-prev:hover .ant-pagination-item-link,
  :deep .ant-pagination-next:hover .ant-pagination-item-link {
    &:hover {
      color: #738ce6;
      border-color: #738ce6;
    }
  }

  :deep .ant-pagination-options {
    height: 28px;
  }
  :deep .ant-pagination-options-quick-jumper {
    height: 28px;
    line-height: 28px;
  }

  :deep .ant-select-focused:not(.ant-select-disabled).ant-select:not(.ant-select-customize-input) .ant-select-selector {
    border: 1px solid;
    border-color: #738ce6;
    box-shadow: none;
    border-right-width: 1px !important;
  }
  :deep .ant-select:not(.ant-select-disabled):hover .ant-select-selector {
    border: 1px solid;
    border-color: #adb4cc;
    border-radius: 4px;
  }
  :deep .ant-select-item-option-selected:not(.ant-select-item-option-disabled) {
    background-color: rgba(115, 140, 230, 0.2);
    width: 86px;
    height: 28px;
    height: 28px;
    justify-content: center;
    border-radius: 4px;
  }

  :deep .rc-virtual-list-holder-inner {
    align-items: center;
  }
  :deep .ant-select-item-option-active:not(.ant-select-item-option-disabled) {
    background-color: #f5f5f5;
    width: 86px;
    height: 28px;
    background-color: var(--netural-color);
    border-radius: 4px;
  }
</style>
