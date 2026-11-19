<template>
  <div style="padding: 10px">
    <div style="width: 100%; background: #fff; padding: 10px">
      <div class="rowClass">
        上传模型文件：
        <BasicUpload :max-size="20"
                     :max-number="1"
                     @change="handleFileChange"
                     :accept="['zip','tar', 'tar.gz']"
                     :api="uploadModelFile"/>
      </div>
      <div class="rowClass">
        上传主文件：
        <BasicUpload :max-size="20"
                     :max-number="1"
                     @change="handleFileChange"
                     :accept="['zip','tar', 'tar.gz']"
                     :uploadParams="{ type: 'main' }"
                     :api="uploadMainFile"/>
      </div>
      <div class="rowClass">
        <a-button type="primary" @click="debugStart"><BugOutlined/>调试</a-button>
      </div>
      <div class="operateBook">
        <a-button type="error" @click="downloadBook"><BookOutlined/>操作说明</a-button>
      </div>
      <div style="margin-top: 10px">
        <BasicTable @register="registerTableFile">
          <template #filePath="{ record }">
            <a-tooltip :title="record.filePath" placement="top">
              <div style="width: 200px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{{record.filePath}} </div>
            </a-tooltip>
          </template>

          <template #workPath="{ record }">
            <a-tooltip :title="record.workPath" placement="top">
              <div style="width: 200px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{{record.workPath}} </div>
            </a-tooltip>
          </template>

          <template #status="{ record }">
            <a-tag :color="record.status?'green' : 'red'">{{ record.status? "启用": "未启用" }}</a-tag>
          </template>
          <template #fileType="{ record }">
           {{ record.fileType == "model"? "模型文件": "主文件" }}
          </template>
          <template #action="{ record }">
            <TableAction :actions="getFileTableAction(record)"/>
          </template>
        </BasicTable>
      </div>
    </div>

<!--    <div style="display: inline-flex">
      <BasicTable @register="registerTableClass">
        <template #headerTop>
          <div style="width: 100%; padding-bottom: 10px">
            <a-button type="primary" @click="registerClass">类名登记</a-button>
          </div>
        </template>
        <template #action="{ record }">
          <TableAction :actions="getClassTableAction(record)"/>
        </template>
        <template #status="{ record }">
          <a-tag :color="record.status?'green' : 'red'">{{ record.status? "启用": "禁用" }}</a-tag>
        </template>
      </BasicTable>

      <BasicTable @register="registerMethodTable" style="margin-left: 5px">
        <template #headerTop>
          <div style="width: 100%; padding-bottom: 10px">
            <a-button type="primary" @click="registerMethod">登记类中的算法</a-button>
          </div>
        </template>
        <template #action="{ record }">
          <TableAction :actions="getMethodTableAction(record)"/>
        </template>
        <template #status="{ record }">
          <a-tag :color="record.status?'green' : 'red'">{{ record.status? "启用": "禁用" }}</a-tag>
        </template>
      </BasicTable>
    </div>-->
  </div>
  <DebugDrawer @register="registerDrawer"></DebugDrawer>
<!--  <ModalClass @register="registerModalClass" @refresh-table="refreshClassTable"></ModalClass>
  <ModalMethod @register="registerModalMethod" @refresh-table="reloadMethodTable"></ModalMethod>-->
</template>

<script setup lang="ts">
import {BugOutlined, BookOutlined} from "@ant-design/icons-vue";
import {BasicUpload} from "/@/components/Upload";
import {BasicTable, TableAction} from "/@/components/Table";
import {useListPage} from "/@/hooks/system/useListPage";
import {columnsClass, columnsFile, columnsMethod} from "/@/views/adapter/adapter.data";
import {useDrawer} from "/@/components/Drawer";
import DebugDrawer from "/@/views/adapter/components/DebugDrawer.vue";
import ModalClass from "./components/ModalClass.vue";
import ModalMethod from "./components/ModalMethod.vue";
import {
  applyFileById,
  deleteClass,
  deleteFileById,
  deleteMethod,
  getClassNamePage,
  getMethodPage,
  getPage,
  uploadMainFile,
  uploadModelFile
} from "/@/views/adapter/adapter.api";
import {useModal} from "/@/components/Modal";

const {tableContext: tableContextFile} = useListPage({
  designScope: 'model-file-table',
  tableProps: {
    title: '文件列表',
    bordered: true,
    api: getPage,
    columns: columnsFile,
    striped: true,
    useSearchForm: false,
    tableSetting: {
      fullScreen: false,
      setting: true,
    },
    actionColumn: {
      width: 150,
    },
  },
})

const [registerTableFile, {reload: reloadFileTable, setProps: setFileTableProps}] = tableContextFile;

const getFileTableAction = (record: Recordable) => {
  return [
    {
      label: '应用',
      popConfirm: {
        title: '确定应用吗？',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        confirm: async () => {
          await applyFile(record)
        },
      },
    },
    {
      label: '删除',
      popConfirm: {
        title: '确定删除吗？',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        confirm:  async () => {
          await deleteFile(record)
        },
      },
    },
  ];
};

const handleFileChange = () => {
  reloadFileTable();
};

const deleteFile = async (record: any) => {
  setFileTableProps({
    loading: true,
  })
  try {
    await deleteFileById({id: record.id})
    await reloadFileTable()
  }finally {
    setFileTableProps({
      loading: false,
    })
  }
}

const applyFile= async (record: any) => {
  setFileTableProps({
    loading: true,
  })
  try {
    await applyFileById({id: record.id})
    await reloadFileTable()
  }finally {
    setFileTableProps({
      loading: false,
    })
  }
}

const {tableContext} = useListPage({
  designScope: 'class-category-table',
  tableProps: {
    title: '主文件新增的类名',
    bordered: true,
    api: getClassNamePage,
    columns: columnsClass,
    striped: true,
    useSearchForm: false,
    tableSetting: {
      fullScreen: false,
      setting: true,
    },
    actionColumn: {
      width: 120,
    },
  },
})
const [registerTableClass, {reload: reloadClassTable }] = tableContext;

const getClassTableAction = (record: Recordable) => {
  return [
    {
      label: '删除',
      popConfirm: {
        title: '删除class会删除绑定的算法，确定删除？',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        confirm: async () => {
          await deleteClass(record)
          await reloadClassTable()
          await reloadMethodTable()
        },
      },
    },{
      label: '修改',
      onClick: () => {
        openModalClass(true, {
          isUpdate: true,
          model: record,
        })
      },
    },
  ];
};

const refreshClassTable = async () => {
  await reloadClassTable()
  await reloadMethodTable()
};

const  [registerModalClass, { openModal: openModalClass }] =useModal()
const registerClass = () => {
  openModalClass(true, {
    isUpdate: false,
    model: {},
  })
}

const {tableContext: tableContextMethod} = useListPage({
  designScope: 'algorithm-ids-table',
  tableProps: {
    title: '每个类中具备的算法ID',
    bordered: true,
    api: getMethodPage,
    columns: columnsMethod,
    striped: true,
    useSearchForm: false,
    tableSetting: {
      fullScreen: false,
      setting: true,
    },
    actionColumn: {
      width: 120,
    },
  },
})

const [registerMethodTable, {reload: reloadMethodTable}] = tableContextMethod;

const [registerModalMethod, { openModal: openModalMethod }]  = useModal()

const getMethodTableAction = (record: Recordable) => {
  return [
    {
      label: '删除',
      popConfirm: {
        title: '确定删除吗？',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        confirm: async () => {
          await deleteMethod({ id: record.id })
          await reloadMethodTable()
        }
      }
    }, {
      label: '修改',
      onClick: () => {
        openModalMethod(true, {
          isUpdate: true,
          model: record,
        })
      },
    },
  ]
}

const registerMethod = () => {
  openModalMethod(true, {
    isUpdate: false,
    model: {},
  })
}

const [registerDrawer, { openDrawer }] = useDrawer();

function debugStart() {
  openDrawer(true, {});
}

const getCurrentHostname = () => {
  const hostname = window.location.hostname;
  const port = window.location.port;
  const protocol = window.location.protocol;

  // 拼接IP地址和端口
  return protocol + '//' + hostname + (port ? ':' + port : '');
}

const downloadBook = ()=> {
  const currentHost = getCurrentHostname();
  const fileUrl = currentHost + '/算法移植说明.pdf';
  window.open(fileUrl)
}

</script>

<style scoped lang="less">
.rowClass {
  margin-right: 25px;
  padding-left: 5px;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
}

.operateBook{
  display: inline-flex;
  justify-content: flex-start;
  align-items: center;
  float: right;
}
</style>
