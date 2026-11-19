<template>
  <div class="sound-container">
    <a-card title="IO配置" :bordered="false" style="width: 100%">
      <a-form
        ref="formRef"
        :model="formState"
        :label-col="{span: 6}"
        :wrapper-col="{span: 18}"
        autocomplete="off">
        <a-row :gutter="24">
          <a-col :span="6">
            <a-form-item label="端点命名" name="pinName" :rules="rulesState.pinName">
              <a-input placeholder="请给端点命名" v-model:value="formState.pinName"/>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="端点编号" name="pinNumber" :rules="rulesState.pinNumber">
              <a-input-number :min="0" placeholder="请输入端点编号"
                              v-model:value="formState.pinNumber" style="width: 100%"/>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="电平类型" name="pinType" :rules="rulesState.pinType">
              <a-select placeholder="请选择电平类型"
                        v-model:value="formState.pinType" style="width: 100%">
                <a-select-option value="High">高电平</a-select-option>
                <a-select-option value="Low">低电平</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="表示值" name="pinVal" :rules="rulesState.pinVal">
              <a-input placeholder="请输入表示值" v-model:value="formState.pinVal"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="6">
            <a-form-item label="状态" name="status" :rules="rulesState.status">
              <a-select placeholder="请选择状态" v-model:value="formState.status">
                <a-select-option :value="1">启用</a-select-option>
                <a-select-option :value="0">禁用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-button type="primary" @click="submitData" :loading="addBtnLoading">提 交</a-button>
          </a-col>
        </a-row>
      </a-form>
      <BasicTable @register="registerTable">
        <template #action="{ record }">
          <TableAction :actions="getTableAction(record)"/>
        </template>
        <template #pinType="{record}">
          <a-tag :color="formatPinType['color_' + record['pinType']]">
            {{ formatPinType['name_' + record['pinType']] }}
          </a-tag>
        </template>
        <template #status="{record}">
          <a-tag :color="formatStatus['color_' + record['status']]">
            {{ formatStatus['name_' + record['status']] }}
          </a-tag>
        </template>
      </BasicTable>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref} from "vue";
import {ActionItem, BasicColumn, BasicTable, TableAction} from "/@/components/Table";
import {useListPage} from '/@/hooks/system/useListPage';
import {message} from "ant-design-vue";
import {addPinSetting, deletePinSetting, editPinSetting, listPage} from "/@/views/dido/pin/index";
import {useMessage} from "/@/hooks/web/useMessage";

const {createConfirm} = useMessage()

const formRef = ref()
const addBtnLoading = ref(false)

const formState = reactive({
  id: '',
  pinName: '',
  pinNumber: 0,
  pinType: 'High',
  pinVal: '1',
  status: 1
})

const rulesState = {
  pinName: [{required: true, message: '请给端点命名'}],
  pinNumber: [{required: true, message: '请输入端点编号'}],
  pinType: [{required: true, message: '请选择电平类型'}],
  pinVal: [{required: true, message: '请输入电平表示值'}],
  status: [{required: true, message: '请选择状态'}],
}

const formatPinType = {
  color_High: 'red',
  name_High: '高电平',
  color_Low: 'blue',
  name_Low: '低电平'
}

const formatStatus = {
  color_1: 'green',
  name_1: '启用',
  color_0: '',
  name_0: '禁用'
}

//表格
const columns: BasicColumn[] = [
  {
    title: '端点名称',
    align: 'center',
    dataIndex: 'pinName'
  }, {
    title: '端点编号',
    align: 'center',
    dataIndex: 'pinNumber',
  }, {
    title: '电平类型',
    align: 'center',
    dataIndex: 'pinType',
    slots: {customRender: 'pinType'},
  }, {
    title: '表示值',
    align: 'center',
    dataIndex: 'pinVal',
  }, {
    title: '状态',
    align: 'center',
    dataIndex: 'status',
    slots: {customRender: 'status'},
  }
]

// 列表页面公共参数、方法
const {tableContext} = useListPage({
  designScope: 'dido-pin-table',
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

//BasicTable绑定注册
const [registerTable, {reload}] = tableContext;

// 操作栏
function getTableAction(record): ActionItem[] {
  return [
    {
      label: '编辑',
      onClick: handleEdit.bind(null, record),
    },
    {
      label: '删除',
      onClick: handleDelete.bind(null, record)
    }
  ];
}

function handleEdit(record: any) {
  const {pinType, id, pinNumber, pinName, status, pinVal} = record
  formState.pinType = pinType
  formState.id = id
  formState.pinNumber = pinNumber
  formState.pinName = pinName
  formState.status = status
  formState.pinVal = pinVal
  message.info('配置信息已填充到上面配置表单')
}

async function handleDelete(record: any) {
  createConfirm({
    iconType: 'warning',
    content: "确定删除此配置？",
    onOk() {
      deletePinSetting({id: record.id}).then(() => {
        reload()
        message.success('删除成功')
      })
    }
  });
}

const resetForm = () => {
  Object.assign(formState, {
    id: '',
    pinName: '',
    pinNumber: 0,
    pinType: 'High',
    pinVal: '1',
    status: 1
  })
}

const submitData = async () => {
  try {
    await formRef.value.validateFields();
    if (formState.pinNumber == 0) {
      message.error('端点编号必须大于0')
      return
    }
    addBtnLoading.value = true
    if (formState.id) {
      await editPinSetting(formState)
    } else {
      await addPinSetting(formState)
    }
    message.success('操作成功')
    await reload()
    resetForm()
  } catch (err) {
    console.log(err)
  } finally {
    addBtnLoading.value = false
  }

}
</script>

<style scoped lang="less">
.sound-container {
  height: calc(100vh - 48px);
  width: 100%;
  min-width: 1366px;
  padding: 15px;
}
</style>

