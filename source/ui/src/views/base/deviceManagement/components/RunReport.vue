<template>
  <a-row style="margin-left: 6px; margin-top: 25px;">
    <a-col :span="12">
      <Line :height="lineHeight" :chart-data="cpuLineData" :option="cpuLineOption"> </Line>
    </a-col>
    <a-col :span="12">
      <Line :height="lineHeight"  :chart-data="gpuLineData" :option="gpuLineOption"></Line>
    </a-col>
  </a-row>
  <a-row style="margin-left: 6px;">
    <a-col :span="12">
      <Line :height="lineHeight"  :chart-data="memLineData" :option="memLineOption"/>
    </a-col>
    <a-col :span="12">
      <Line :height="lineHeight"  :chart-data="diskLineData" :option="diskLineOption"/>
    </a-col>
  </a-row>
</template>

<script lang="ts" setup>
import {ref, unref, watch, watchEffect} from "vue";
  import {Col, Row} from "ant-design-vue";
  import Line from "/@/components/chart/Line.vue";

  const props = defineProps(['devRunInfo']);
  const ACol = Col;
  const ARow = Row;
  const lineHeight = '160px';
  const cpuLineData = ref([{name:'',value:0}]);
  const gpuLineData = ref([{name:'',value:0}]);
  const memLineData = ref([{name:'',value:0}]);
  const diskLineData = ref([{name:'',value:0}]);
  const cpuLineOption = {
    title:{
      text: 'CPU 0%' ,
      left:'left',
      textStyle:{
        color: '#7badc7',
        fontSize: 15,
      },
      backgroundColor:'rgba(123, 173, 199, 0.2)',
    },
    yAxis: {
      max: 100,
      min: 0,
      minInterval: 50,
    },
    xAxis: {
      show: false,
    },
    tooltip:{
      trigger: 'axis',
      formatter: '{c} %'
    }
  }
  const gpuLineOption = {
    title:{
      text: 'GPU 0%' ,
      left:'left',
      textStyle:{
        color: '#7badc7',
        fontSize: 15,
      },
      backgroundColor:'rgba(123, 173, 199, 0.2)',
    },
    yAxis: {
      max: 100,
      min: 0,
      minInterval: 50,
    },
    xAxis: {
      show: false,
    },
    tooltip:{
      trigger: 'axis',
      formatter: '{c} %'
    }
  }
  const memLineOption = {
    title:{
      text: '内存 0%' ,
      left:'left',
      textStyle:{
        color: '#7badc7',
        fontSize: 15,
      },
      backgroundColor:'rgba(123, 173, 199, 0.2)',
    },
    yAxis: {
      max: 100,
      min: 0,
      minInterval: 50,
    },
    xAxis: {
      show: false,
    },
    tooltip:{
      trigger: 'axis',
      formatter: '{c} %'
    }
  }
  const diskLineOption = {
    title:{
      text: '磁盘 0MB' ,
      left:'left',
      textStyle:{
        color: '#7badc7',
        fontSize: 15,
      },
      backgroundColor:'rgba(123, 173, 199, 0.2)',
    },
    yAxis: {
      max: 100,
      min: 0,
      minInterval: 50,
    },
    xAxis: {
      show: false,
    },
    tooltip:{
      trigger: 'axis',
      formatter: '{c} %'
    }
  }

  function updateData(data){
    if(cpuLineData.value.length > 20){
      cpuLineData.value.shift();
      gpuLineData.value.shift();
      memLineData.value.shift();
      diskLineData.value.shift();
    }
    if(JSON.stringify(data) != '{}') {
      cpuLineData.value.push({name: '', value: data.cpuRate});
      gpuLineData.value.push({name: '', value: data.gpuRate});
      memLineData.value.push({name: '', value: data.memRate});
      diskLineData.value.push({name: '', value: data.diskRate});
      cpuLineOption.title.text = 'CPU ' + cpuLineData.value[cpuLineData.value.length - 1].value + '%';
      gpuLineOption.title.text = 'GPU ' + gpuLineData.value[gpuLineData.value.length - 1].value + '%';
      memLineOption.title.text = '内存 ' + memLineData.value[memLineData.value.length - 1].value + '%';
      diskLineOption.title.text = '磁盘 ' + Number.parseFloat((data.diskUse/1024).toFixed(0)) + 'G';
    }
  }

  watchEffect(() => {
    if(props.devRunInfo)
      updateData(props.devRunInfo);
  })

  function clearData(){
    cpuLineData.value = [];
    gpuLineData.value = [];
    memLineData.value = [];
    diskLineData.value = [];
    cpuLineOption.title.text = 'CPU 0%';
    gpuLineOption.title.text = 'GPU 0%';
    memLineOption.title.text = '内存 0%';
    diskLineOption.title.text = '磁盘 0MB';
  }

  defineExpose({
    clearData
  })

</script>

<style scoped>

</style>
