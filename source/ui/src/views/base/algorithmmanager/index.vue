<template>
  <a-row :class="['p-4', `${prefixCls}--box`]" type="flex" :gutter="10">
    <a-col :xl="5" :lg="24" :md="24" style="margin-bottom: 10px; background-color: var(--background-Color)">
      <div>
        <a-row style="margin-top: 24px; margin-bottom: 15px">
          <a-col><span class="camera-title">相机列表</span></a-col>
        </a-row>
        <a-row>
          <a-col :xl="11">
            <div class="div-status">
              <img alt="" :src="treeCameraIcon" />
              <span>在线相机 </span><span> {{ cameraNum.on }}/{{ cameraNum.total }}</span>
            </div>
          </a-col>
          <a-col :xl="1">
            <a-divider type="vertical" class="divider" />
          </a-col>
          <a-col :xl="12">
            <div class="div-status">
              <img style="float: left" alt="" :src="treeAlgIcon" />
              <span>算法个数授权</span><span> {{ cameraNum.algOn }}/{{ cameraNum.algAll }}</span>
            </div>
          </a-col>
        </a-row>
        <div style="width: calc(100% - 12px)">
          <tab-control class="tab-control" :title="['设备', '分组']" @click-item="clickItem" />
        </div>
      </div>
      <CameraTree ref="cameraT" @select="onHandleSelect" @rootTreeData="onRootTreeData" />
    </a-col>
    <a-col :xl="19" :lg="24" :md="24" style="margin-bottom: 10px">
      <a-card :bordered="false" style="height: 100%">
        <template #title>
          <img style="float: left; margin-top: 6px; margin-right: 4px" :src="treeCameraIcon" alt="" /><label>{{ selectTitle }}</label>
        </template>
        <div class="operation">
          <a-popover placement="bottom" class="popover">
            <template #content v-if="disabled">
              <div class="tips">
                <i class="tips-icon"></i>
                <span>可与相机绑定的算法授权数量已达上限<br /> </span>
              </div>
              <p class="paragph">1.请联系管理员申请更多授权；<br />2.删除已有相机的算法，以在本相机上配置算法；</p>
            </template>
            <div class="operation">
              <a-switch
                style="height: 24px; border-radius: 3px"
                v-model:checked="algSwitch"
                :un-checked-children="switchOFF"
                :checked-children="switchOn"
                v-show="selectTitle !== '' && selectTitle !== '请选择相机'"
                @change="switchChange"
                v-auth="'alg:switch'"
                :disabled="disabled"
              />
              <p class="divider" v-show="selectTitle !== '' && selectTitle !== '请选择相机'"></p>
              <div class="add-alg">
                <primary-button @click="restartPyt"  label="重启算法" >
                </primary-button>
              </div>
              <div class="add-alg">
                <primary-button @click="handleAdd" v-auth="'alg:add'" label="新建算法" :disabled="disabled">
                  <i class="add-alg-icon"></i>
                </primary-button>
              </div>
            </div>
          </a-popover>

          <!-- <a-button
            style="margin-left: 20px; width: 94px; height: 24px; background-color: #738ce6; border-radius: 4px; padding-top: 1px; padding-left: 10px; vertical-align: middle"
            type="primary"
            @click="handleAdd"
            v-auth="'alg:add'"
          >
            <img alt="" :src="addAlgIcon" style="float: left; margin-right: 4px; margin-top: 4px" />
            <span>新建算法</span>
          </a-button> -->
        </div>

        <template v-if="haveData" key="noData">
          <div>
            <a-card :bordered="false">
              <span>未查询到关联算法</span>
            </a-card>
          </div>
        </template>
        <template v-else>
          <div style="margin-top: 6px">
            <a-row v-for="(item, index) of algorithmList" :key="index">
              <a-col>
                <a-divider />
                <div style="margin-top: 3%">
                  <div style="margin-left: 4%">
                    <img src="../../../assets/svg/icon/img_algo.svg" class="dev-img" />
                  </div>
                  <div style="margin-left: 4%; float: left">
                    <div>
                      {{ item.modelName_dictText }}
                    </div>
                    <div class="div-alg-status">
                      <img style="width: 12px; height: 12px; margin-top: 5px; margin-right: 6px" alt="" :src="item.runStatus === 'N' ? pauseIcon : runIcon" />
                      <label>
                        {{ item.runStatus === 'N' ? '未执行' : '执行中' }}
                      </label>
                    </div>
                    <!--                    <div>-->
                    <!--                      <label>生效日期 </label> {{ item.checkDate ? item.checkDate.replace(',', ' ~ ') : '未配置' }}-->
                    <!--                    </div>-->
                  </div>
                  <div class="operation">
                    <a-button type="link" class="preview" @click="goView">预览</a-button>
                    <a-button type="link" class="see" @click="handleDetail(item)" v-auth="'alg:detail'">查看</a-button>
                    <a-button type="link" class="edit" @click="handleEdit(item)" v-auth="'alg:edit'">编辑</a-button>
                    <a-button type="link" class="delete" @click="deleteOneAlg(item)" danger v-auth="'alg:del'">删除</a-button>
                  </div>
                </div>
              </a-col>
            </a-row>
            <a-divider />
          </div>
        </template>
      </a-card>
    </a-col>
  </a-row>
  <AlgorithmModal @register="registerModal" @success="handleSuccess" :threval="mainState.threshold" :target-threval="mainState.targetThreshold" />
</template>

<script lang="ts" setup>
  import CameraTree from './components/CameraTree.vue';
  import PrimaryButton from '/@/components/BaseButton/PrimaryButton.vue';

  import { Col, Row, Popover } from 'ant-design-vue';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { computed, onMounted, provide, ref, reactive } from 'vue';
  import { cameraNums, list, setRunStatus, getRunStatus, deleteOne, algLicenseNum, getThreshold,restarstPythone} from './AlgorithmManager.api';
  import AlgorithmModal from './components/AlgorithmModal.vue';
  import { useModal } from '/@/components/Modal';
  import TabControl from '/@/components/TabControl/TabControl.vue';
  import treeCameraIcon from '/@/assets/svg/icon/tree_icon_camera.svg';
  import treeAlgIcon from '/@/assets/svg/icon/tree_icon_algorithm.svg';
  import runIcon from '/@/assets/svg/icon/algo_icon_running.svg';
  import pauseIcon from '/@/assets/svg/icon/algo_icon_pause.svg';

  import { useGo } from '/@/hooks/web/usePage';
  import { licenseObj } from '../algorithmmanager/AlgorithmManager.data';

  const { prefixCls } = useDesign('depart-manage');
  provide('prefixCls', prefixCls);

  const [registerModal, { openModal }] = useModal();

  const ARow = Row;
  const ACol = Col;
  const APopover = Popover;
  const go = useGo();
  const rootTreeData = ref<any[]>([]);
  const cameraT = ref();
  const algorithmList = ref([]);
  const selectTitle = ref('请选择相机');
  const selectId = ref('');
  const switchOn = ref('算法开启');
  const switchOFF = ref('算法关闭');
  const haveData = computed(() => algorithmList.value.length === 0);
  const algSwitch = ref<boolean>(false);
  const disabled = ref<boolean>(false);
  const canUseLicense = ref<number>(0);
  //授权数
  const mainState = reactive({
    licenseNum: {} as licenseObj,
    threshold: 0 as number,
    visible: false as boolean,
    targetThreshold: 0 as number,
  });
  const cameraNum = ref({
    on: 0,
    total: 0,
    algOn: 0,
    algAll: 0,
  });

  const clickItem = (index: number) => {
    if (index === 1) {
      cameraT.value.loClick();
    } else {
      cameraT.value.devClick();
    }
  };

  function handleAdd() {
    openModal(true, { algList: algorithmList.value, cameraName: selectTitle.value, cameraId: selectId.value, isUpdate: false, showFooter: true });
  }

  async function handleEdit(record) {
    openModal(true, { algList: algorithmList.value, cameraName: selectTitle.value, cameraId: selectId.value, record: record, isUpdate: true, showFooter: true });
    const res = await getThreshold({ id: record.alarmType });
    mainState.threshold = res.threshold;
    mainState.targetThreshold = res.targetThreshold;
  }

  function handleDetail(record) {
    openModal(true, { algList: algorithmList.value, cameraName: selectTitle.value, cameraId: selectId.value, record: record, isUpdate: true, showFooter: false });
  }

  function switchChange() {
    setRunStatus({ id: selectId.value, status: algSwitch.value ? 'Y' : 'N' }, statusSetSuccess);
  }
  function restartPyt() {
    restarstPythone()
  }
  async function statusSetSuccess() {
    let ret = await getRunStatus({ id: selectId.value });
    algSwitch.value = ret.runStatus === 'Y';
  }

  async function onHandleSelect(item, itemId) {
    selectTitle.value = item;
    selectId.value = itemId;
    await statusSetSuccess();
    await loadAlgorithmList(itemId);
    mainState.licenseNum = await algLicenseNum({ id: itemId });
    canUseLicense.value = mainState.licenseNum.canUseNum;
    if (canUseLicense.value === 0 && !algorithmList.value.alarmType && selectTitle.value !== '请选择相机') {
      algSwitch.value = false;
      disabled.value = true;
    }
  }

  function onRootTreeData(data) {
    rootTreeData.value = data;
  }

  async function loadAlgorithmList(item) {
    const algList = await list({ belongTo: item });
    algorithmList.value = algList.records;
  }

  function handleSuccess() {
    if (selectId.value) {
      loadAlgorithmList(selectId.value);
    }
  }

  async function getCameraNums() {
    cameraNum.value = await cameraNums();
  }

  async function deleteOneAlg(item) {
    await deleteOne({ id: item.id }, handleSuccess);
  }

  function goView() {
    go('/viewPreview/view');
  }

  onMounted(() => {
    // console.log(algorithmList.value);
    getCameraNums();
  });
</script>
<style lang="less" scoped>
  .popover {
    border-radius: 2px;
  }
  .popover > .ant-popover-inner-content {
    background: #fdf7f7;
  }
  .tips {
    display: flex;
    height: 24px;
    align-items: center;
  }
  .tips-icon {
    width: 16px;
    height: 16px;
    background: url('/@/assets/svg/icon/info_icon_tips_red.svg');
    display: inline-block;
    margin: 0 4px 0 12px;
  }
  .paragph {
    padding: 12px 0 0 32px;
    color: var(--tips-text-color);
    font-size: 13px;
  }
</style>
<style lang="less" scoped>
  .div-status {
    width: 100%;
    text-align: center;
    //background-color: #d9d9d9;
    margin-bottom: 6px;
    margin-top: 4px;
    border-radius: 8px;
    display: flex;
    justify-content: center;
    flex-wrap: wrap;
  }

  .div-status span {
    margin-left: 4px;
    font-family: Source Han Sans CN;
    color: #474e66;
    font-size: 11px;
  }
  .divider {
    left: -8px;
  }

  .btn {
    width: 50%;
    border-radius: 4px;
  }

  .camera-title {
    margin-left: 24px;
    font-family: Source Han Sans CN;
    font-weight: 550;
    color: var(--secondary-text-color);
    font-size: 15px;
  }

  .operation {
    display: flex;
  }
  .divider {
    width: 1px;
    height: 12px;
    background-color: rgba(211, 216, 230, 0.9);
    margin-left: 12px;

    margin-top: 4px;
  }
  .add-alg {
    margin-left: 12px;
  }
  .add-alg-icon {
    background: url('/@/assets/svg/icon/editor_btn_addalgo_default.svg');
    width: 14px;
    height: 14px;
    display: inline-block;
  }
  .dev-img {
    padding: 1px;
    float: left;
    margin-top: 10px;
  }
  .tab-control {
    width: calc(100% - 36px);
    // margin-left: 0;
    // border-color: #8b949e;
    height: 100%;
    /deep/.active {
      width: 50% !important;
    }
  }
  /deep/.tab-control-item {
    width: 50% !important;
  }

  .div-alg-status {
    width: 80px;
    height: 24px;
    background-color: #edeff6;
    border-radius: 12px;
    display: flex;
    justify-content: center;
    margin-top: 10px;
  }

  .delete {
    width: 42px;
    height: 24px;
    text-align: center;
    background-color: transparent;
    font-family: Source Han Sans CN;
    color: var(--serious-text-color);
    font-size: 13px;

    &:hover {
      background-color: rgba(204, 102, 102, 0.2);
      border-radius: 4px;
    }
  }
  .operation {
    display: flex;
    justify-content: flex-end;
  }
  .see,
  .preview,
  .edit,
  .delete {
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .see,
  .preview,
  .edit {
    width: 50px;
    height: 24px;
    font-family: Source Han Sans CN;
    color: var(--icon-Color);
    font-size: 13px;
    text-align: center;
    background-color: transparent;

    &:hover {
      background-color: var(--fill-three-color);
      border-radius: 4px;
    }
  }
</style>
