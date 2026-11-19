<template>
  <div ref="containerRef">
    <a-form ref="formRef"
            :model="formState"
            layout="vertical"
            :rules="rules"
    >
      <a-row :gutter="24" style="margin-left: 15px; margin-right: 10px">
        <a-col :span="6">
          <a-form-item label="相机归属" has-feedback name="cameraOwnership">
            <JDictSelectTag placeholder="请选择" :show-choose-option="false" type="select"
                            v-model:value="formState.cameraOwnership"
                            dict-code="device_management,device_name,id"/>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="自定义分组" has-feedback name="location">
            <JDictSelectTag placeholder="请选择" :show-choose-option="false" type="select"
                            v-model:value="formState.location"
                            dict-code="ky_category,name,id,category='1'"/>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="录像机厂商" has-feedback name="factory">
            <ApiSelect placeholder="请选择"
                       v-model:value="formState.factory"
                       :api="getDictItemsByCode" :params="{ dictCode: 'factory_name'}"/>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="录像机IP地址" has-feedback name="ip">
            <a-input placeholder="硬盘录像机ip" v-model:value="formState.ip"/>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="24" style="margin-left: 15px; margin-right: 10px">
        <a-col :span="6">
          <a-form-item label="录像机端口号" has-feedback name="port">
            <a-input-number :min="1" :max="65535" placeholder="硬盘录像机端口号"
                            v-model:value="formState.port"
                            style="width: 100%"/>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="录像机账号" has-feedback name="username">
            <a-input placeholder="输入录像机账号" v-model:value="formState.username"/>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="录像机密码" has-feedback name="password">
            <a-input placeholder="输入录像机密码" v-model:value="formState.password"/>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="起始通道" has-feedback name="startChannelNum">
            <a-input-number placeholder="起始通道" :min="1" :max="254"
                            v-model:value="formState.startChannelNum"
                            style="width: 100%"/>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="24" style="margin-left: 15px; margin-right: 10px">
        <a-col :span="6">
          <a-form-item label="结束通道" has-feedback name="endChannelNum">
            <a-input-number placeholder="结束通道" :min="1" :max="254"
                            v-model:value="formState.endChannelNum"
                            style="width: 100%"/>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="码流类型" has-feedback name="streamType">
            <a-select placeholder="选择码流" v-model:value="formState.streamType"
                      style="width: 100%">
              <a-select-option value="sub">子码流</a-select-option>
              <a-select-option value="main">主码流</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="编码格式（可选）" has-feedback name="streamFormat">
            <a-select placeholder="选择编码格式" v-model:value="formState.streamFormat"
                      style="width: 100%">
              <a-select-option value="h264">H264</a-select-option>
              <a-select-option value="h265">H265</a-select-option>
              <a-select-option value="mpeg-4">MPEG-4</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="  ">
            <div style="float: right">
              <a-button type="default" @click="reset()">重 置</a-button>
              <a-button type="primary" style="margin-left: 10px" @click="addCameraInfo">确 定
              </a-button>
            </div>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <div style="margin-left: 15px; margin-right: 10px">
      <BasicTable @register="registerTable" style="min-width: unset">
        <!--操作栏-->
        <template #action="{ record }">
          <TableAction :actions="getTableAction(record)"/>
        </template>
      </BasicTable>
    </div>

    <a-modal v-model:visible="visible"
             title="修改相机信息"
             :width="600"
             :get-container="getContainer"
             @ok="updateCameraInfo">
      <a-form ref="nvrFormRef" layout="vertical" :model="nvrForm">
        <a-row style="margin: 10px 50px 0 50px">
          <a-col :span="24">
            <a-form-item label="相机名称" has-feedback name="cameraName">
              <a-input placeholder="相机名称" v-model:value="nvrForm.cameraName"/>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="相机编码" has-feedback name="cameraNumber">
              <a-input placeholder="相机编码" v-model:value="nvrForm.cameraNumber"/>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="视频地址" has-feedback name="streamAddress">
              <a-input placeholder="视频地址" v-model:value="nvrForm.streamAddress"/>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import {FormInstance, message} from "ant-design-vue";
import {reactive, ref} from "vue";
import JDictSelectTag from "/@/components/Form/src/jeecg/components/JDictSelectTag.vue";
import {useListPage} from '/@/hooks/system/useListPage';
import {ActionItem, BasicColumn, BasicTable, TableAction} from "/@/components/Table";
import {Rule} from "ant-design-vue/es/form";
import {upperCase} from "lodash-es";
import {getDictItemsByCode, saveOrUpdateBatch} from "/@/views/base/cameraManager/CameraManager.api";
import ApiSelect from "/@/components/Form/src/components/ApiSelect.vue";

const labelCol = {style: {width: '100px'}}

const containerRef = ref<HTMLElement | null>(null)
const getContainer = () => {
  return containerRef.value;
}

const formRef = ref<FormInstance>()
const formState = reactive({
  cameraOwnership: '',
  location: '',
  factory: undefined,
  ip: '',
  port: 554,
  username: '',
  password: '',
  startChannelNum: 1,
  endChannelNum: 8,
  streamType: 'sub',
  streamFormat: undefined
})

// modal
const visible = ref(false)
const nvrFormRef = ref<FormInstance>()
const nvrForm = reactive({
  tempId: '',
  cameraName: '',
  cameraNumber: '',
  streamAddress: ''
})

const updateCameraInfo = ()=> {
  const target = tableData.value.find((item: any) => item.tempId === nvrForm.tempId)
  if (target) {
    target.cameraName = nvrForm.cameraName
    target.cameraNumber = nvrForm.cameraNumber
    target.streamAddress = nvrForm.streamAddress
    target.streamAddressPush = nvrForm.streamAddress
  }
  nvrFormRef.value.resetFields()
  visible.value = false
}

let checkIp = async (_rule: Rule, value: string) => {
  if (!value) {
    return Promise.reject('请输入硬盘录像机ip');
  }
  const ipSplit = value.split(".")
  if (ipSplit.length != 4) {
    return Promise.reject('硬盘录像机ip不正确');
  }
  const ipv4Regex = /^((25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d|0)\.){3}(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d|0)$/;
  if (!ipv4Regex.test(value)) {
    return Promise.reject('硬盘录像机ip地址不正确')
  }
  return Promise.resolve();
}

let checkChannel = async (_rule: Rule, value: number) => {
  if (!value) {
    return Promise.reject('通道号不能为空');
  }
  if (formState.startChannelNum >= formState.endChannelNum) {
    return Promise.reject('起始通道大于或等于结束通道')
  }
  return Promise.resolve()
}

const rules: Record<string, Rule[]> = {
  cameraOwnership: [{required: true, message: '相机归属不能为空'}],
  location: [{required: true, message: '自定义分组不能为空'}],
  factory: [{required: true, message: '相机厂商不能为空'}],
  ip: [{required: true, validator: checkIp, trigger: "blur"}],
  port: [{required: true, message: '端口号不能为空'}],
  username: [{required: true, message: '录像机用户名不能为空'}],
  password: [{required: true, message: '录像机密码不能为空'}],
  startChannelNum: [{required: true, message: '通道数量不能为空'}],
  endChannelNum: [{required: true, validator: checkChannel, trigger: 'blur'}],
  streamType: [{required: true, message: '请选择码流类型'}],
}

const columns: BasicColumn[] = [
  {
    title: '相机名称',
    dataIndex: 'cameraName',
    key: 'cameraName',
  },
  {
    title: '相机编码',
    dataIndex: 'cameraNumber',
    key: 'cameraNumber',
  },
  {
    title: '视频地址',
    dataIndex: 'streamAddress',
    key: 'streamAddress'
  },
];

const tableData = ref([])

const {tableContext} = useListPage({
  designScope: 'basic-table-demo',
  tableProps: {
    dataSource: tableData,
    columns: columns,
    useSearchForm: false,
    showTableSetting: false,
    size: 'small'
  },
});

const [registerTable, {reload, setTableData}] = tableContext;

/**
 * 操作栏
 */
function getTableAction(record: any): ActionItem[] {
  return [
    {
      label: '删除',
      onClick: handleDel.bind(null, record),
    },
    {
      label: '编辑',
      onClick: handleEdit.bind(null, record),
    },
  ];
}

const handleDel = (record: any) => {
  tableData.value = tableData.value.filter(item => item.tempId != record.tempId)
}

const handleEdit = (record: any) => {
  visible.value = true
  nvrForm.tempId = record.tempId
  nvrForm.cameraName = record.cameraName
  nvrForm.cameraNumber = record.cameraNumber
  nvrForm.streamAddress = record.streamAddress
}

const reset = () => {
  formRef.value.resetFields()
  tableData.value = []
}

const addCameraInfo = () => {
  const valid = formRef.value.validate()
  if (!valid) return
  tableData.value = []
  for (let i = formState.startChannelNum; i <= formState.endChannelNum; i++) {
    const rtspAddress = getRtspTemplate(i)
    const temp = {
      tempId: i,
      cameraName: '通道' + i,
      cameraOwnership: formState.cameraOwnership,
      cameraStatus: 4,
      location: formState.location,
      username: formState.username,
      password: formState.password,
      factory: formState.factory,
      streamAddress: rtspAddress,
      cameraType: 'NVR',
      streamType: formState.streamType,
      streamFormat: formState.streamFormat,
      channel: i,
      ip: formState.ip,
      port: formState.port,
      isConfig: 1,
      streamAddressPush: rtspAddress,
      streamTypePush: formState.streamType,
      streamFormatPush: formState.streamFormat? formState.streamFormat: 'h264',
      cameraNumber: ''
    }
    tableData.value.push(temp)
  }
}

// 根据不同品牌获取nvr rtsp
const getRtspTemplate = (channelNum: number) => {
  const prefix = `rtsp://${formState.username}:${formState.password}@${formState.ip}:${formState.port}`
  let result: string
  switch (upperCase(formState.factory)) {
    case 'HK': // 海康
      const hkStreamType = formState.streamType === 'main' ? '01' : '02' // 01:主码流 02:子码流
      result = `${prefix}/Streaming/Channels/${channelNum}${hkStreamType}`
      break;
    case 'DH': // 大华
      const dhStreamType = formState.streamType === 'main' ? '0' : '1'; // 0:主码流 1:子码流
      result = `${prefix}/cam/realmonitor?channel=${channelNum}&subtype=${dhStreamType}`
      break
    case "YS": // 宇视
      const ysStreamType = formState.streamType === 'main' ? '0' : '1'; // 0:主码流 1:子码流
      result = `${prefix}/unicast/c${channelNum}/s${ysStreamType}`;
      break
    case 'TDWY': // 天地伟业
      const tdwyStreamType = formState.streamType === 'main' ? '1' : '2'; // 1:主码流 2:子码流
      result = `${prefix}/${channelNum}/${tdwyStreamType}`;
      break
    default:
      result = prefix
      break;
  }
  return result;
}

const saveData = async () => {
  if (!tableData.value || tableData.value.length == 0) {
    message.error("请生成数据后，在执行该操作")
    return
  }
  await saveOrUpdateBatch(tableData.value)
  reset()
}

defineExpose({
  saveData
})
</script>

<style lang="less" scoped>
:deep(.ant-modal-content) {
  //弹窗 X位置
  .ant-modal-close {
    margin-right: 12px;
  }
}
</style>
