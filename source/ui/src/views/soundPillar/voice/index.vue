<template>
  <div style="margin: 15px">
    <a-card title="音频配置" :bordered="false" style="width: 100%">
      <BasicTable @register="registerTable">
        <template #tableTitle>
          <a-button type="primary" @click="syncAudioFiles" :loading="syncAudioBtnLoading">
            同步音频文件
          </a-button>
        </template>
        <!--操作栏-->
        <template #action="{ record }">
          <TableAction :actions="getTableAction(record)"/>
        </template>
        <template #enable="{record}">
          <a-tag color="success" v-if="record.status"
                 @click="changeDeviceStatus(0, record.audioId, record.algorithm)"
                 style="cursor: pointer">启用
          </a-tag>
          <a-tag color="default" v-else
                 @click="changeDeviceStatus(1, record.audioId, record.algorithm)"
                 style="cursor: pointer">禁用
          </a-tag>
        </template>
        <template #algorithm="{record}">
          <span v-if="isNaN(parseInt(record.algorithm))">未设置</span>
          <span v-else>{{ formatAlgorithm(record.algorithm) }}</span>
        </template>
      </BasicTable>
    </a-card>
    <basic-modal
      v-bind="$attrs"
      @register="register"
      title="配置音频算法"
      @ok="setAlgorithm">
      <a-form
        :model="soundVoiceInfo"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 15 }"
        autocomplete="off">
        <a-form-item label="文件名称">
          <strong>{{ soundVoiceInfo.displayName }}</strong>
        </a-form-item>
        <a-form-item label="文件id">
          <strong>{{ soundVoiceInfo.audioId }}</strong>
        </a-form-item>
        <a-form-item label="文件大小">
          <strong>{{ soundVoiceInfo.fileSize }}</strong>
        </a-form-item>
        <a-form-item label="时长">
          <strong>{{ soundVoiceInfo.duration }}</strong>
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="soundVoiceInfo.status" name="status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="选择算法" name="algorithm">
          <a-select v-model:value="soundVoiceInfo.algorithm">
            <a-select-option v-for="(item, index) in algorithmArr" :key="index"
                             :value="item.code">{{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </basic-modal>
    <basic-modal
      v-bind="$attrs"
      @register="register1"
      title="测试音频"
      :showOkBtn="false"
      @cancel="closeModal1">
      <a-form
        :model="soundVoiceInfo"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 15 }"
        autocomplete="off">
        <a-form-item label="文件名称">
          <strong>{{ soundVoiceInfo.displayName }}</strong>
        </a-form-item>
        <a-form-item label="文件id">
          <strong>{{ soundVoiceInfo.audioId }}</strong>
        </a-form-item>
        <a-form-item label="文件大小">
          <strong>{{ soundVoiceInfo.fileSize }}</strong>
        </a-form-item>
        <a-form-item label="时长">
          <strong>{{ soundVoiceInfo.duration }}</strong>
        </a-form-item>
        <div style="width: 100%;">
          <audio  style="margin: 0 auto; width: 80%"
                  ref="audioElement"
                  controls
                  controlslist="noplaybackrate nodownload"></audio>
        </div>
      </a-form>
    </basic-modal>
  </div>
</template>

<script setup>
import {BasicTable, TableAction} from '/@/components/Table';
import {BasicModal, useModalInner} from '/@/components/Modal';
import {useListPage} from '/@/hooks/system/useListPage';
import {
  getAlgorithmList,
  getAudioFiles,
  listPage,
  updateVoiceAlgorithm
} from "/@/views/soundPillar/voice/AudioFile.api";
import {message} from "ant-design-vue";
import {onBeforeMount, reactive, ref} from "vue";
import {parseInt} from "lodash-es";
import {useMessage} from "/@/hooks/web/useMessage";

// ==== 表格 ====
const columns = [
  {
    title: 'id',
    align: 'center',
    dataIndex: 'id',
    defaultHidden: true
  },
  {
    title: '文件名称',
    align: 'center',
    dataIndex: 'displayName'
  },
  {
    title: '文件id',
    align: 'center',
    dataIndex: 'audioId'
  },
  {
    title: '文件大小',
    align: 'center',
    dataIndex: 'fileSize'
  },
  {
    title: '时长',
    align: 'center',
    dataIndex: 'duration'
  },
  {
    title: '上传时间',
    align: 'center',
    dataIndex: 'uploadTime'
  },
  {
    title: '状态',
    align: 'center',
    dataIndex: 'status',
    slots: {customRender: 'enable'}
  },
  {
    title: '算法',
    align: 'center',
    dataIndex: 'algorithm',
    slots: {customRender: 'algorithm'}
  }
];

const {tableContext} = useListPage({
  tableProps: {
    api: listPage,
    columns: columns,
    striped: true,
    useSearchForm: false,
    actionColumn: {
      width: 120,
    },
  },
});

// BasicTable绑定注册
const [registerTable, {reload}] = tableContext;

// 表格操作栏
function getTableAction(record) {
  return [
    {
      label: '编辑',
      onClick: handleEdit.bind(null, record),
    },
    {
      label: '测试',
      onClick: handleTest.bind(null, record),
    },
  ];
}

//自定义表单字段
const soundVoiceInfo = reactive({})
const [register, {closeModal, setModalProps}] = useModalInner();

// action 绑定的方法
function handleEdit(record) {
  Object.assign(soundVoiceInfo, record)
  setModalProps({visible: true})
}

// action 调试
const [register1, {closeModal: closeModal1, setModalProps: setModalProps1}] = useModalInner();
const audioElement = ref()
function handleTest(record){
  Object.assign(soundVoiceInfo, record)
  setModalProps1({visible: true})
  setTimeout(()=>{
    audioElement.value.src = soundVoiceInfo.accessUrl
    audioElement.value.pause()
  }, 100)

}

// 算法编辑列提交的方法
async function setAlgorithm() {
  const params = {
    audioId: soundVoiceInfo.audioId,
    algorithm: soundVoiceInfo.algorithm,
    status: soundVoiceInfo.status
  }
  await updateVoiceAlgorithm(params)
  message.success("提交成功")
  reload()
  closeModal()
}

// 格式化算法
function formatAlgorithm(algorithm) {
  const target = algorithmArr.value.find(item => item.code === algorithm)
  if (target) return target.name
  return algorithm
}

// 修改音频文件状态
const {createConfirm} = useMessage()

async function changeDeviceStatus(status, audioId, algorithm) {
  const params = {
    audioId: audioId,
    status: status,
    algorithm: algorithm
  }
  const contentStr = status === 0 ? '确定禁用该音频？' : '确定启用该音频？'
  createConfirm({
    iconType: 'warning',
    title: '提示信息',
    content: contentStr,
    onOk: async () => {
      await updateVoiceAlgorithm(params)
      message.success("操作成功")
      reload()
    },
    onCancel: () => console.log('取消'),
  });
}

const syncAudioBtnLoading = ref(false)

async function syncAudioFiles() {
  syncAudioBtnLoading.value = true
  await getAudioFiles()
  reload()
  message.success("同步音频文件成功")
  syncAudioBtnLoading.value = false
}

// 获取算法列表
let algorithmArr = ref([])

async function getAlgorithmArr() {
  algorithmArr.value.length = 0
  const list = await getAlgorithmList()
  if (list) algorithmArr.value = list
}

onBeforeMount(() => {
  getAlgorithmArr()
  // const test = listPage().then(res => console.log(res))
})

</script>

<style scoped lang="less">
.hiddenCol {
  display: none;
}
</style>
