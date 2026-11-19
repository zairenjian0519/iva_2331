<template>
  <BasicModal v-bind="$attrs"
              @register="registerModal"
              destroyOnClose title="版本升级"
              :width="800">
    <a-row style="margin-top: 20px">
      <a-col :offset="1">
        <label>升级选项：</label>
        <a-radio-group v-model:value="mainState.softType" @change="handleSelect">
          <a-radio value="web_front">web前端</a-radio>
          <a-radio value="web_backend">web后台</a-radio>
          <a-radio value="algorithm_pkg">算法升级</a-radio>
        </a-radio-group>
      </a-col>
    </a-row>
    <a-row style="margin-top: 20px">
      <a-col :offset="1" v-if="mainState.softType == 'algorithm_pkg'">
        <div style="margin-bottom: 15px">
          选择算法：<a-select style="width: 300px;" v-model:value="mainState.selectAlg" @change="handleSelectAlg">
            <a-select-option v-for="item in algList" :key="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </div>
      </a-col>
      <a-col :offset="1">
        <label>选择文件：</label>
        <a-upload :file-list="fileList"
                  :beforeUpload="beforeUpload"
                  :show-upload-list="false"
                  @remove="handleRemove">
          <a-input style="width: 300px" v-model:value="selectFileName" :readonly="true">
            <template #suffix>
              <img :src="infoIconWarning" alt=""/>
            </template>
          </a-input>
        </a-upload>
        <a-button type="danger" class="ml-2" @click="handleUpdate">上传</a-button>
        <a-button class="ml-2" @click="resetUpdate">撤回升级</a-button>
        <a-button class="ml-2" @click="resetInput">重置输入</a-button>
      </a-col>
    </a-row>
    <BasicTable @register="registerTable">
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)"/>
      </template>
      <template #status="{ record }">
        <a-tag color="success" v-if="record.status == '1'">已发布</a-tag>
        <a-tag color="default" v-if="record.status == '0'">未发布</a-tag>
      </template>
      <template #algorithmName="{ record }">
        {{ formatAlgorithmName(record.algorithmId) }}
      </template>
    </BasicTable>
  </BasicModal>
</template>

<script lang="ts" setup>
import BasicModal from '/@/components/Modal/src/BasicModal.vue';
import {useModalInner} from '/@/components/Modal';
import {Col, RadioChangeEvent, Row, UploadProps} from 'ant-design-vue';
import {computed, reactive, ref} from 'vue';

import infoIconWarning from '/@/assets/svg/icon/info_icon_warning.svg';
import {ActionItem, BasicColumn, BasicTable, TableAction} from "/@/components/Table";
import {useListPage} from '/@/hooks/system/useListPage';
import {useMessage} from "/@/hooks/web/useMessage";
import {
  deleteSoftFile,
  getPageData,
  publishSoftFile,
  uploadUpdateFile,
  withdraw
} from "/@/views/base/deviceManagement/UpdateSoft.api";
import type {UploadFileCallBack, UploadFileParams} from "/#/axios";
import {initDictOptions} from "/@/utils/dict";

const ACol = Col;
const ARow = Row;
const selectFileName = ref<string>('');
const devItem = ref();

const {createMessage, createConfirm} = useMessage()

const mainState = reactive({
  softType: 'web_front',
  selectAlg: ''
});

// modal
const [registerModal, {setModalProps}] = useModalInner(async (data) => {
  selectFileName.value = '';
  devItem.value = data.item;
  setModalProps({confirmLoading: false, showCancelBtn: false, showOkBtn: false, footer: null});
})

// Table
const columns: BasicColumn[] = [
  {
    title: '版本号',
    align: 'center',
    dataIndex: 'softVersion'
  }, {
    title: '上传时间',
    align: 'center',
    dataIndex: 'createTime',
  }, {
    title: '状态',
    align: 'center',
    dataIndex: 'status',
    slots: {customRender: 'status'},
  }
]

const {tableContext} = useListPage({
  designScope: 'up-soft-table',
  tableProps: {
    api: getPageData,
    searchInfo: {
      softType: mainState.softType,
      pageSize: 10,
      pageNo: 1,
      algorithmId: mainState.selectAlg
    },
    columns: columns,
    striped: true,
    useSearchForm: false,
    tableSetting: {
      fullScreen: false,
      size: false,
    },
    actionColumn: {
      width: 120,
    },
  },
});
const [registerTable, {reload, setColumns, setProps}] = tableContext;

function getTableAction(record: any): ActionItem[] {
  return [
    {
      label: '发布',
      onClick: handleRelease.bind(null, record),
    },
    {
      label: '删除',
      onClick: handleDelete.bind(null, record)
    }
  ];
}

const handleRelease = (record: any) => {
  let contentStr = `确定使用当前版本：${record.softVersion}？`
  if (record.softType == 'web_backend') {
    contentStr = `确定使用当前版本：${record.softVersion}？发布后会重启服务，请等待3分钟左右刷新网页。`
  }
  createConfirm({
    iconType: 'warning',
    title: '发布版本',
    content: contentStr,
    onOk: () => {
      setModalProps({
        loading: true,
        loadingTip: `${record.softVersion} 正在发布中...`,
        afterClose: () => {
          resetUpdateLoading()
        }
      })
      publishSoftFile({id: record.id})
        .then(() => {
          refreshTable()
          // 如果是前端更新，从新加载当前页面
          if (record.softType == 'web_front') {
            reloadPage()
          }
        }).catch(err => {
        console.log('发布失败：', err)
      }).finally(() => {
        resetUpdateLoading()
      })
    },
  })
};

const reloadPage = () => {
  setTimeout(() => {
    createConfirm({
      iconType: 'info',
      title: '提示',
      content: `发布成功，是否立即刷新网页？`,
      onOk: () => {
        location.reload()
      }
    })
  }, 500)
}

const handleDelete = (record: any) => {
  console.log(record);
  createConfirm({
    iconType: 'error',
    title: '删除版本',
    content: `确定删除版本：${record.softVersion}？`,
    onOk: () => {
      setModalProps({
        loading: true,
        loadingTip: '正在删除中...',
        afterClose: () => {
          resetUpdateLoading()
        }
      })
      deleteSoftFile({
        id: record.id
      }).then(() => {
        refreshTable()
      }).catch(err => {
        console.log('删除失败：', err)
      }).finally(() => {
        resetUpdateLoading()
      })
    },
  })
}

const refreshTable = async () => {
  const params = {
    searchInfo: {
      softType: mainState.softType,
      page: 1,
      algorithmId: ''
    }
  }
  if (mainState.softType == 'algorithm_pkg') {
    params.searchInfo.algorithmId = mainState.selectAlg
  } else {
    params.searchInfo.algorithmId = ''
  }
  setProps({
    searchInfo: params.searchInfo
  })
  await reload()
}

const handleSelect = (e: RadioChangeEvent) => {
  mainState.softType = e.target.value
  const newColumns = columns.filter(item => item.dataIndex != 'algorithmName')
  if (mainState.softType == 'algorithm_pkg') {
    // 放在数组首位
    newColumns.splice(0, 0, {
      title: '算法名称',
      align: 'center',
      dataIndex: 'algorithmName',
      slots: {customRender: 'algorithmName'},
    })
    getAlg()
  }
  setColumns(newColumns)
  refreshTable()
}
// 文件上传部分
const fileList = ref<UploadProps['fileList']>([])

const beforeUpload: UploadProps['beforeUpload'] = file => {
  console.log('beforeUpload', file)
  selectFileName.value = file.name
  fileList.value = [file]
  return false
}

const handleRemove: UploadProps['onRemove'] = file => {
  const index = fileList.value.indexOf(file);
  const newFileList = fileList.value.slice();
  newFileList.splice(index, 1);
  fileList.value = newFileList;
}

const handleUpdate = () => {
  createConfirm({
    iconType: 'info',
    title: '提示',
    content: '是否继续？点击确定开始上传文件',
    onOk: () => {
      handleSubmit()
    }
  })
}

const resetUpdateLoading = () => {
  setModalProps({
    loading: false,
    loadingTip: ''
  })
}

function getRawFile(fileItem: any): File | null {
  if (fileItem instanceof File) return fileItem;
  if (fileItem?.originFileObj instanceof File) return fileItem.originFileObj;
  if (fileItem?.file instanceof File) return fileItem.file;
  return null;
}

const updateCallbackSuccess = (data: any) => {
  console.log('upload result: ', data)
  if (data.success) {
    createMessage.success(data.message)
  } else {
    createMessage.error(data.message)
  }
  refreshTable()
}

const updateCallbackProgress = (percentCompleted: number, progressEvent: ProgressEvent) => {
  console.log('upload progress: ', percentCompleted, progressEvent)
  setModalProps({
    loadingTip: `文件正在努力上传中...${percentCompleted}%`,
  })
}

const resetUpdate = async () => {
  setModalProps({
    loading: true,
    loadingTip: '正在撤回中，请稍后...',
    afterClose: () => {
      resetUpdateLoading()
    }
  })
  await withdraw({
    softType: mainState.softType
  })
  await refreshTable()
  resetUpdateLoading()
}

const resetInput = () => {
  fileList.value = []
  selectFileName.value = ''
}

async function handleSubmit() {
  if (!mainState.softType) {
    createMessage.error('请选择升级选项')
    return
  }

  const file = getRawFile(fileList.value[0]);
  if (!file) {
    createMessage.error('请选择有效的文件');
    return;
  }

  const params: UploadFileParams = {
    data: {
      softType: mainState.softType,
      algorithmId: mainState.selectAlg
    },
    // 从fileList中获取file
    file: file,
    filename: selectFileName.value,
  }
  const callback: UploadFileCallBack = {
    success: updateCallbackSuccess,
    onProgress: updateCallbackProgress,
  }

  console.log('upload params: ', params)
  try {
    setModalProps({
      loading: true,
      loadingTip: '正在上传中，请稍后...',
      afterClose: () => {
        resetUpdateLoading()
      }
    })
    await uploadUpdateFile(params, callback)
  } finally {
    resetUpdateLoading()
  }
}

const algList = ref([])
const getAlg = async () => {
  algList.value = await initDictOptions('ky_category,name,id,category="0" and pid!="0"')
  // const targetName = '未穿着反光衣'
  // 从algList中查找targetName
  // const target = algList.value.find(item => item.label == targetName)
  // if (target) {
  //   mainState.selectAlg = target.value
  // }
}

// 通过带参数的计算属性查询算法名称
const formatAlgorithmName = computed(() => {
  return (algId: string) => {
    const target = algList.value.find(item => item.value == algId)
    return target ? target.label : '未知算法'
  }
})

const handleSelectAlg = (value: any) => {
  mainState.selectAlg = value
  refreshTable()
}

</script>

<style scoped></style>
