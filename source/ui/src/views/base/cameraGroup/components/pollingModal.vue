<template>
  <div ref="pollingModal">
    <a-modal :title="title" width="1108px" centered v-model:visible="dialogVisible" :ok-text="okText" @ok="handleOk" @cancel="handleCancel" :get-container="getContainer">
      <a-row>
        <a-col :span="4">
          <div style="float: left; width: 85%">
            <div class="info">
              <i class="alg-icon"></i>
              <span class="task">当前任务</span><br />
            </div>
            <div v-for="(item, index) of formState.algList" :key="index" :class="item.algorithmId === selectModelName ? 'alg-div-active' : 'alg-div'">
              <div style="width: 85%; float: left" @click="selectItem(item)">
                <a-button type="link" class="div-btn">{{ item.algorithmName }} </a-button>
              </div>
              <CloseOutlined @click="delAlgItem(item)" style="margin-top: 1px; margin-right: 2px; float: right" />
            </div>
          </div>
          <a-divider type="vertical" style="float: right; height: 100%" />
        </a-col>
        <a-col :span="20">
          <a-row>
            <a-col :span="24">
              <span class="name">{{ ele.groupName }}</span>
              <span class="dot"></span>
              <span style="margin: 4px">当前时间 {{ currentDate }}</span>
            </a-col>
          </a-row>

          <a-row style="margin-top: 8px">
            <a-form>
              <a-col :span="12">
                <a-form-item name="algSelected" class="label" label="选择算法" :wrapper-col="{ span: 14 }" :rules="[{ message: '请选择算法', trigger: 'blur' }]">
                  <div class="select-alg">
                    <!-- :disabled="isDisable(item.value)" -->
                    <a-select v-model:value="formState.algSelected" show-search @change="algSelectChange" :filter-option="filterOption">
                      <template v-for="item in algOptions" :key="`${item.label}`">
                        <a-select-option :value="item.value" :disabled="isDisable(item.text)">
                          <span :title="item.label">
                            {{ item.label }}
                          </span>
                        </a-select-option>
                      </template>
                    </a-select>
                  </div>
                </a-form-item>
              </a-col>
            </a-form>
            <a-form>
              <a-col :span="12">
                <a-form-item name="taskRemark" class="task-note" label="任务备注" :wrapper-col="{ span: 16 }">
                  <a-input v-model:value="formState.taskRemark" placeholder="备注" class="mark" />
                </a-form-item>
              </a-col>
            </a-form>
          </a-row>
          <div class="params">
            <a-row>
              <a-col :span="24" class="name"> 检测策略</a-col>
            </a-row>
            <a-row>
              <a-col span="24">
                <label class="name">布控时间</label>
                <!--                <label style="margin-left: 15px;">生效日期</label>-->
                <!--                <a-range-picker v-model:value="checkDate" style="margin-left: 10px" valueFormat="YYYY-MM-DD"/>-->
              </a-col>
            </a-row>
            <a-row style="margin-top: 10px">
              <a-col span="24" offset="1">
                <TimePlan v-model:value="checkTime" />
              </a-col>
            </a-row>
            <!-- <a-row style="margin-top: 10px">
              <a-col span="24" offset="1">
                <PointPlan v-model:value="time" />
              </a-col>
            </a-row> -->
            <a-row style="margin-top: 10px">
              <a-col span="24">
                <div class="alg-tactics">
                  <label style="margin-left: 32px; font-weight: 550">算法策略</label>
                  <div class="switch">
                    <BaseSwitch v-model:checked="formState.checked" @handle-change="handleCheckedParams" />
                  </div>
                </div>

                <a-row class="slider-box">
                  <a-col offset="2" span="3" >
                    <label style="margin-top: 4px; float: left">算法阈值 <span v-if="uncoveredSoil">(%)</span></label>
                  </a-col>
                  <a-col span="12" offset="1">
                    <a-slider v-model:value="thresholdVal" :min="0" :max="100" :step="1" />
                  </a-col>
                  <a-col offset="2" span="2">
                    <a-input-number v-model:value="thresholdVal" :min="0" :max="100" />
                  </a-col>
                </a-row>
                <a-row v-if="!uncoveredSoil" class="target-box">
                  <a-col offset="2" span="3">
                    <label style="margin-top: 4px; float: left">目标数量阈值</label>
                  </a-col>
                  <a-col span="12" offset="1">
                    <template v-if="formState.selectedLabel === aggregation">
                      <a-slider v-model:value="targetThresholdVal" :min="4" :max="20" :step="1" :disabled="isTargetSlider" />
                    </template>
                    <template v-else>
                      <a-slider v-model:value="targetThresholdVal" :min="0" :max="20" :step="1" :disabled="isTargetSlider" />
                    </template>
                  </a-col>
                  <a-col offset="2" span="2">
                    <a-input-number v-model:value="targetThresholdVal" :disabled="isTargetInput" :min="0" :max="20" />
                  </a-col>
                </a-row>
              </a-col>
            </a-row>
            <a-row>
              <a-col :span="24" class="level">
                <div>
                  <label style="float: left; margin-top: 4px; margin-left: 32px">事件等级</label>
                  <div style="margin-left: 110px">
                    <JDictSelectTag :show-choose-option="false" :value="alarmLevel" @change="levelChange" placeholder="请选择等级" dictCode="alarm_level" />
                    <!-- <base-label :label="alarmLevel" /> -->
                  </div>
                </div>
              </a-col>
            </a-row>
          </div>
        </a-col>
      </a-row>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
  import { computed, ref, reactive, watch, onMounted } from 'vue';
  import { Modal, Row, Col, Form, FormItem } from 'ant-design-vue';
  import BaseSwitch from '/@/components/BaseSwitch/BaseSwitch.vue';
  import { CloseOutlined } from '@ant-design/icons-vue';
  import JDictSelectTag from '/@/components/Form/src/jeecg/components/JDictSelectTag.vue';
  import TimePlan from '/@/components/TimePlan/src/TimePlan.vue';
  import { debounce } from 'lodash-es';
  // import PointPlan from '/@/components/PointPlan/src/PointPlan.vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { addAlgorithm, getGroupAlgInfo, list, deleteAlg, getThreshold, editAlgorithm, saveOrUpdate, getDevIpList, saveGroupAll } from '../CameraGroup.api';
  import { algorithmList, groupList, allAlgInfo, algMsg, selectCamera } from '../CameraGroup.data';
  import axios from 'axios';
  const AModal = Modal;
  const ARow = Row;
  const ACol = Col;
  const AForm = Form;
  const AFormItem = FormItem;

  const { createMessage } = useMessage();
  const props = defineProps<{
    title: string;
    visible: boolean | undefined;
    okText: string;
    //组名
    ele: groupList;
    algList: algorithmList[];
    algOptions: allAlgInfo[];
  }>();

  const checkCycleVal = ref(40);
  const afterUnit = ref('ms');
  const alarmCycleVal = ref(0);
  const selectModelName = ref('');
  const sensitivityVal = ref(2); //算法让默认值改为2
  const thresholdVal = ref(0);
  const targetThresholdVal = ref(0);
  const hitCountVal = ref(3);
  const timeSpaceVal = ref(5);
  const checkTime = ref('');
  const time = ref('');
  const alarmLevel = ref('0');
  const isUpdate = ref(true);
  const currentDate = ref('');
  const algId = ref();
  //目标数量阈值slider
  const isTargetSlider = ref(false);
  //目标数量阈值input
  const isTargetInput = ref(false);
  const formState = reactive({
    algSelected: '',
    taskRemark: '',
    cameraSel: '',
    algInfo: {} as algMsg,
    algList: [] as algorithmList[],
    thresholdVal: 0 as number,
    targetThresholdVal: 0 as number,
    checked: true as boolean,
    selectedLabel: '' as string,
  });
  const mainState = reactive({
    currentDate: '' as string,
    hostUrl: window.location.host,
    hostIp: '',
  });
  const emits = defineEmits(['val', 'handleOk', 'handleCancel', 'title', 'okText', 'cancelText', 'handleDelete', 'isDisable']);

  // 裸土覆盖标志
  const uncoveredSoil = ref(false);

  const isFloat = (num) => {
    return !isNaN(num) && num % 1 !== 0;
  };
  const pollingModal = ref<HTMLElement | string | false>();

  //不可配目标数量阈值的算法数组,10离岗、11单人作业、12人员入侵
  const noConfig: string[] = ['离岗', '单人作业', '人员入侵'];
  //人员聚集的value
  const aggregation = '人员聚集';

  const dialogVisible = computed({
    get() {
      return props.visible;
    },
    set(val) {
      emits('val', val);
    },
  });
  const getAlgList = async () => {
    const listInfo = await list({});
    listInfo.records.forEach((item) => {
      if (props.ele.id === item.id) {
        formState.algList = item.algorithmList;
      }
    });
  };

  watch(
    () => dialogVisible.value,
    (val) => {
      if (val) {
        getAlgList();
      }
    },
    {
      immediate: true,
      deep: true,
    }
  );
  onMounted(() => {
    setInterval(() => {
      //   当前时间
      const date: Date = new Date();
      currentDate.value = date.toLocaleString();
    }, 1000);
    // console.log(checkTime, 'checkTime');
  });
  const title = computed({
    get() {
      return props.title;
    },
    set(val) {
      emits('title', val);
    },
  });

  const okText = computed({
    get() {
      return props.okText;
    },
    set(val) {
      emits('okText', val);
    },
  });

  // 制定modal挂载的节点
  const getContainer = () => {
    return pollingModal.value;
  };

  const map = new Map();
  //选择算法模糊搜索
  const filterOption = (input, option) => {
    props.algOptions.forEach((item) => {
      map.set(item.value, item.label);
    });
    for (const [key, value] of map) {
      if (value.indexOf(input) !== -1 && option.key === key) {
        return option;
      }
    }
  };

  //重置表单
  function resetFieldsValue() {
    selectModelName.value = '';
    formState.algSelected = '';
    formState.taskRemark = '';
    checkTime.value = '';
    checkCycleVal.value = 40;
    thresholdVal.value = formState.thresholdVal;
    targetThresholdVal.value = formState.targetThresholdVal;
    hitCountVal.value = 3;
    alarmCycleVal.value = 0;
    alarmLevel.value = '1';
    sensitivityVal.value = 2;
    timeSpaceVal.value = 4;
  }

  const handleCheckedParams = () => {
    formState.checked = !formState.checked;
    if (formState.checked) {
      document.querySelector<HTMLElement>('.slider-box')!.style.display = 'flex';
      document.querySelector<HTMLElement>('.target-box')!.style.display = 'flex';
    } else {
      document.querySelector<HTMLElement>('.slider-box')!.style.display = 'none';
      document.querySelector<HTMLElement>('.target-box')!.style.display = 'none';
    }
  };

  //浮点数提示
  const checkFloatValue = (val, message) => {
    if (isFloat(val)) {
      createMessage.error(message);
      return true;
    }
    return false;
  };

  //点击完成，需判断当前是更新还是新建
  const handleOk = debounce(async () => {
    if (checkFloatValue(thresholdVal.value, '算法阈值不能输入小数')) {
      return;
    }
    if (checkFloatValue(targetThresholdVal.value, '目标数量阈值不能输入小数')) {
      return;
    }
    let checkTimeAry = checkTime.value.split(';');
    if (checkTimeAry.includes('')) {
      createMessage.error('布控时间不能为空');
      return;
    }
    let params = {
      alarmCycle: alarmCycleVal.value,
      alarmLevel: alarmLevel.value,
      algorithmId: formState.algSelected,
      // algorithmId: algId.value,
      belongTo: '',
      // checkArea: '[]',
      checkCycle: checkCycleVal.value,
      // checkDate: '[]',
      checkTime: checkTime.value,
      // createTime: '',
      groupId: props.ele.id,
      threshold: thresholdVal.value,
      targetThreshold: targetThresholdVal.value,
      hitCount: hitCountVal.value,
      sensitivity: sensitivityVal.value,
      timeSpace: timeSpaceVal.value,
      taskRemark: formState.taskRemark,
    };
    if (formState.algSelected) {
      await saveOrUpdate(params, isUpdate.value);
      const devIpList = await getDevIpList({ groupId: props.ele.id });
      reopen(devIpList);
    }

    emits('handleOk');
    resetFieldsValue();
  }, 50);

  async function reopen(devIpList) {
    mainState.currentDate = new Date().toLocaleString();
    sessionStorage.setItem('date', mainState.currentDate);
    await saveGroupAll({});
    for (const devIp in devIpList) {
      const openUrl = '/kyscene/addjob?' + devIpList[devIp];
      // const closeUrl = '/kyscene/removejob?' + devIpList[devIp];
      const param = { group_id: props.ele.id };
      // try {
      //   const resClose: any = await axios.post(closeUrl, param);
      // } catch (error) {
      //   console.log('请求失败');
      // }
      try {
        const resOpen: any = await axios.post(openUrl, param);
      } catch (error) {
        console.log('请求失败');
      }
    }
  }

  const handleIssued = async () => {
    mainState.currentDate = new Date().toLocaleString();
    sessionStorage.setItem('date', mainState.currentDate);
    await saveGroupAll({});
  };
  //取消
  const handleCancel = () => {
    emits('handleCancel');
    resetFieldsValue();
  };

  //算法change
  async function algSelectChange(value, option) {
    uncoveredSoil.value = "裸土覆盖" === option.key;
    formState.selectedLabel = option.key;
    selectModelName.value = formState.algSelected;
    if (noConfig.includes(formState.selectedLabel)) {
      isTargetSlider.value = true;
      isTargetInput.value = true;
    } else {
      isTargetSlider.value = false;
      isTargetInput.value = false;
    }
    algId.value = formState.algSelected;
    algId.value = null;
    isUpdate.value = false;
    const res = await getThreshold({ id: selectModelName.value }); //变化算法的时候调用默认阈值

    thresholdVal.value = res.threshold;
    targetThresholdVal.value = res.targetThreshold;
  }

  //轮询配置算法弹窗选择算法是否可点击
  const isDisable = (key) => {
    if ("车辆未冲洗" === key) return true
    if (props.algList) {
      for (const v of props.algList) {
        if (v.algorithmName === key) {
          return true;
        }
      }
      return false;
    }
  };

  async function selectItem(item): void {
    uncoveredSoil.value = "裸土覆盖" === item.algorithmName;
    formState.algInfo = await getGroupAlgInfo({ groupId: props.ele.id, algorithmId: item.algorithmId });
    // const res = await getThreshold({ id: item.algorithmId }); //编辑已创建算法时直接读取已经配置的阈值，不重新调用默认阈值接口
    // formState.algInfo.threshold = res.threshold;
    if (noConfig.includes(item.algorithmName)) {
      isTargetSlider.value = true;
      isTargetInput.value = true;
    } else {
      isTargetSlider.value = false;
      isTargetInput.value = false;
    }
    setAlgFieldsValue({ ...formState.algInfo });
    //选择算法点击赋值
    formState.algSelected = formState.algInfo.modelName;
    selectModelName.value = formState.algInfo.modelName;
    alarmLevel.value = formState.algInfo.alarmLevel;
    isUpdate.value = true;
    algId.value = item.algorithmId;
    // formState.thresholdVal = res.threshold;
  }

  //删除单个算法
  const delAlgItem = debounce(async (item) => {
    await deleteAlg({ groupId: props.ele.id, algorithmId: item.algorithmId }, getAlgList);
    const devIpList = await getDevIpList({ groupId: props.ele.id });
    reopen(devIpList);
    emits('handleDelete', item, props.ele);
  }, 50);

  //赋值
  function setAlgFieldsValue(data) {
    // formState.algSelected = data.modelName;
    formState.taskRemark = data.taskRemark;
    checkCycleVal.value = data.checkCycle;
    hitCountVal.value = data.hitCount;
    if (data.threshold === 0) {
      thresholdVal.value = formState.thresholdVal;
    } else {
      thresholdVal.value = data.threshold;
    }
    targetThresholdVal.value = data.targetThreshold;
    timeSpaceVal.value = data.timeSpace;
    alarmCycleVal.value = data.alarmCycle;
    sensitivityVal.value = data.sensitivity;
    checkTime.value = data.checkTime;
  }

  function levelChange(val) {
    alarmLevel.value = val;
  }
</script>
<style lang="less" scoped>
  :deep .ant-modal {
    font-family: Source Han Sans CN;

    .dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background-color: var(--adopt-text-color);
      margin-right: 8px;
      margin-top: 20px;
      display: inline-block;
    }
    .select-alg {
      width: 176px !important;
      height: 28px !important;
      /deep/.ant-select-single .ant-select-selector {
        width: 176px !important;
        height: 28px !important;
        border: 1px solid var(--weak-split-line-color) !important;
        position: relative;
      }
      /deep/ .ant-select-arrow {
        position: absolute;
        right: 5%;
      }
    }
    .label {
      margin: 12px 0 0 -55px;
    }
    .task-note {
      margin: 12px 0 12px 24px;
    }
    .info {
      display: flex;
      align-items: center;
      height: 20px;
      padding: 16px 0 12px 32px;
      height: 40px;
    }
    .alg-icon {
      background: url('/@/assets/svg/icon/tree_icon_algorithm.svg');
      width: 14px;
      height: 14px;
      display: inline-block;
      margin: -2px 4px 0 0;
    }

    .task {
      color: var(--headerline-text-color);
      font-size: 13px;
    }

    .mark {
      width: 240px;
      height: 28px;
      border: 1px solid;
      border-color: var(--weak-split-line-color);
      border-radius: 4px;
    }
    .alg-tactics {
      display: flex;
    }
    .switch {
      margin: 0 0 0 8px;
    }
    .alg-threshold {
      margin: 12px 0 0 0;
    }
    .level {
      margin: 8px 0 8px 0;
    }
    .div-btn {
      margin: 3px;
      border: 0;
      border-radius: 20px;
      color: var(--secondary-text-color);
    }

    .div-btn:hover {
      color: var(--primary-Color);
    }
    .div-btn:ative {
      color: var(--primary-Color);
    }
    .alg-div {
      margin-top: 8px;
      width: 136px;
      height: 25px;
      background-color: var(--input-text-color);
      border-radius: 3px;
      font-family: Source Han Sans CN;
      color: var(--headerline-text-color);
      font-size: 13px;
      display: flex;
      align-items: center;
      margin: 0 0 12px 32px;
    }
    .alg-div:hover,
    .alg-div:active {
      background: rgba(112, 137, 224, 0.2);
      color: var(--primary-Color);
      font-size: 13px;
    }

    .alg-btn:active {
      background: rgba(112, 137, 224, 0.2);
      color: var(--primary-Color);
      font-size: 13px;
    }
    .alg-div:hover {
      // border: 1px solid @primary-Color;
      color: var(--primary-Color);
    }
    .alg-div:hover * {
      color: var(--primary-Color);
    }

    .alg-div-active {
      width: 136px;
      height: 25px;
      background: rgba(112, 137, 224, 0.2);
      color: var(--primary-Color);
      font-size: 13px;
      display: flex;
      align-items: center;
      margin: 0 0 12px 32px;
    }
    .alg-div-active * {
      color: var(--primary-Color);
    }
    .name {
      margin: 16px 8px 12px 24px;
      font-weight: 550;
    }
    .text {
      color: #7f8599;
      font-size: 12px;
      font-weight: 550;
    }
    .ant-modal-footer {
      display: flex !important;
      justify-content: flex-end !important;
      align-items: center;
    }
    .ant-modal-footer .ant-btn {
      width: 64px;
      height: 24px;
      background-color: var(--fill-color);
      border-radius: 4px;
      color: var(--secondary-text-color);
      cursor: pointer;
      padding-top: 2px;
    }
    .ant-modal-footer .ant-btn:hover {
      background-color: var(--fill-two-color);
      color: var(--primary-Color);
    }
    .ant-modal-footer .ant-btn:active {
      background-color: var(--input-text-color);
      border-color: #7089ed;
      color: var(--primary-Color-actived);
    }
    .ant-modal-footer .ant-btn-primary {
      margin-left: 8px;
      width: 64px;
      height: 24px;
      background-color: var(--primary-Color);
      border-radius: 4px;
      color: rgba(255, 255, 255, 0.95);
      border: none;
      cursor: pointer;
    }

    .ant-modal-footer .ant-btn-primary:hover {
      background-color: var(--primary-Color-hover);
      color: rgba(255, 255, 255, 0.95);
    }
    .ant-modal-footer .ant-btn-primary:active {
      background-color: var(--primary-Color-actived);
    }
  }
  .target-box {
    margin: 8px 0 0 0;
  }
</style>
