<template>
    <div class="operation">
      <secondary-button label="删除" class="delete-action" @click="handleBatchDelete">
        <i class="delete-icon"></i>
      </secondary-button>
      <secondary-button label="导出" class="export" @click="handleExport">
        <i class="export-icon"></i>
      </secondary-button>
<!--      <secondary-button label="导入到人脸库" class="import-action" @click="handleImport">
        <i class="import-icon"></i>
      </secondary-button>-->
      <Divider type="vertical"/>
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
</template>
<script lang="ts" setup>
import {defineEmits} from 'vue';
import {Divider} from 'ant-design-vue';
import BaseTooltip from '/@/components/BaseTooltip/BaseTooltip.vue';
import SecondaryButton from '/@/components/BaseButton/SecondaryButton.vue';

const props = defineProps<{
  isShowTable: Boolean;
}>();

const emits = defineEmits(['handleShowTable', 'handleExport', 'handleImport', 'handleBatchDelete']);

const handleShowTable = () => {
  emits('handleShowTable');
};

//多选删除
const handleBatchDelete = () => {
  emits('handleBatchDelete');
};

// 导出
const handleExport = async () => {
  emits('handleExport');
};

// 导入
const handleImport = () => {
  emits('handleImport');
};

</script>
<style lang="less" scoped>


.operation {
  display: flex;
  padding-top: 10px;
  justify-content: flex-end;

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
.import-action,
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
  background: url('/@/assets/svg/icon/editor_btn_export_default.svg');
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

.import-icon {
  display: inline-block;
  width: 14px;
  height: 14px;
  background: url('/@/assets/svg/icon/editor_btn_import_default.svg') no-repeat;
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

</style>
