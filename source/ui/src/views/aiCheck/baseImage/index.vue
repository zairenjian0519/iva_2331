<template>
  <BasicTable @register="registerTable">
    <template #img = "{ record }">
      <TableImage :simple-show="true" :img-list="[record.thumbnail]" :size="60"/>
    </template>
    <template #headerTop>
      <a-button type="primary" @click="addImage" ><plus-outlined/> 添加</a-button>
    </template>

    <template #action="{ record }">
      <TableAction :actions="getTableAction(record)" />
    </template>
  </BasicTable>

  <upload-image-modal @register="registerModal" @refresh-table="refreshTable"/>
</template>

<script setup lang="ts">
import {PlusOutlined} from "@ant-design/icons-vue";
import {ActionItem, BasicTable, TableAction, TableImg as TableImage} from "/@/components/Table";
import {useListPage} from "/@/hooks/system/useListPage";
import {columns} from "./BaseImage.data"
import UploadImageModal from "./UploadImageModal.vue";
import {useModal} from "/@/components/Modal";
import {deleteRecord, listPage} from "./BaseImage.api";

const { tableContext } = useListPage( {
  tableProps: {
    api: listPage,
    useSearchForm: false,
    striped: true,
    bordered: true,
    columns: columns,
    pagination: {
      pageSize: 30,
    },
    actionColumn: {
      width: 150,
    },
  }
})

const [registerTable, { reload }] = tableContext

function getTableAction(record): ActionItem[] {
  return [
    {
      label: '删除',
      icon: 'ant-design:delete-outlined',
      color: 'error',
      popConfirm: {
        title: '是否确认删除',
        confirm: handleDelete.bind(null, record),
      },
    },
  ];
}

const handleDelete = async (record) => {
  await deleteRecord({id: record.id})
  refreshTable()
}

const refreshTable = () => {
  reload ()
}

const addImage = () => {
  openModal()
}

const [registerModal, { openModal }] = useModal()

</script>

<style scoped lang="less">

</style>
