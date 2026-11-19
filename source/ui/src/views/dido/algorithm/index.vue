<template>
  <div class="sound-container">
    <a-card title="算法配置" :bordered="false" style="width: 100%">
      <a-form
        ref="formRef"
        :model="formState"
        :label-col="{span: 6}"
        :wrapper-col="{span: 18}"
        autocomplete="off">
        <a-row :gutter="24">
          <a-col :span="6">
            <a-form-item label="IO端点" name="serialPinId" :rules="rulesState.serialPinId">
              <a-select placeholder="请选择端点"
                        v-model:value="formState.serialPinId" style="width: 100%">
                <a-select-option v-for="item in GPOList" :key="item.id" :value="item.id">
                  {{ item['pinName'] }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="2">
            <a-button type="primary" @click="testPin" :loading="testBtnLoad">测试 pin</a-button>
          </a-col>
          <a-col :span="6">
            <a-form-item label="绑定算法" name="algorithmCode" :rules="rulesState.algorithmCode">
              <a-select placeholder="请选择需要绑定的算法"
                        v-model:value="formState.algorithmCode" style="width: 100%"
                        :options="algorithmArr">
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="状态" name="status" :rules="rulesState.status">
              <a-select placeholder="请选择状态" v-model:value="formState.status">
                <a-select-option :value="1">启用</a-select-option>
                <a-select-option :value="0">禁用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-button type="primary" @click="submit" :loading="submitBtnLoad">提 交</a-button>
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
        <template #algorithmCode="{record}">
          {{ getAlgorithmName(record['algorithmCode']) }}
        </template>
      </BasicTable>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from "vue";
import {list, testPinVal} from "/@/views/dido/pin/index";
import {
  addData,
  deleteRow,
  getAlgorithmList,
  listPage,
  updateDate
} from "/@/views/dido/algorithm/index";
import {ActionItem, BasicColumn, BasicTable, TableAction} from "/@/components/Table";
import {useListPage} from "/@/hooks/system/useListPage";
import {useMessage} from "/@/hooks/web/useMessage";
import {message} from "ant-design-vue";

const {createMessage, createConfirm} = useMessage()

const submitBtnLoad = ref(false)
const testBtnLoad = ref(false)

const formRef = ref()

const formState = reactive({
  id: '',
  serialPinId: undefined,
  algorithmCode: undefined,
  status: 1
})

const rulesState = {
  serialPinId: [{required: true, message: '请选择端点'}],
  algorithmCode: [{required: true, message: '请选择算法'}],
  status: [{required: true, message: '请选择状态'}],
}

const GPOList = ref([])
const algorithmArr = reactive([]);

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

const getAlgorithmName = (algorithmCode: string) => {
  const itemObj = algorithmArr.find(item => item.value == algorithmCode)
  if (itemObj) return itemObj.label
  return algorithmCode
}

onMounted(() => {
  getGpOList()
  initAlgorithmList()
})

const getGpOList = async () => {
  GPOList.value = await list() || []
}

const initAlgorithmList = async () => {
  const algorithmList = await getAlgorithmList()
  if (algorithmList && algorithmList.length > 0) {
    algorithmArr.length = 0
    for (const item of algorithmList) {
      algorithmArr.push({label: item['name'], value: item['code']})
    }
  }
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
    title: '绑定的算法',
    align: 'center',
    dataIndex: 'algorithmCode',
    slots: {customRender: 'algorithmCode'},
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

const handleEdit = (row: any) => {
  console.log(row)
  formState.id = row.id
  formState.serialPinId = row.serialPinId
  formState.algorithmCode = row.algorithmCode
  formState.status = row.status
  message.info('配置信息已填充到上面配置表单')
}

const handleDelete = async (row: any) => {
  createConfirm({
    iconType: 'warning',
    content: "确定删除此配置？",
    onOk() {
      deleteRow({id: row.id}).then(() => {
        reload()
        message.success('删除成功')
      })
    }
  });
}

const submit = async () => {
  try {
    await formRef.value.validateFields();
    submitBtnLoad.value = true
    if (formState.id) {
      await updateDate(formState)
    } else {
      await addData(formState)
    }
    createMessage.success("操作成功")
    await reload()
  } finally {
    submitBtnLoad.value = false
  }
}

const testPin = async () => {
  if (!formState.serialPinId) {
    createMessage.error("请选择一个IO端点")
    return
  }

  try {
    testBtnLoad.value = true
    await testPinVal({id: formState.serialPinId})
    createMessage.success("操作成功")
  } finally {
    testBtnLoad.value = false
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
