<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose title="算法任务" :default-fullscreen="true" @ok="handleSubmit" @cancel="handleCancel" ok-text="完成">
    <a-form ref="formRef" :scroll-to-first-error="true" :model="formState" :label-col="{ style: { width: '90px' } }" :colon="false" @finish="onFinish">
      <a-row>
        <a-col span="5">
          <div style="float: left; width: 85%">
            <span style="margin-bottom: 4px">当前任务</span><br />
            <div v-for="(item, index) of algList" :key="index" :class="item.modelName === selectModelName ? 'alg-div-active' : 'alg-div'">
              <div style="width: 85%; float: left" @click="selectItem(item)">
                <a-button type="link" class="div-btn">
                  {{ item.modelName_dictText }}
                </a-button>
              </div>
              <CloseOutlined @click="delAlgItem(item)" style="margin-top: 1px; margin-right: 2px; float: right" />
            </div>
          </div>
          <a-divider type="vertical" style="float: right; height: 100%" />
        </a-col>
        <a-col span="19">
          <a-row v-if="cameraName !== '' && cameraName !== '请选择相机'">
            <a-col span="24">
              <span class="name">{{ cameraName }}</span>
              <!-- <CheckCircleTwoTone style="margin-left: 20px" /> -->
              <span class="dot"></span>
              <span style="margin: 4px">当前时间 {{ dateFormat(nowDate) }}</span>
            </a-col>
          </a-row>
          <a-row v-else>
            <a-col span="8">
              <!--                <label style="color: red;margin-right: 4px;font-family: SimSun, sans-serif;">*</label><a-cascader expand-trigger="hover" show-search placeholder="请选择相机" v-model:value="casCameraName" :field-names="{label:'title', value:'key'}" :options="casOptions" @change="casChange"/>-->
              <a-form-item name="casCameraName" label="" :wrapper-col="{ span: 14 }" :rules="[{ required: true, message: '请选择相机', trigger: 'blur' }]">
                <a-cascader
                  expand-trigger="hover"
                  show-search
                  placeholder="请选择相机"
                  v-model:value="casCameraName"
                  :field-names="{ label: 'title', value: 'key' }"
                  :options="casOptions"
                  @change="casChange"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row style="margin-top: 8px">
            <a-col :span="8">
              <a-form-item name="algSelected" label="选择算法" :wrapper-col="{ span: 14 }" :rules="[{ message: '请选择算法', trigger: 'blur' }]">
                <div class="select-alg">
                  <!-- :disabled="isDisable(item.value)" -->
                  <a-select v-model:value="formState.algSelected" show-search @change="algSelectChange" :filter-option="filterOption">
                    <template v-for="item in algOptions" :key="`${item.label}`">
                      <a-select-option :value="item.value" :disabled="isDisable(item)">
                        <span :title="item.label">
                          {{ item.label }}
                        </span>
                      </a-select-option>
                    </template>
                  </a-select>
                </div>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item name="taskRemark" label="任务备注" :wrapper-col="{ span: 16 }">
                <a-input v-model:value="formState.taskRemark" placeholder="备注" class="mark" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-divider style="margin-top: 1px" />
          <div class="content">
            <a-row>
              <a-col v-if="cameraName !== '' && cameraName !== '请选择相机'">
                <a-row>
                  <a-col span="24">
                    <label class="roi">检测区域ROI</label>
                    <a-button v-if="!uncoveredSoil && !isGrime && !isBelt && !isIntrusion" :class="roiAreaType === ROIAreaType.Full ? 'roi-btn roi-btn-active roi-btn-poly-active' : 'roi-btn'" size="small" @click="allImgBtnClick"
                    ><img style="float: left; margin-top: 4px; margin-right: 4px" :src="allBtnIcon" alt="" /> 全屏</a-button>
                    <a-button :class="roiAreaType === ROIAreaType.Rect ? 'roi-btn roi-btn-active roi-btn-poly-active' : 'roi-btn'" size="small" @click="rectBtnClick"
                    ><img style="float: left; margin-top: 4px; margin-right: 4px" :src="rectangleBtnIcon" alt="" /> 矩形</a-button
                    >
                    <a-button v-if="!isGrime" :class="roiAreaType === ROIAreaType.Polygon ? 'roi-btn roi-btn-active roi-btn-poly-active' : 'roi-btn'" size="small" @click="mulBtnClick"
                    ><img style="float: left; margin-top: 4px; margin-right: 4px" :src="polygonBtnIcon" alt="" />多边形</a-button
                    >
<!--                    <span class="divider"></span>-->
                    <a-button v-if="!uncoveredSoil && !isGrime && !isBelt && !isIntrusion" :class="roiAreaType === ROIAreaType.InvalidArea ? 'roi-btn roi-btn-active roi-btn-invalid-active' : 'roi-btn'" size="small" @click="invalidClick"
                    ><img style="float: left; margin-top: 4px; margin-right: 4px" :src="invalidBtnIcon" alt="" />无效区域</a-button
                    >
                    <a-button class="reset-btn" size="small" @click="clearROI"><img style="float: left; margin-top: 4px; margin-right: 4px" :src="resetBtnIcon" alt="" /> 重置</a-button>
                  </a-col>
                </a-row>
                <!--              <a-row style="margin-top: 6px;">-->
                <!--                <a-col  offset="2" span="10">-->
                <!--                  <label>最小识别像素 x:{{ xVal }} y:{{ yVal }}</label>-->
                <!--                </a-col>-->
                <!--              </a-row>-->
                <a-row style="margin-top: 10px; margin-bottom: 10px; margin-left: -70px">
                  <a-col span="24" class="wrap">
                    <p class="tips">
                      <i class="tips-icon"></i>
                      <span class="tips-text">当前最小识别像素 91×54</span>
                    </p>

                    <template v-if="loading">
                      <div class="loading">
                        <i class="loading-icon"></i>
                        <span>图像加载中，请稍后…</span>
                      </div>
                    </template>
                    <template v-else>
                      <!--                  <img ref="cameraImg" style="display: none" src="../../../../assets/svg/login-box-bg.svg" @onload="loadROIImg">-->
                      <img ref="cameraImg" style="display: none" :src="roiImgSrc" @load="loadROIImg" />
                      <canvas ref="cameraCanvas" width="752" height="423" style="border: 1px solid"> 您的浏览器不支持 HTML5 canvas 标签。 </canvas>
                      <canvas ref="cameraSaveCanvas" width="752" height="423" @mousedown="canvasMouseDown($event)" @mouseup="canvasMouseUp($event)" @mousemove="canvasMouseMove($event)"></canvas>
                    </template>

                    <!--                  <img ref="cameraImg" style="display: none" src="../../../../assets/svg/login-box-bg.svg" @onload="loadROIImg">-->
                    <!-- <img ref="cameraImg" style="display: none" :src="roiImgSrc" @onload="loadROIImg" />
                    <canvas ref="cameraCanvas" width="752" height="423" style="border: 1px solid"> 您的浏览器不支持 HTML5 canvas 标签。 </canvas>
                    <canvas ref="cameraSaveCanvas" width="752" height="423" @mousedown="canvasMouseDown($event)" @mouseup="canvasMouseUp($event)" @mousemove="canvasMouseMove($event)"></canvas> -->
                  </a-col>
                </a-row>
              </a-col>
              <a-col v-else span="24" style="text-align: -webkit-center; margin-bottom: 10px">
                <div style="background-color: grey; text-align: center; width: 80%; height: 400px; padding-top: 200px">
                  <label>暂未选定相机</label>
                </div>
              </a-col>
            </a-row>
            <a-row>
              <a-col span="24" class="control-time">
                <label class="head">布控时间</label>
                <!--                <label style="margin-left: 15px;">生效日期</label>-->
                <!--                <a-range-picker v-model:value="checkDate" style="margin-left: 10px" valueFormat="YYYY-MM-DD"/>-->
              </a-col>
            </a-row>
            <a-row style="margin-top: 10px">
              <a-col span="24" offset="1">
                <TimePlan v-model:value="checkTime" />
              </a-col>
            </a-row>
            <!--            <a-row style="margin-top: 10px;">-->
            <!--              <a-col span="10">-->
            <!--                <div style="float: left; padding-top: 5px;">-->
            <!--                  <label>检测周期</label>-->
            <!--                </div>-->
            <!--                <a-input-number v-model:value="iouThreshold" style="width: 50%; margin-left: 20px;" placeholder="周期" :min="2" :max="1000" addon-after="s"/>-->
            <!--              </a-col>-->
            <!--            </a-row>-->

            <a-row style="margin-top: 10px">
              <a-col span="24">
                <label class="head">事件策略</label>
                <a-row>
                  <a-col offset="2" :span="10">
                    <div>
                      <label style="float: left; margin-top: 4px; margin-left: 27px">事件等级</label>
                      <div style="margin-left: 110px">
                        <JDictSelectTag :show-choose-option="false" :value="alarmLevel" @change="levelChange" placeholder="请选择等级" dictCode="alarm_level" />
                        <!-- <base-label :label="alarmLevel" /> -->
                      </div>
                    </div>
                  </a-col>
                  <a-col offset="2" span="10">
                    <a-tooltip placement="top">
                      <template #title> 本算法任务触发一次事件推送后，N秒内不再推送事件 </template>
                      <div style="margin-top: 5px; float: left">
                        <label style="float: left">事件推送间隔</label>
                        <img style="margin-top: 3px" :src="infoIcon" alt="" />
                      </div>
                    </a-tooltip>
                    <a-input-number v-model:value="alarmCycleVal" :min="1" style="width: 30%; margin-left: 15px" placeholder="推送周期" addon-after="s" />
                  </a-col>
                </a-row>

                <!-- <a-row style="margin-top: 10px"> </a-row> -->
              </a-col>
              <a-button id="btn" style="display: none" type="primary" html-type="submit" /> </a-row
            ></div>
          <a-row>
            <span>高级参数</span>
            <div class="switch">
              <BaseSwitch v-model:checked="formState.checked" @handle-change="handleCheckedParams" />
            </div>
          </a-row>
          <div class="params" v-if="formState.checked">
            <template v-if="!uncoveredSoil">
              <a-row>
                <a-col :span="24" class="head"> 检测策略</a-col>
              </a-row>
              <a-row style="margin-top: 10px">
                <a-col :span="12">
                  <a-tooltip placement="top">
                    <template #title> 跟踪事件结果时，前后相邻两个结果之间的重合度(至少50%) </template>
                    <div style="margin-top: 5px; float: left">
                      <label style="float: left">跟踪框交并比</label>
                      <img style="margin-top: 3px" :src="infoIcon" alt="" />
                    </div>
                  </a-tooltip>
                  <a-input-number v-model:value="iouThresholdVal" style="width: 30%; margin-left: 15px" :min="50" :max="100" placeholder="跟踪框交并比" addon-after="%" />
                </a-col>
                <!-- <a-row style="margin-top: 10px"> -->
                <a-col :span="12">
                  <a-tooltip placement="top">
                    <template #title> 连续N秒内检测到相同事件结果，首次产生告警 </template>
                    <div style="margin-top: 5px; float: left">
                      <label style="float: left">时间持续间隔</label>
                      <img style="margin-top: 3px" :src="infoIcon" alt="" />
                    </div>
                  </a-tooltip>
                  <a-input-number v-model:value="timeSpaceVal" style="width: 30%; margin-left: 15px" :min="1" placeholder="时间持续间隔" addon-after="s" />
                </a-col>
              </a-row>
              <a-row style="margin-top: 10px">
                <a-col span="12">
                  <a-tooltip placement="top">
                    <template #title>当跟踪结果切换后保持N秒，产生告警</template>
                    <div style="margin-left: 2px; margin-top: 5px; float: left">
                      <label style="float: left">连续命中周期</label>
                      <img style="margin-top: 3px" :src="infoIcon" alt="" />
                    </div>
                  </a-tooltip>
                  <a-input-number v-model:value="hitCountVal" style="width: 30%; margin-left: 15px" :min="1" placeholder="连续命中周期" addon-after="s" />
                </a-col>

                <a-col span="12">
                  <a-tooltip placement="top">
                    <template #title>有可能是误报事件结果，N秒后产生告警</template>
                    <div style="margin-left: 1px; margin-top: 5px; float: left">
                      <label style="float: left">误报过滤周期</label>
                      <img style="margin-top: 3px" :src="infoIcon" alt="" />
                    </div>
                  </a-tooltip>

                  <a-input-number v-model:value="sensitivityVal" style="width: 30%; margin-left: 15px" :min="1" placeholder="误报过滤周期" addon-after="s" />
                </a-col>
              </a-row>
            </template>

            <a-row style="margin-top: 10px">
              <a-col span="24">
                <label class="head">算法策略</label>
                <a-row>
                  <a-col offset="2" span="2">
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

                  <template v-if="path === '/base/camera'">
                    <a-col span="12">
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
                  </template>
                  <template v-else>
                    <a-col span="12">
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
                  </template>
                </a-row>

                <a-row v-if="isGrime || isBelt || isIntrusion">
                  <a-col offset="2" span="2">
                    <label style="margin-top: 4px; float: left">帧间隔</label>
                  </a-col>
                  <a-col span="12" offset="1">
                    <a-slider v-model:value="frameInterval" :min="1" :max="100" :step="1" />
                  </a-col>
                  <a-col offset="2" span="2">
                    <a-input-number v-model:value="frameInterval" :min="1" :max="100" />
                  </a-col>
                </a-row>
              </a-col>
            </a-row>
          </div>
        </a-col>
      </a-row>
    </a-form>
  </BasicModal>
</template>

<script lang="ts" setup>
import {computed, defineEmits, onMounted, reactive, ref, unref} from 'vue';
import {useRouter} from 'vue-router';
import {CascaderProps, Col, Input, InputNumber, Row, Select, SelectProps} from 'ant-design-vue';
import {CloseOutlined} from '@ant-design/icons-vue';
import {BasicModal, useModalInner} from '/@/components/Modal';
import {
  cameraTree,
  deleteOne,
  getThreshold,
  imgInfo,
  list,
  saveConfig,
  saveOrUpdate
} from '../AlgorithmManager.api';
import {initDictOptions} from '/@/utils/dict';
import {debounce, omit} from 'lodash-es';

import TimePlan from '/@/components/TimePlan/src/TimePlan.vue';
import JDictSelectTag from '/@/components/Form/src/jeecg/components/JDictSelectTag.vue';
import BaseSwitch from '/@/components/BaseSwitch/BaseSwitch.vue';

import resetBtnIcon from '/@/assets/svg/icon/editor_btn_resetxs_default.svg';
import allBtnIcon from '/@/assets/svg/icon/algo_btn_roifullscreen_default.svg';
import polygonBtnIcon from '/@/assets/svg/icon/algo_btn_roipolygon_default.svg';
import rectangleBtnIcon from '/@/assets/svg/icon/algo_btn_roirectangle_default.svg';
import invalidBtnIcon from '/@/assets/svg/icon/algo_btn_mask_default.svg';
import infoIcon from '/@/assets/svg/icon/info_icon_help.svg';
import {useMessage} from '/@/hooks/web/useMessage';

import {
  CanvasArea,
  CanvasPoint,
  CheckArea,
  CheckPoint,
  ROIAreaType
} from '../AlgorithmManager.data';

const emit = defineEmits(['register', 'success']);
const props = defineProps<{
  threval: number;
  targetThreval: number;
  enableFaceAlgorithm: boolean
}>();

const { createMessage } = useMessage();
const { currentRoute } = useRouter();
const path = unref(currentRoute).path;

const isUpdate = ref(true);
const ARow = Row;
const ACol = Col;
const ASelect = Select;
const AInputNumber = InputNumber;
const AInput = Input;
const algList = ref<any>([]);
const algId = ref();
//目标数量阈值slider
const isTargetSlider = ref(false);
//目标数量阈值input
const isTargetInput = ref(false);
const selectModelName = ref();
const casCameraName = ref();
const cameraName = ref('');
const cameraId = ref('');
const alarmCycleVal = ref(600);
// 跟踪框交并比
const iouThresholdVal = ref(70);
//误报过滤周期
const sensitivityVal = ref(2);
//连续命中周期
const hitCountVal = ref(5);
//事件持续间隔
const timeSpaceVal = ref(4);
const xVal = ref(0);
const yVal = ref(0);
const checkTime = ref('');
const checkDate = ref([]);
const thresholdVal = ref(50);
const targetThresholdVal = ref();

// 帧间隔
const frameInterval = ref(10)

const checkArea = ref<CheckArea[]>([]);
const invalidArea = ref<CheckArea[]>([]);
const alarmLevel = ref('0');
const nowDate = ref(new Date());
const casOptions = ref<CascaderProps['options']>([]);
const algOptions = ref<SelectProps['options']>([]);
const cameraImg = ref();
const cameraCanvas = ref();
const cameraSaveCanvas = ref();

const loading = ref(false);
const cameraCtx = computed(() => {
  return cameraCanvas.value?.getContext('2d');
});
const cameraSaveCtx = computed(() => {
  return cameraSaveCanvas.value?.getContext('2d');
});
const roiImgSrc = ref('../../../../assets/svg/login-box-bg.svg');
const roiMoveBeforeX = ref(0);
const roiMoveBeforeY = ref(0);
const roiAreaType = ref<ROIAreaType>(0);
const roiAreaAry = ref<CanvasArea>([]);
const roiMoveToStart = ref(-1);
const formRef = ref();
const formState = reactive({
  algSelected: '',
  taskRemark: '',
  cameraSel: '',
  checked: false,
  hostUrl: window.location.host,
  hostIp: '',
  selectedLabel: ''
});

// 裸土覆盖
const uncoveredSoil = ref(false);

// 是否为油污检测
const isGrime = ref(false)

// 是否为皮带检测
const isBelt = ref(false)

// 人机入侵
const isIntrusion = ref(false)

function isFloat(num) {
  return !isNaN(num) && num % 1 !== 0;
}

//不可配目标数量阈值的算法数组,10离岗、11单人作业、12人员入侵
const noConfig: string[] = ['离岗', '单人作业', '人员入侵'];
//人员聚集的value
const aggregation = '人员聚集';

// const { resetFields, validate } = Form.useForm(formState);
const enableFaceAlgorithm = ref(false)

//表单赋值
const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
  enableFaceAlgorithm.value = data.enableFaceAlgorithm

  //重置表单
  // await resetFields();
  algList.value = data?.algList;
  algId.value = data?.record?.id;
  cameraName.value = data?.cameraName;
  cameraId.value = data?.cameraId;
  casCameraName.value = '';
  setModalProps({ confirmLoading: false, showCancelBtn: !!data?.showFooter, showOkBtn: !!data?.showFooter, width: 1200 });
  isUpdate.value = !!data?.isUpdate;
  //不是从相机配置进来，而是算法实例，需要判断是不是从单人作业/离岗/人员入侵点击进来的
  if (path !== '/base/camera') {
    data?.algList.forEach((ele) => {
      if (noConfig.includes(ele.alarmType_dictText)) {
        isTargetInput.value = true;
        isTargetSlider.value = true;
      } else {
        isTargetInput.value = false;
        isTargetSlider.value = false;
      }
    });
  }

  loadROIImg();
  await loadCameraImg();

  await initDictData();
  if (unref(isUpdate)) {
    // 表单赋值
    setAlgFieldsValue({ ...data.record });
  } else if (data?.cameraId) {
    allImgBtnClick();
  }
  let v = await cameraTree();
  casOptions.value = [
    {
      key: 'device',
      title: '设备',
      children: v.deviceTree,
    },
    {
      key: 'location',
      title: '分组',
      children: v.locationTree,
    },
  ];
});

//高级参数控制
const handleCheckedParams = () => {
  formState.checked = !formState.checked;
};

const map = new Map();
//选择算法模糊搜索
const filterOption = (input, option) => {
  algOptions.value?.forEach((item) => {
    map.set(item.value, item.label);
  });
  for (const [key, value] of map) {
    if (value.indexOf(input) !== -1 && option.key === key) {
      return option;
    }
  }
};

function levelChange(val) {
  alarmLevel.value = val;
}

//回显ROI
function echoRoi(areaVal) {
  if (!areaVal) {
    return;
  }
  //回显roi区域
  for (const area of areaVal) {
    // roiAreaType.value = !(checkArea.value.length === 5 && checkArea.value[0][1] === checkArea.value[1][1]);
    roiAreaType.value = ROIAreaType.Rect;
    cameraCtx.value.beginPath();
    cameraCtx.value.moveTo(area[0][0] * cameraCanvas.value.width, area[0][1] * cameraCanvas.value.height);

    // let beginPoint = { x: area[0][0], y: area[0][1] };
    for (let i = 1; i < area.length; i++) {
      cameraCtx.value.lineTo(area[i][0] * cameraCanvas.value.width, area[i][1] * cameraCanvas.value.height);
      // if (area[i][0] === beginPoint.x && area[i][1] === beginPoint.y && i < area.length - 1) {
      //   beginPoint = { x: area[i + 1][0], y: area[i + 1][1] };
      //   cameraCtx.value.closePath();
      //   cameraCtx.value.beginPath();
      //   cameraCtx.value.moveTo(area[i + 1][0] * cameraCanvas.value.width, area[i + 1][1] * cameraCanvas.value.height);
      //   cameraCtx.value.strokeStyle = 'green';
      //   cameraCtx.value.width = 2;
      //   i++;
      // }
    }
    cameraCtx.value.closePath();

    if (areaVal === checkArea.value) {
      cameraCtx.value.fillStyle = 'rgba(255,255,255,0)'; //填充颜色
      cameraCtx.value.lineWidth = 1;
      cameraCtx.value.setLineDash([0]);
      cameraCtx.value.strokeStyle = 'green';
    } else {
      // 设置填充的样式为斜线图案
      const pattern = cameraCtx.value.createPattern(createDiagonalPattern(), 'repeat');
      cameraCtx.value.fillStyle = pattern;
      cameraCtx.value.lineWidth = 1;
      cameraCtx.value.strokeStyle = 'gray';
    }

    cameraCtx.value.fill();
    cameraCtx.value.stroke(); //绘制
    changeLineColor();
  }
}

function setAlgFieldsValue(data) {
  selectModelName.value = data.modelName;
  formState.algSelected = data.modelName;
  formState.taskRemark = data.taskRemark;
  iouThresholdVal.value = data.iouThreshold;
  hitCountVal.value = data.hitCount;
  if (data.threshold === 0) {
    thresholdVal.value = props.threval;
  } else {
    thresholdVal.value = data.threshold;
  }
  targetThresholdVal.value = data.targetThreshold;
  timeSpaceVal.value = data.timeSpace;
  alarmCycleVal.value = data.alarmCycle;
  sensitivityVal.value = data.sensitivity;
  checkDate.value = JSON.parse(data.checkDate);
  checkTime.value = data.checkTime;
  alarmLevel.value = data.alarmLevel;

  if (data?.checkArea || data?.invalidArea) {
    cameraCtx.value.clearRect(0, 0, cameraCanvas.value.width, cameraCanvas.value.height);
    //刚进来没有画，没有值，不要被转化为null
    checkArea.value = JSON.parse(data.checkArea);
    invalidArea.value = JSON.parse(data.invalidArea);
    loadROIImg();
    if (checkArea.value?.length === 0 && invalidArea.value?.length === 0) {
      roiAreaType.value = ROIAreaType.Full;
      return;
    }
  }
  echoRoi(checkArea.value);
  echoRoi(invalidArea.value);
}

function loadROIImg() {
  if (cameraCtx.value) {
    try {
      cameraCtx.value.drawImage(cameraImg.value, 0, 0, 760, 428);
    } catch (e) {
      // console.error(e);
    }
  }
}

function canvasMouseMove(e) {
  if (roiAreaType.value === ROIAreaType.Full) return;
  xVal.value = e.clientX;
  yVal.value = e.clientY;
  let xy = changeToCanvasXY(e.clientX, e.clientY);

  let piX, piY;
  let ctx = cameraSaveCtx.value;
  /*清空画布*/
  ctx.clearRect(0, 0, cameraSaveCtx.value.width, cameraSaveCtx.value.height);

  /*鼠标下跟随的圆点*/
  makEarc(ctx, xy.x, xy.y, GetRandomNum(1, 1), 0, 180, 'rgba(102,168,255,1)');

  if (roiAreaAry.value.length > 0) {
    if (xy.x > roiAreaAry.value[0].x - 38 && xy.x < roiAreaAry.value[0].x + 38 && xy.y > roiAreaAry.value[0].y - 21 && xy.y < roiAreaAry.value[0].y + 21) {
      if (roiAreaAry.value.length > 1) {
        piX = roiAreaAry.value[0].x;
        piY = roiAreaAry.value[0].y;
        ctx.clearRect(0, 0, cameraSaveCtx.value.width, cameraSaveCtx.value.height);
        makEarc(ctx, piX, piY, GetRandomNum(1, 1), 0, 180, 'rgba(102,168,255,1)');
        roiMoveToStart.value = 1;
      }
    } else {
      piX = xy.x;
      piY = xy.y;
      roiMoveToStart.value = -1;
    }
    if (roiAreaType.value === ROIAreaType.Polygon || roiAreaType.value === ROIAreaType.InvalidArea) {
      /*开始绘制*/
      ctx.beginPath();
      ctx.moveTo(roiAreaAry.value[0].x, roiAreaAry.value[0].y);

      if (roiAreaAry.value.length > 1) {
        for (let i = 1; i < roiAreaAry.value.length; i++) {
          makEarc(ctx, piX, piY, GetRandomNum(1, 1), 0, 180, 'rgba(102,168,255,1)');
          ctx.lineTo(roiAreaAry.value[i].x, roiAreaAry.value[i].y);
        }
      }

      ctx.lineTo(piX, piY);

      if (roiAreaType.value === ROIAreaType.InvalidArea) {
        ctx.lineWidth = 1;
      } else {
        ctx.lineWidth = 1;
        ctx.setLineDash([0]);
      }

      ctx.closePath();
      // ctx.strokeStyle = 'rgba(255,255,255,0)'; //填充颜色

      // ctx.fill(); //填充
      ctx.stroke(); //绘制
    } else {
      ctx.strokeStyle = 'green';
      ctx.lineWidth = 1;
      ctx.setLineDash([0]);
      // // ctx.fillRect(161, 195, 255, 0.4);
      // ctx.strokeStyle = 'green';
      // ctx.setLineDash([5]);

      ctx.strokeRect(roiAreaAry.value[0].x, roiAreaAry.value[0].y, piX - roiAreaAry.value[0].x, piY - roiAreaAry.value[0].y);
      ctx.stroke(); //绘制
    }
  }
}

function clearROI() {
  cameraCtx.value.clearRect(0, 0, cameraCanvas.value.width, cameraCanvas.value.height);
  cameraSaveCtx.value.clearRect(0, 0, cameraSaveCanvas.value.width, cameraSaveCanvas.value.height);
  roiAreaAry.value = [];
  checkArea.value = [];
  invalidArea.value = [];
  // roiAreaType.value = ROIAreaType.Full;
  loadROIImg();
}

function canvasMouseDown(e) {
  let xy = changeToCanvasXY(e.clientX, e.clientY);
  roiMoveBeforeX.value = xy.x;
  roiMoveBeforeY.value = xy.y;
}

function canvasMouseUp(e) {
  if (roiAreaType.value === ROIAreaType.Full) {
    return;
  }
  let piX: number, piY: number;
  let ctx = cameraSaveCtx.value;

  if (roiAreaType.value === ROIAreaType.Polygon || roiAreaType.value === ROIAreaType.InvalidArea) {
    if (roiAreaType.value === ROIAreaType.InvalidArea) {
      cameraSaveCtx.value.strokeStyle = 'gray';
      cameraSaveCtx.value.lineWidth = 1;
      cameraCtx.value.strokeStyle = 'gray';
      cameraCtx.value.lineWidth = 1;
    } else {
      cameraSaveCtx.value.strokeStyle = 'green';
      cameraSaveCtx.value.lineWidth = 1;
      cameraCtx.value.strokeStyle = 'green';
      cameraCtx.value.lineWidth = 1;
      cameraCtx.value.setLineDash([0]);
    }
    if (roiMoveToStart.value > 0 && roiAreaAry.value.length > 0) {
      piX = roiAreaAry.value[0].x;
      piY = roiAreaAry.value[0].y;
      //画点
      makEarc(ctx, piX, piY, GetRandomNum(1, 1), 0, 180, 'rgba(102,168,255,1)');
      roiAreaAry.value.push({ x: piX, y: piY });
      canvasSave(); //保存点线同步到另一个canvas
      saveCanvas(); //生成画布
    } else {
      piX = roiMoveBeforeX.value;
      piY = roiMoveBeforeY.value;
      makEarc(ctx, piX, piY, GetRandomNum(1, 1), 0, 180, 'rgba(102,168,255,1)');
      roiAreaAry.value.push({ x: piX, y: piY });
      canvasSave(); //保存点线同步到另一个canvas
    }
  } else {
    cameraSaveCtx.value.strokeStyle = 'green';
    cameraSaveCtx.value.lineWidth = 1;
    cameraCtx.value.strokeStyle = 'green';
    cameraCtx.value.lineWidth = 1;
    cameraCtx.value.setLineDash([0]);
    if (roiAreaAry.value.length > 0) {
      piX = roiMoveBeforeX.value;
      piY = roiMoveBeforeY.value;
      //画点
      makEarc(ctx, piX, piY, GetRandomNum(1, 1), 0, 180, 'rgba(102,168,255,1)');
      cameraCtx.value.setLineDash([0]);
      roiAreaAry.value.push({ x: piX, y: piY });
      canvasSave(); //保存点线同步到另一个canvas
      cameraSaveCtx.value.clearRect(0, 0, cameraSaveCanvas.value.width, cameraSaveCanvas.value.height);
      roiAreaAry.value = [];
    } else {
      piX = roiMoveBeforeX.value;
      piY = roiMoveBeforeY.value;
      makEarc(ctx, piX, piY, GetRandomNum(2, 2), 0, 180, 'rgba(102,168,255,1)');
      roiAreaAry.value.push({ x: piX, y: piY });
      canvasSave(); //保存点线同步到另一个canvas
    }
  }
}

function checkRoiAngle() {
  let flag = 0;
  for (let i = 0; i < roiAreaAry.value.length; i++) {
    let j = (i + 1) % roiAreaAry.value.length;
    let k = (i + 2) % roiAreaAry.value.length;
    let z = (roiAreaAry.value[j].x - roiAreaAry.value[i].x) * (roiAreaAry.value[k].y - roiAreaAry.value[j].y);
    z -= (roiAreaAry.value[j].y - roiAreaAry.value[i].y) * (roiAreaAry.value[k].x - roiAreaAry.value[j].x);
    if (z < 0) {
      flag |= 1;
    } else if (z > 0) {
      flag |= 2;
    }
    if (flag == 3) {
      return false;
    }
  }
  return true;
}

// 创建斜线图案
function createDiagonalPattern() {
  const patternCanvas = document.createElement('canvas');
  const patternContext = patternCanvas.getContext('2d');
  if (patternContext) {
    patternCanvas.width = 10;
    patternCanvas.height = 10;

    patternContext.beginPath();
    patternContext.moveTo(0, 10);
    patternContext.lineTo(10, 0);
    patternContext.strokeStyle = 'rgba(128,128,128,0.3)';
    patternContext.lineWidth = 1;
    patternContext.stroke();
  }

  return patternCanvas;
}

// 存储已生成的点线
function canvasSave() {
  // cameraCtx.value.clearRect(0, 0, cameraCanvas.value.width, cameraCanvas.value.height);
  // loadROIImg();

  if (roiAreaType.value === ROIAreaType.Polygon) {
    if (roiAreaAry.value.length > 1) {
      // if (!checkRoiAngle()) {
      //   //算法组说可以画凹多边形
      //   roiAreaAry.value = [];
      //   return;
      // }
      cameraCtx.value.beginPath();
      cameraCtx.value.moveTo(roiAreaAry.value[0]?.x, roiAreaAry.value[0]?.y);
      for (let i = 1; i < roiAreaAry.value.length; i++) {
        cameraCtx.value.lineTo(roiAreaAry.value[i].x, roiAreaAry.value[i].y);
        cameraCtx.value.fillStyle = 'rgba(255,255,255,0)'; //填充颜色
        cameraCtx.value.lineWidth = 1;
        cameraCtx.value.setLineDash([0]);
        cameraCtx.value.stroke(); //绘制
      }
      cameraCtx.value.closePath();
    }
  } else if (roiAreaType.value === ROIAreaType.Rect) {
    if (roiAreaAry.value.length > 1) {
      cameraCtx.value.lineWidth = 1;
      cameraCtx.value.fillStyle = 'rgba(255,255,255,0)'; //填充颜色
      cameraCtx.value.strokeRect(roiAreaAry.value[0].x, roiAreaAry.value[0].y, roiAreaAry.value[1].x - roiAreaAry.value[0].x, roiAreaAry.value[1].y - roiAreaAry.value[0].y);
      checkArea.value.push(
        // 每个ROI保存成一个数组, 多个ROI组成二维数组
        [
          xyAtZero(roiAreaAry.value[0].x, roiAreaAry.value[0].y),
          xyAtZero(roiAreaAry.value[1].x, roiAreaAry.value[0].y),
          xyAtZero(roiAreaAry.value[1].x, roiAreaAry.value[1].y),
          xyAtZero(roiAreaAry.value[0].x, roiAreaAry.value[1].y),
          xyAtZero(roiAreaAry.value[0].x, roiAreaAry.value[0].y),
        ]
      );
      changeLineColor();
    }
  } else {
    if (roiAreaAry.value.length > 1) {
      // if(!checkRoiAngle()){ //算法组说可以画凹多边形
      //   roiAreaAry.value = [];
      //   return;
      // }
      cameraCtx.value.beginPath();
      cameraCtx.value.lineWidth = 1;
      cameraCtx.value.moveTo(roiAreaAry.value[0].x, roiAreaAry.value[0].y);
      for (let i = 1; i < roiAreaAry.value.length; i++) {
        cameraCtx.value.lineTo(roiAreaAry.value[i].x, roiAreaAry.value[i].y);
        cameraCtx.value.setLineDash([0]);
        // 设置填充的样式为斜线图案
        const pattern = cameraCtx.value.createPattern(createDiagonalPattern(), 'repeat');
        cameraCtx.value.fillStyle = pattern;
        cameraCtx.value.fill(); //绘制
        cameraCtx.value.stroke();
      }

      cameraCtx.value.closePath();
    }
  }
}

function isChangeColor(start, end) {
  // if ((x === x1 && (x === 0 || x === 1)) || (y === y1 && (y === 0 || y === 1))) {
  //   return true;
  // }
  // return false;
  const pointAry = [0, 1, -0, -1];
  return !!(start && end && ((start[0] === end[0] && pointAry.includes(start[0])) || (start[1] === end[1] && pointAry.includes(start[1]))));
}

//遍历改变颜色
function drawChangeColor(val) {
  for (let j = 0; j < val?.length; j++) {
    for (let i = 0; i < val[j].length; i++) {
      const start = val[j][i];
      const end = val[j][i + 1];
      if (isChangeColor(start, end)) {
        cameraCtx.value.beginPath();
        cameraCtx.value.moveTo(start[0] * cameraCanvas.value.width, start[1] * cameraCanvas.value.height);
        cameraCtx.value.lineTo(end[0] * cameraCanvas.value.width, end[1] * cameraCanvas.value.height);
        cameraCtx.value.strokeStyle = '#00ddff';
        cameraCtx.value.lineWidth = 1;
        cameraCtx.value.setLineDash([]);
        cameraCtx.value.stroke();
        cameraCtx.value.closePath();
      }
    }
  }
}

function changeLineColor() {
  drawChangeColor(checkArea.value);
  drawChangeColor(invalidArea.value);
}

function xyAtZero(x: number, y: number): CheckPoint {
  x = Number.parseFloat((x / cameraCanvas.value.width).toFixed(2));
  y = Number.parseFloat((y / cameraCanvas.value.height).toFixed(2));
  return [x, y];
}

/*生成画布 结束绘画*/
function saveCanvas() {
  const roiArea: CheckArea = [];
  for (let i = 0; i < roiAreaAry.value.length; i++) {
    roiArea.push(xyAtZero(roiAreaAry.value[i].x, roiAreaAry.value[i].y));
  }
  //如果是无效区域,将有效的值清空
  if (roiAreaType.value === ROIAreaType.InvalidArea) {
    // checkArea.value = [];
    invalidArea.value.push(roiArea);
    cameraCtx.value.lineWidth = 1;
  } else {
    checkArea.value.push(roiArea);
    cameraCtx.value.lineWidth = 1;
  }

  cameraSaveCtx.value.clearRect(0, 0, cameraSaveCanvas.value.width, cameraSaveCanvas.value.height);
  cameraCtx.value.closePath(); //结束路径状态，结束当前路径，如果是一个未封闭的图形，会自动将首尾相连封闭起来
  cameraCtx.value.fill(); //填充
  cameraCtx.value.stroke(); //绘制
  roiAreaAry.value = [];
  changeLineColor();
}

// function getPoint() {}

function changeToCanvasXY(x: number, y: number): CanvasPoint {
  return {
    x: x - cameraCanvas.value.getBoundingClientRect().left,
    y: y - cameraCanvas.value.getBoundingClientRect().top,
  };
}

/*canvas生成圆点*/
function GetRandomNum(Min: number, Max: number): number {
  let Range = Max - Min;
  let Rand = Math.random();
  return Min + Math.round(Rand * Range);
}

function makEarc(ctx: CanvasRenderingContext2D, x: number, y: number, r: number, s: number, e: number, color: string) {
  ctx.clearRect(0, 0, cameraSaveCanvas.value.width, cameraSaveCanvas.value.height); //清空画布
  ctx.beginPath();
  ctx.fillStyle = color;
  ctx.arc(x, y, r, s, e);
  ctx.fill();
}

function allImgBtnClick(): void {
  roiAreaType.value = ROIAreaType.Full;
  cameraCtx.value.clearRect(0, 0, cameraCanvas.value.width, cameraCanvas.value.height);
  cameraSaveCtx.value.clearRect(0, 0, cameraSaveCanvas.value.width, cameraSaveCanvas.value.height);
  roiAreaAry.value = [];
  loadROIImg();
  // cameraCtx.value.fillStyle = 'rgba(161,195,255,0.4)'; //填充颜色
  // cameraCtx.value.fillRect(0, 0, cameraCanvas.value.width, cameraCanvas.value.height);
  checkArea.value = [];
  // checkArea.value = [
  //   [
  //     [0, 0],
  //     [1, 0],
  //     [1, 1],
  //     [0, 1],
  //     [0, 0],
  //   ],
  // ];
}

function rectBtnClick(): void {
  clearAllBtnRoi();
  roiAreaType.value = ROIAreaType.Rect;
  // cameraCtx.value.clearRect(0, 0, cameraCanvas.value.width, cameraCanvas.value.height);
  cameraSaveCtx.value.clearRect(0, 0, cameraSaveCanvas.value.width, cameraSaveCanvas.value.height);
  roiAreaAry.value = [];
  // checkArea.value = [];
  // loadROIImg();
}

function invalidClick(): void {
  roiAreaType.value = ROIAreaType.InvalidArea;
  // cameraCtx.value.clearRect(0, 0, cameraCanvas.value.width, cameraCanvas.value.height);
  cameraSaveCtx.value.clearRect(0, 0, cameraSaveCanvas.value.width, cameraSaveCanvas.value.height);
  roiAreaAry.value = [];
  // checkArea.value = [];
  // loadROIImg();
}

function mulBtnClick(): void {
  roiAreaType.value = ROIAreaType.Polygon;
  // cameraCtx.value.clearRect(0, 0, cameraCanvas.value.width, cameraCanvas.value.height);
  cameraSaveCtx.value.clearRect(0, 0, cameraSaveCanvas.value.width, cameraSaveCanvas.value.height);
  roiAreaAry.value = [];
  // checkArea.value = [];
  // loadROIImg();
}

function clearAllBtnRoi(): void {
  if (roiAreaType.value === ROIAreaType.Full) {
    loadROIImg();
    checkArea.value = [];
    invalidArea.value = [];
  }
}

function selectItem(item): void {
  if (noConfig.includes(item.alarmType_dictText)) {
    isTargetSlider.value = true;
    isTargetInput.value = true;
  } else {
    isTargetSlider.value = false;
    isTargetInput.value = false;
  }
  resetFieldsValue();
  try {
    setAlgFieldsValue({ ...item });
  } catch (e) {
    console.error(e);
  }
  selectModelName.value = item.modelName;
  algId.value = item.id;
  isUpdate.value = true;
}

async function algSelectChange(value, option) {
  console.log("@options", option)
  uncoveredSoil.value = "裸土覆盖" === option.key;
  isGrime.value = "跑冒滴漏" === option.key;
  isBelt.value = "皮带松动" === option.key;
  isIntrusion.value = "入侵检测" === option.key;
  formState.selectedLabel = option.key;
  selectModelName.value = formState.algSelected;
  if (noConfig.includes(formState.selectedLabel)) {
    isTargetSlider.value = true;
    isTargetInput.value = true;
  } else {
    isTargetSlider.value = false;
    isTargetInput.value = false;
  }
  algId.value = null;
  isUpdate.value = false;
  resetFieldsValue();
  const res = await getThreshold({ id: selectModelName.value });
  console.log(res, "getThreshold")
  thresholdVal.value = res.threshold;
  targetThresholdVal.value = res.targetThreshold;

  // 添加特殊字段
  iouThresholdVal.value = res.iouThreshold? res.iouThreshold : iouThresholdVal.value;
  sensitivityVal.value = res.sensitivity? res.sensitivity : sensitivityVal.value;
  timeSpaceVal.value = res.timeSpace? res.timeSpace : timeSpaceVal.value;
  hitCountVal.value = res.hitCount? res.hitCount : hitCountVal.value;
}

function casChange(val, opt) {
  if (val && opt[opt.length - 1].type === 'camera') {
    formState.algSelected = '';
    selectModelName.value = '';
    casCameraName.value = val[val.length - 1];
    cameraName.value = opt[opt.length - 1].title;
    cameraId.value = opt[opt.length - 1].key;
    loadAlgorithmList(casCameraName.value);
  } else {
    casCameraName.value = '';
  }
}

async function loadAlgorithmList(item) {
  const alg = await list({ belongTo: item });
  algList.value = alg.records;
  await loadCameraImg();
}

async function loadCameraImg() {
  if (cameraId.value) {
    loading.value = true;
    const dev = await imgInfo({ cameraId: cameraId.value });
    if (dev) {
      loading.value = false;
      roiImgSrc.value = '/kyscene' + dev.imgSrc + '?' + dev.ip;
    }
  }
}

const delAlgItem = debounce(async (item) => {
  await deleteOne({ id: item.id }, handleSuccess);
  emit('success');
}, 100);

async function handleSuccess() {
  await loadAlgorithmList(cameraId.value);
}

function isDisable(item) {
  console.log('@disable item', item.value, item.label)
  if (item.label === '裸土覆盖') return true
  return algList.value.length > 0 && isAlgHave(item.value);
}

function isAlgHave(key) {
  for (const v of algList.value) {
    if (v.modelName === key) {
      return true;
    }
  }
  return false;
}

async function initDictData() {
  //根据字典Code, 初始化字典数组
  const dictData = await initDictOptions('ky_category,name,id,category="0" and pid!="0"');
  algOptions.value = dictData.reduce((prev, next) => {
    if (next) {
      const value = next['value'];
      prev.push({
        label: next['text'] || next['label'],
        value: value,
        ...omit(next, ['text', 'value']),
      });
    }
    return prev;
  }, []);

  // 开启人脸算法后，只有人脸算法
  if (enableFaceAlgorithm.value) {
    algOptions.value = algOptions.value.filter(item => item.label.includes('人脸识别'));
  } else {
    algOptions.value = algOptions.value.filter(item => !item.label.includes('人脸识别'));
  }
  // console.log('algOptions', algOptions.value)
}

async function dataSubmit() {
  try {
    let values = saveFieldsValue();
    if (isGrime || isBelt || isIntrusion) {
      Object.assign(values, {frameInterval: frameInterval.value});
    }
    setModalProps({ confirmLoading: true });
    if (!values) {
      return;
    } else {
      //提交表单
      await saveOrUpdate(values, isUpdate.value);
    }

    resetFieldsValue();
    formState.algSelected = '';
    selectModelName.value = '';
    // isUpdate.value =true;
    //关闭弹窗
    // closeModal();
    //刷新列表
    emit('success');
    if (cameraId.value) {
      await loadAlgorithmList(cameraId.value);
    }
    await saveConfig({ cameraId: cameraId.value });
  } finally {
    setModalProps({ confirmLoading: false });
  }
}

//表单提交事件
const handleSubmit = debounce(async (v) => {
  console.log("roiAreaType:" + roiAreaType.value)
  if (uncoveredSoil.value) { // 裸土覆盖必须设置ROI
    const hasROI = roiAreaType.value === 1 || roiAreaType.value === 2
    if (!hasROI) {
      createMessage.error('请绘制ROI区域')
      return
    }
    const isDrawROI = checkArea.value.length > 0
    if (!isDrawROI) {
      createMessage.error('请绘制ROI区域')
      return
    }
  }
  if (isGrime.value || isBelt.value || isIntrusion.value) {
    if (checkArea.value.length <= 0) {
      createMessage.error('请绘制ROI区域')
      return
    }
    if (checkArea.value.length > 1) {
      createMessage.error('只能绘制一个区域, 当前绘制区域数量 ' + checkArea.value.length)
      return
    }
  }

  if (isBelt.value) {
    const area1 = checkArea.value[0]
    if (area1.length !== 5) {
      createMessage.error('请绘制4点式区域')
      return
    }
  }

  formState.checked = false;
  document.getElementById('btn')!.click();
}, 50);

function onFinish(val) {
  dataSubmit();
}

function resetFieldsValue() {
  // selectModelName.value = '';
  // formState.algSelected = '';
  formState.taskRemark = '';
  checkArea.value = [];
  invalidArea.value = [];
  checkDate.value = [];
  checkTime.value = '';
  iouThresholdVal.value = 70;
  thresholdVal.value = props.threval;
  targetThresholdVal.value = props.targetThreval;
  hitCountVal.value = 5;
  alarmCycleVal.value = 600;
  alarmLevel.value = '1';
  sensitivityVal.value = 2;
  timeSpaceVal.value = 4;
  loadROIImg();
  allImgBtnClick();
}

//浮点数提示
const checkFloatValue = (val, message) => {
  if (isFloat(val)) {
    createMessage.error(message);
    return true;
  }
  return false;
};

function saveFieldsValue() {
  //未选算法情况
  if (!formState.algSelected) {
    selectModelName.value = '';
    thresholdVal.value = 0;
  }

  if (checkFloatValue(iouThresholdVal.value, '跟踪框交并比不能输入小数')) {
    return;
  }
  if (checkFloatValue(hitCountVal.value, '连续命中周期不能输入小数')) {
    return;
  }

  if (checkFloatValue(sensitivityVal.value, '误报过滤周期不能输入小数')) {
    return;
  }
  if (checkFloatValue(timeSpaceVal.value, '误报过滤周期不能输入小数')) {
    return;
  }
  if (checkFloatValue(alarmCycleVal.value, '事件推送间隔不能输入小数')) {
    return;
  }
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

  return {
    id: algId.value ? algId.value : null,
    modelName: selectModelName.value,
    taskRemark: formState.taskRemark,
    belongTo: cameraId.value,
    checkArea: JSON.stringify(checkArea.value),
    checkDate: JSON.stringify(checkDate.value),
    checkTime: checkTime.value,
    iouThreshold: iouThresholdVal.value,
    threshold: thresholdVal.value,
    targetThreshold: targetThresholdVal.value,
    hitCount: hitCountVal.value,
    alarmCycle: alarmCycleVal.value,
    alarmLevel: alarmLevel.value,
    alarmType: selectModelName.value,
    sensitivity: sensitivityVal.value,
    timeSpace: timeSpaceVal.value,
    invalidArea: JSON.stringify(invalidArea.value),
  };
}

function dateFormat(time: Date): string {
  let date = new Date(time);
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const hours = date.getHours();
  const min = date.getMinutes();
  const sec = date.getSeconds();
  return (
    date.getFullYear() +
    '-' +
    (month < 10 ? '0' + month : month) +
    '-' +
    (day < 10 ? '0' + day : day) +
    ' ' +
    (hours < 10 ? '0' + hours : hours) +
    ':' +
    (min < 10 ? '0' + min : min) +
    ':' +
    (sec < 10 ? '0' + sec : sec)
  );
}

function handleCancel() {
  formState.algSelected = '';
  selectModelName.value = '';
  closeModal();
  formState.checked = false;
}

onMounted(() => {
  setInterval(() => {
    nowDate.value = new Date();
  }, 1000);
  if (cameraId.value) {
    loadAlgorithmList(cameraId.value);
  }
});
</script>
<style lang="less" scoped>
.ant-modal-content {
  background-color: var(--background-Color);
  border-radius: 4px;
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
</style>
<style lang="less" scoped>
/** 时间和数字输入框样式 */
:deep(.ant-input-number) {
  width: 100%;
}

:deep(.ant-calendar-picker) {
  width: 100%;
}
.dot {
  width: 10px;
  height: 10px;
  background-color: var(--adopt-text-color);
  margin-left: 12px;
  display: inline-block;
  border-radius: 5px;
}
.name {
  font-family: Source Han Sans CN;
  font-weight: 600;
  color: var(--secondary-text-color);
  font-size: 13px;
  margin-left: 24px;
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

.mark {
  width: 240px;
  height: 28px;

  border: 1px solid;
  border-color: var(--weak-split-line-color);
  border-radius: 4px;
}
.roi {
  margin-left: 24px;
}

.loading {
  width: 751px;
  height: 423px;
  background-color: var(--headerline-text-color);
  border-radius: 4px;
  display: flex;
  color: #fff;
  justify-content: center;
  align-items: center;
}
.loading-icon {
  background: url('/@/assets/svg/icon/loading_onblack copy.gif');
  width: 48px;
  height: 48px;
  display: inline-block;
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
}
.alg-div:hover {
  background: rgba(112, 137, 224, 0.2);
  color: var(--primary-Color);
  font-size: 13px;
}
.alg-btn:active {
  background: rgba(112, 137, 224, 0.2);
  color: var(--primary-Color);
  font-size: 13px;
}
.wrap {
  text-align: -webkit-center;
  height: 430px;
}
.tips {
  font-family: Source Han Sans CN;
  color: var(--tips-text-color);
  font-size: 11px;
  margin-left: -40%;

  &-icon {
    background: url('/@/assets/svg/icon/info_icon_tips.svg');
    width: 16px;
    height: 16px;
    display: inline-block;
    margin-right: 8px;
  }
}
.alg-div:hover {
  // border: 1px solid var(--primary-Color);
  color: var(--primary-Color);
}
.alg-div:hover * {
  color: var(--primary-Color);
}

.alg-div-active {
  margin-top: 8px;
  width: 136px;
  height: 25px;
  background: rgba(112, 137, 224, 0.2);
  color: var(--primary-Color);
  font-size: 13px;
  display: flex;
  align-items: center;
}
.alg-div-active * {
  color: var(--primary-Color);
}

.roi-btn {
  margin-left: 10px;
  border-radius: 6px;
}

.roi-btn-active {
  color: var(--primary-Color);
  border-color: var(--primary-Color);
}

.roi-btn-poly-active img {
  content: url('/@/assets/svg/icon/algo_btn_roipolygon_actived.svg');
}

.roi-btn-rect-active img {
  content: url('/@/assets/svg/icon/algo_btn_roirectangle_actived.svg');
}
.roi-btn-invalid-active img {
  content: url('/@/assets/svg/icon/algo_btn_mask_actived.svg');
}
.reset-btn {
  float: right;
  border-radius: 6px;
  border: 0;
}
.reset-btn:hover img {
  content: url('/@/assets/svg/icon/editor_btn_resetxs_default_hover.svg');
}

canvas {
  position: absolute;
  left: 14%;
}
.content {
  // overflow: auto;
}

.control-time {
  margin: 16px 0 0 0;
}
.head {
  font-weight: 600;
}
.params {
  margin-top: 10px;
}
.switch {
  margin-left: 8px;
}
.target-box {
  margin: 8px 0 0 0;
}
.divider {
  width: 1px;
  height: 100%;
  border-left: 1px solid var(--weak-split-line-color);
  margin: 0 0 0 8px;
}
</style>
