<template>
  <div>
    <BasicTable @register="registerTable" :search-info="searchInfo">
      <template #resultStatus="{ record }">
        <a-tag :color="record.resultStatus === 1 ? '#7bbdc7' : '#cc6666'">
          {{ record.resultStatus === 1 ? '升级成功' : '升级失败' }}
        </a-tag>
      </template>
      <template #expandedRowRender="{ record }">
        <a-steps direction="vertical" progress-dot :current="1" class="steps-left" >
          <a-step :status="record.downStatus === 1 ? 'finish': 'error'" key="0" title="软件下发">
            <template v-slot:description>
              <p>{{"执行时间"}}<span style="margin-left: 2%;">{{ record.downTime}}</span></p>
              <p>{{"返回结果"}}<span style="margin-left: 2%;">{{ record.downStatus === 1 ? "成功": "失败"}}</span></p>
              <p>{{"返回描述"}}<span style="margin-left: 2%;">{{ record.downDes}}</span></p>
            </template>
          </a-step>
          <a-step :status="record.backupStatus === 1 ? 'finish': 'error'" key="1"  title="备份">
            <template v-slot:description>
              <p>{{"执行时间"}}<span style="margin-left: 2%;">{{ record.backupTime}}</span></p>
              <p>{{"返回结果"}}<span style="margin-left: 2%;">{{ record.backupStatus === 1 ? "成功": "失败"}}</span></p>
              <p>{{"返回描述"}}<span style="margin-left: 2%;">{{ record.backupDes}}</span></p>
            </template>
          </a-step>
          <a-step :status="record.upStatus === 1 ? 'finish': 'error'" key="2"  title="升级">
            <template v-slot:description>
              <p>{{"执行时间"}}<span style="margin-left: 2%;">{{ record.upTime}}</span></p>
              <p>{{"返回结果"}}<span style="margin-left: 2%;">{{ record.upStatus === 1 ? "成功": "失败"}}</span></p>
              <p>{{"返回描述"}}<span style="margin-left: 2%;">{{ record.upDes}}</span></p>
            </template>
          </a-step>
          <a-step :status="record.resultStatus === 1 ? 'finish': 'error'" key="3"  title="升级结果">
            <template v-slot:description>
              <p>{{"执行时间"}}<span style="margin-left: 2%;">{{ record.resultTime}}</span></p>
              <p>{{"返回结果"}}<span style="margin-left: 2%;">{{ record.resultStatus === 1 ? "成功": "失败"}}</span></p>
              <p>{{"返回描述"}}<span style="margin-left: 2%;">{{ record.resultDes}}</span></p>
            </template>
          </a-step>
        </a-steps>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              label: '详细记录',
              onClick: showDetail(),
            },
          ]"
        />
      </template>
    </BasicTable>
  </div>
</template>

<script lang="ts" setup>
import {defineExpose, reactive} from "vue";
  import {BasicTable, TableAction, useTable} from "/@/components/Table";
  import {Steps, Step} from 'ant-design-vue';
  import { logList } from "../DeviceManagement.api";
  import { logColumns } from "../DeviceManagement.data";

  const ASteps = Steps;
  const AStep = Step;

  const searchInfo = reactive<Recordable>({'devid': 0});
  //注册table数据
  const [ registerTable, { reload } ] = useTable({
    api: logList,
    title: '',
    columns: logColumns,
    rowKey: 'id',
    canResize: false,
    expandRowByClick: false,
    // actionColumn: {
    //   width: 160,
    //   title: '操作',
    //   dataIndex: 'action',
    //   slots: { customRender: 'action' },
    // },
  });

  function showDetail() {
    console.log('点击了详情');
  }

  function updateData(devId){
    searchInfo.devid = devId;
    reload();
  }

  defineExpose({
    updateData
  });

</script>

<style scoped>
  .steps-left {
    margin-left: 8%;
  }


</style>
