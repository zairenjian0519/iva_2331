<template>
  <div class="polling-container">
    <div class="polling-management">
      <div class="title">轮询管理</div>
      <div class="add-group">
        <primary-button label="新建轮询组" @click="handleShowModal">
          <i class="add-icon"></i>
        </primary-button>
      </div>
      <!-- <template v-if="mainState.group.length == 0">暂无组</template> -->
      <!-- <template> -->
      <div class="group-container" v-if="mainState.group">
        <div class="list" v-for="(item, index) in mainState.group" :key="index">
          <div class="group">
            <!-- checked="isChecked(Array.isArray(item.algorithmList) && Array.isArray(item.cameraList))" -->
            <div class="switch">
              <Switch :checked="item.runStatus === 1" @change="handleChecked(item)" />
              <!-- <BaseSwitch :checked="Array.isArray(item.algorithmList) && item.algorithmList.length > 0" :item="item" @handle-change="handleChecked" /> -->
              <span class="name">{{ item.groupName }}</span>
            </div>
            <div class="select">
              <CameraSelect
                @get-selected-group="handleGetGroup"
                @get-select-camera="getSelectCamera"
                v-model:checkedKeys="item.cameraIds"
                v-model:selectedKeys="item.cameraIds"
                :selectValue="item.cameraList"
                :group-id="item.id"
              />
              <div class="operation">
                <button class="edit-button" @click.stop="handleEditGroup(item)">编辑</button>
                <button class="setting" @click.stop="handlePollingSetting(item)">轮询配置</button>
                <button class="delete" @click.stop="handleDeleteGroup(item)">删除</button>
              </div>
            </div>
          </div>
          <div v-for="(ele, ind) in item.algorithmList" :key="ind">
            <div class="alg-item" :class="{ last: ind === item.algorithmList.length - 1 }">
              <div>
                <i class="alg-item-icon"></i>
                <span class="alg-item-name">{{ ele.algorithmName }}</span>
              </div>
              <div class="operation">
                <button class="setting" @click="handleROISetting(ele, item)">配置ROI</button>
                <button class="delete" @click="handleDeleteAlg(item, ele)">删除</button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <!-- </template> -->

      <div class="bottom">
        <!--      <div class="button">-->
        <!--        <primary-button label="配置下发" @click="handleIssued" class="issue" />-->
        <!--      </div>-->

        <div class="tips">
          <i class="tips-icon"></i>
          <!--        <div class="tips-text">更新配置需使用按钮下发，上次下发时间{{ mainState.currentDate }}</div>-->
          <div class="tips-text">更新配置自动下发文件，上次下发时间{{ mainState.currentDate }}</div>
        </div>
      </div>
    </div>
    <pollingModal
      v-model:visible="mainState.isPollingModal"
      :algList="mainState.algList"
      :ele="mainState.ele"
      :algOptions="mainState.selectAlg"
      :title="mainState.pollingTitle"
      :threval="mainState.threshold"
      :ok-text="mainState.okText"
      @handleOk="handlePollingOk"
      @handleCancel="handlePollingCancel"
      @handleDelete="handleDelete"
    />
    <ROISettingModal
      :title="mainState.ROITitle + '(' + mainState.algItem.algorithmName + ')'"
      :ok-text="mainState.okText"
      v-model:visible="mainState.showRoiSettingModal"
      :selected-keys="mainState.roiKeys"
      :auto-expand-parent="mainState.isExpand"
      :camera-id="mainState.cameraId"
      :label="['超脑', '区域']"
      :tree-data="mainState.selectedGroup"
      :sum="mainState.algGroup.cameraIds?.length"
      :camera-name="mainState.cameraName"
      @handle-select="handleSelect"
      @handle-click-item="handleClickItem"
      @handleOk="handleSettingOk"
      @handleCancel="handleSettingCancel"
    >
      <a-row>
        <a-col v-if="mainState.cameraId">
          <a-row>
            <a-col span="24" class="container" :style="{'padding': '0 15px'}">
              <a-button v-if="!uncoveredSoil" class="full" :class="roiAreaType === ROIAreaType.Full ? 'roi-btn roi-btn-active roi-btn-poly-active' : 'roi-btn'" size="small" @click="allImgBtnClick"
                ><img style="float: left; margin-top: 4px; margin-right: 4px" :src="allBtnIcon" alt="" /> 全屏</a-button
              >
              <a-button :class="roiAreaType === ROIAreaType.Rect ? 'roi-btn roi-btn-active roi-btn-poly-active' : 'roi-btn'" size="small" @click="rectBtnClick"
                ><img style="float: left; margin-top: 4px; margin-right: 4px" :src="rectangleBtnIcon" alt="" /> 矩形</a-button
              >
              <a-button :class="roiAreaType === ROIAreaType.Polygon ? 'roi-btn roi-btn-active roi-btn-poly-active' : 'roi-btn'" size="small" @click="mulBtnClick"
                ><img style="float: left; margin-top: 4px; margin-right: 4px" :src="polygonBtnIcon" alt="" />多边形</a-button
              >
<!--              <span class="divider"></span>-->
              <a-button v-if="!uncoveredSoil" :class="roiAreaType === ROIAreaType.InvalidArea ? 'roi-btn roi-btn-active roi-btn-invalid-active' : 'roi-btn'" size="small" @click="invalidClick"
                ><img style="float: left; margin-top: 4px; margin-right: 4px" :src="invalidBtnIcon" alt="" />无效区域</a-button
              >
              <a-button class="reset-btn" size="small" @click="clearROI"><img style="float: left; margin-top: 4px; margin-right: 4px" :src="resetBtnIcon" alt="" /> 清空屏幕</a-button>
            </a-col>
          </a-row>
          <a-row style="margin-top: 10px; margin-bottom: 10px">
            <a-col span="24" class="wrap">
              <p class="tips">
                <i class="tips-icon"></i>
                <span class="tips-text">当前最小识别像素 91×54</span>
              </p>
              <template v-if="mainState.loading">
                <div class="loading">
                  <i class="loading-icon"></i>
                  <div>图像加载中，请稍后…</div>
                </div>
              </template>
              <template v-else>
                <div class="operation-area">
                  <div class="prev" @click="handleGoPrev">
                    <span class="prev-icon"></span>
                  </div>
                  <img ref="cameraImg" :src="mainState.imgSrc" style="display: none" class="image" @load="loadROIImg" />
                  <canvas ref="cameraCanvas" width="752" height="423" style="border: 1px solid"> 您的浏览器不支持 HTML5 canvas 标签。 </canvas>
                  <canvas
                    ref="cameraSaveCanvas"
                    width="752"
                    height="423"
                    class="canvas-save"
                    @mousedown="canvasMouseDown($event)"
                    @mouseup="canvasMouseUp($event)"
                    @mousemove="canvasMouseMove($event)"
                  ></canvas>

                  <div class="next" @click="handleGoNext">
                    <span class="next-icon"></span>
                  </div>
                </div>
              </template>
            </a-col>
          </a-row>
        </a-col>
        <a-col v-else span="24" style="text-align: -webkit-center; margin-bottom: 10px">
          <div style="background-color: grey; text-align: center; width: 80%; height: 400px; padding-top: 200px">
            <label>暂未选定相机</label>
          </div>
        </a-col>
      </a-row>
    </ROISettingModal>

    <camera-group-modal
      v-model:visible="mainState.isShowModal"
      :title="mainState.title"
      :ok-text="mainState.title"
      :cancel-text="mainState.cancelText"
      @handleOk="handleOk"
      @handleCancel="handleCancel"
    >
      <a-row>
        <a-col :span="14">
          <div class="group-name">轮询组名称</div>
          <a-input v-model:value="mainState.inputValue" class="input-box" />
        </a-col>
        <a-col :span="8" class="polling-gap-item">
          <base-tooltip tooltip-title="定期执行轮询操作的时间间隔，每一轮询组共用同一轮询检测间隔">
            <span class="title">轮询检测间隔</span>
            <i class="tips-icon"></i>
          </base-tooltip>
          <div class="polling-gap">
            <a-input-number v-model:value="mainState.pollingGap" :min="1" class="polling-gap-input" />
            <span class="unit">s</span>
          </div>
        </a-col>
      </a-row>

      <div class="camera-select">
        <base-tooltip tooltip-title="轮询组中的所有相机将应用相同的配置">
          <span>选择相机</span>
          <i class="tips-icon"></i>
        </base-tooltip>
      </div>
      <camera-select
        :isPlaceholder="true"
        @get-select-camera="getSelectCamera"
        :selectValue="mainState.selectedCamera"
        @handle-check="handleCheck"
        @handle-select="handleSelectTree"
        @handle-expand="handleExpand"
      />
    </camera-group-modal>
    <camera-group-modal
      v-model:visible="mainState.isShowEditModal"
      :title="mainState.editTitle"
      :ok-text="mainState.editTitle"
      :cancel-text="mainState.cancelText"
      @handleOk="handleEditOk"
      @handleCancel="handleEditCancel"
    >
      <a-row>
        <a-col :span="14">
          <div class="group-name">轮询组名称</div>
          <a-input v-model:value="groupName" class="input-box" />
        </a-col>
        <a-col :span="8" class="polling-gap-item">
          <base-tooltip tooltip-title="定期执行轮询操作的时间间隔，每一轮询组共用同一轮询检测间隔">
            <span class="title">轮询检测间隔</span>
            <i class="tips-icon"></i>
          </base-tooltip>
          <div class="polling-gap">
            <a-input-number v-model:value="pollingPeriod" :min="1" class="polling-gap-input" />
            <span class="unit">s</span>
          </div>
        </a-col>
      </a-row>
    </camera-group-modal>
  </div>
</template>
<script lang="ts" setup>
  import { onMounted, reactive, ref, computed } from 'vue';
  import { message, Switch } from 'ant-design-vue';
  import PrimaryButton from '/@/components/BaseButton/PrimaryButton.vue';
  import BaseTooltip from '/@/components/BaseTooltip/BaseTooltip.vue';
  import CameraSelect from './components/CameraSelect.vue';
  import pollingModal from './components/pollingModal.vue';
  import CameraGroupModal from './components/CameraGroupModal.vue';
  import ROISettingModal from './components/ROISettingModal.vue';

  import { initDictOptions } from '/@/utils/dict';
  import { list, addGroup, imgInfo, deleteAlg, editGroup, deleteGroup, changeStatus, editROI, editCamera, saveGroupAll, getROI, getDevIpList } from './CameraGroup.api';
  import { groupList, selectCamera } from './CameraGroup.data';
  import resetBtnIcon from '/@/assets/svg/icon/editor_btn_resetxs_default.svg';
  import allBtnIcon from '/@/assets/svg/icon/algo_btn_roifullscreen_default.svg';
  import polygonBtnIcon from '/@/assets/svg/icon/algo_btn_roipolygon_default.svg';
  import invalidBtnIcon from '/@/assets/svg/icon/algo_btn_mask_default.svg';
  import rectangleBtnIcon from '/@/assets/svg/icon/algo_btn_roirectangle_default.svg';
  import { CanvasArea, CheckArea, ROIAreaType, allAlgInfo, CheckPoint, roiType, IRawCameraTree } from './CameraGroup.data';
  import { cameraTreeData } from '/@/components/BaseSearch/BaseSearch.api';
  import axios from 'axios';
import { flatMapDeep } from 'lodash-es';
import { nextTick } from 'process';

  interface TreeNodeData {
    title: string;
    key: string;
    type: string;
    parentNode: string;
    haveAlarm: boolean;
    parentKey: string;
    children?: TreeNodeData[];
    slots: { icon: string };
    runStatus: string;
    cameraCount: number;
  }
  interface algorithmList {
    algorithmId: string;
    algorithmName: string;
  }

  const uncoveredSoil = ref(false);

  const mainState = reactive({
    group: [] as groupList[],
    groupId: '' as string,
    algGroup: [] as groupList[],
    groupInfo: {} as groupList,
    //显示弹窗
    isShowModal: false,
    title: '新建',
    cancelText: '取消',
    isShowEditModal: false,
    editTitle: '编辑',
    ROITitle: '配置ROI',
    pollingTitle: '轮询配置',
    inputValue: '' as string,
    selectedCamera: [] as selectCamera[],
    tempCamera: [] as selectCamera[],
    selectedGroup: [] as TreeNodeData[],
    cameraIds: [] as string[],

    showRoiSettingModal: false as boolean,
    okText: '完成',
    algItem: {} as algorithmList,
    isExpand: true as boolean,
    activeKey: 0 as number,
    newTree: [] as TreeNodeData[],
    imgSrc: '' as string,
    loading: true as boolean,
    cameraId: '' as string,
    deleteItem: {},
    cameraName: '' as string,
    //轮询配置是否显示
    isPollingModal: false as boolean,
    algList: [] as algorithmList[],
    selectAlg: [] as allAlgInfo[],
    ele: [] as groupList[],
    treeValue: { locationTree: [] as IRawCameraTree[], deviceTree: [] as IRawCameraTree[] },
    tempValue: { locationTree: [] as IRawCameraTree[], deviceTree: [] as IRawCameraTree[] },
    tempTree: [] as TreeNodeData[],
    //点的是哪个相机的图
    currentIndex: 0 as number,
    //roi区域
    roiInfo: {} as roiType,
    currentDate: '' as string,
    cameraTree: { locationTree: [] as TreeNodeData[], deviceTree: [] as TreeNodeData[] },
    pollingGap: '',
    hostUrl: window.location.host,
    hostIp: '',
    selectedKeys: [] as string[],
    checkedKeys: [] as string[],
    expandKeys: [] as string[],
    roiKeys: [] as string[],
    devIpList:[],
    devIpListRef:[]
  });

  const groupName = ref('');
  const pollingPeriod = ref();
  const xVal = ref(0);
  const yVal = ref(0);
  const checkArea = ref<CheckArea[]>([]);
  const invalidArea = ref<CheckArea[]>([]);
  const roiAreaAry = ref<CanvasArea>([]);
  const roiMoveToStart = ref(-1);
  const cameraImg = ref();
  const cameraCanvas = ref();
  const cameraSaveCanvas = ref();
  const roiMoveBeforeX = ref(0);
  const roiMoveBeforeY = ref(0);
  const roiAreaType = ref<ROIAreaType>(0);
  const cameraCtx = computed(() => {
    return cameraCanvas.value?.getContext('2d');
  });
  const cameraSaveCtx = computed(() => {
    return cameraSaveCanvas.value?.getContext('2d');
  });

  const isFloat = (num) => {
    return !isNaN(num) && num % 1 !== 0;
  };

  onMounted(() => {
    getList();
    getAlg();
    mainState.currentDate = sessionStorage.getItem('date');
    //一进入页面就触发相机组的确认点击
    // document.querySelector('ant-btn .sure')?.click();
  });
  const getAlg = async () => {
    mainState.selectAlg = await initDictOptions('ky_category,name,id,category="0" and pid!="0"');
  };
  const isshow= ref(false)
  const getList = async () => {
    const res = await list({});
    //开始就处理cameraIds
    res.records.forEach((item) => {
      item.cameraIds = item.cameraIds.split(',');
    });
    mainState.group = res.records;
  };

  //新建弹窗树check
  const handleCheck = (keys: string[]) => {
    mainState.checkedKeys = keys;
  };

  //新建弹窗树select
  const handleSelectTree = (keys: string[]) => {
    mainState.selectedKeys = keys;
  };

  //新建弹窗展开
  const handleExpand = (keys) => {
    //上次展开的进行收缩
    mainState.expandKeys = keys;
  };

  //显示弹窗
  const handleShowModal = () => {
    mainState.isShowModal = true;
    //再次打开弹窗内容清空
    mainState.inputValue = '';
    mainState.pollingGap = '';
    mainState.selectedCamera.length = 0;
  };

  //取消
  const handleCancel = () => {
    mainState.isShowModal = false;
  };

  //新建
  const handleOk = async () => {
    if (mainState.inputValue == '') {
      return message.error('请配置轮询组名称！');
    }
    if (mainState.pollingGap == '') {
      return message.error('请配置轮询检测间隔！');
    }
    if (isFloat(mainState.pollingGap)) {
      return message.error('轮询间隔不能配置小数');
    }

    await addGroup({
      cameraIds: mainState.cameraIds.join(','),
      groupName: mainState.inputValue,
      pollingPeriod: mainState.pollingGap,
    });

    handleIssued();
    getList();
    //新建成功也清除相机&选中的
    mainState.cameraIds.length = 0;
    mainState.isShowModal = false;
    mainState.selectedKeys.length = 0;
    mainState.checkedKeys.length = 0;
    mainState.expandKeys.length = 0;
  };

  //打开编辑弹窗
  const handleEditGroup = (item) => {
    mainState.isShowEditModal = true;
    mainState.groupInfo = item;
    groupName.value = item.groupName;
    pollingPeriod.value = item.pollingPeriod;
  };

  //编辑弹窗点编辑
  const handleEditOk = async () => {
    mainState.devIpListRef = await getDevIpList({ groupId: mainState.groupInfo.id });
    if (mainState.devIpList.length===0){
      mainState.devIpList = mainState.devIpListRef;
    }
    if (groupName.value == '') {
      return message.error('请配置轮询组名称！');
    }
    if (pollingPeriod.value == '') {
      return message.error('请配置轮询检测间隔！');
    }
    if (isFloat(pollingPeriod.value)) {
      return message.error('轮询间隔不能配置小数');
    }
    await editGroup({ id: mainState.groupInfo.id, groupName: groupName.value, pollingPeriod: pollingPeriod.value });
    mainState.isShowEditModal = false;
    handleIssued();
    for (const devIp in mainState.devIpList) {
      const openUrl = '/kyscene/addjob?' + mainState.devIpList[devIp];
      const param = { group_id: mainState.groupInfo.id };
      try {
        const resOpen: any = await axios.post(openUrl, param);
      } catch (error) {
        console.log('请求失败');
      }
    }
    getList();
  };

  //编辑弹窗点取消
  const handleEditCancel = () => {
    mainState.isShowEditModal = false;
  };

  //获取选择的相机
  const getSelectCamera = (val, id) => {
    getList();
    mainState.groupId = id;
    mainState.selectedCamera = val;
    mainState.tempCamera = val;
    //清空上次选择的
    mainState.cameraIds.length = 0;
    mainState.selectedCamera.forEach((item) => {
      mainState.cameraIds.push(item.id);
    });
  };

  //获取选择相机加父节点
  const getAll = async (val,kyle)=>{
     val.forEach((item) => {
        if (!item.parentKey && item.children) {
          if (kyle === '0') {
            mainState.treeValue.deviceTree.push({
              Key: item.key,
              slots: { icon: 'device_icon_info' },
              cameraCount: item.children.length,
              title: item.title,
              type: 'device',
              children: item.children,
            });
            mainState.selectedGroup = mainState.treeValue.deviceTree.filter((item) => {
              return item.type === 'device';
            });
          } else {
            mainState.treeValue.locationTree.push({
              Key: item.key,
              slots: { icon: 'location_parent_icon_info' },
              cameraCount: item.children.length,
              title: item.title,
              type: 'area',
              children: item.children,
            });

            mainState.selectedGroup = mainState.treeValue.deviceTree.filter((item) => {
              return item.type === 'area';
            });
          }
        }
      });
    
  }
  const handleGetGroup = async (val: TreeNodeData[], key, groupId, camera) => {
    if( mainState.cameraIds.length==0){
      const devIpList = await getDevIpList({ groupId: mainState.groupId });
      getAll(val,key)
      await editCamera({
        id: mainState.groupId,
        cameraIds: mainState.selectedCamera
          .map((item) => {
            return item.id;
          })
          .join(','),
      })
      handleIssuedremove(devIpList);
    }else{
      getAll(val,key)
      await editCamera({
        id: mainState.groupId,
        cameraIds: mainState.selectedCamera
          .map((item) => {
            return item.id;
          })
          .join(','),
      }).then(async ()=>{
        const devIpList = await getDevIpList({ groupId: mainState.groupId });
        handleIssuedadd(devIpList)
      })
    }
    getList();
  };

  async function reopen(devIpList, groupId) {
    console.log(mainState.group,"999");
    mainState.currentDate = new Date().toLocaleString();
    sessionStorage.setItem('date', mainState.currentDate);
    await saveGroupAll({});
    mainState.group.map(async item=>{
      if(item.algorithmList.length==0){
        for (const devIp in devIpList) {
          const closeUrl = '/kyscene/removejob?' + devIpList[devIp];
          const param = { group_id: groupId };
          try {
            const resOpen: any = await axios.post(closeUrl, param);
          } catch (error) {
            console.log('请求失败');
          }
        }
      }else{
         for (const devIp in devIpList) {
          const openUrl = '/kyscene/addjob?' + devIpList[devIp];
          const param = { group_id: groupId };
          try {
            const resOpen: any = await axios.post(openUrl, param);
          } catch (error) {
            console.log('请求失败');
          }
        }
      }
    })

  }

  //轮询配置
  const handlePollingSetting = (ele) => {
    mainState.algList = ele.algorithmList;
    mainState.ele = ele;
    mainState.isPollingModal = true;
  };

  //轮询配置确认
  const handlePollingOk = () => {
    mainState.isPollingModal = false;
    getList();
  };

  //轮询配置取消
  const handlePollingCancel = () => {
    mainState.isPollingModal = false;
  };
  
  //轮询配置删除
  const handleDeleteGroup = async (ele) => {
    mainState.devIpListRef = await getDevIpList({ groupId: ele.id });
    setTimeout(async () => {
      deleteGroup({ id: ele.id }, getList).then(async () => {
        if (mainState.devIpList.length === 0) {
          mainState.devIpList = mainState.devIpListRef;
        }
        for (const devIp in mainState.devIpList) {
          const closeUrl = '/kyscene/removejob?' + mainState.devIpList[devIp];
          const param = { group_id: ele.id };
          try {
            const resClose: any = await axios.post(closeUrl, param);
          } catch (error) {
            console.log('请求失败');
          }
        }
        setTimeout(()=>{
          handleIssued();
        },300)
        
      });
    }, 300);
  };

  // //轮询配置删除
  // const handleDeleteGroup = async (ele) => {
  //    handleIssued();
  //   mainState.devIpListRef = await getDevIpList({ groupId: ele.id });
  //   if (mainState.devIpList.length===0){
  //     mainState.devIpList = mainState.devIpListRef;
  //   }
  //   for (const devIp in mainState.devIpList) {
  //     const closeUrl = '/kyscene/removejob?' + mainState.devIpList[devIp];
  //     const param = { group_id: ele.id };
  //     try {
  //       const resClose: any = await axios.post(closeUrl, param);
  //     } catch (error) {
  //       console.log('请求失败');
  //     }
  //   }
   
  //   await deleteGroup({ id: ele.id }, getList);
    
  // };

  //轮询配置弹窗的删除
  const handleDelete = () => {
    getList();
  };

  //配置ROI
  const handleROISetting = async (item, ele) => {
    uncoveredSoil.value = '裸土覆盖' === item.algorithmName
    console.log('is 裸土覆盖', uncoveredSoil.value)
    mainState.groupId = ele.id;
    mainState.cameraTree = await cameraTreeData({});
    //如果树存在于总的树结构中，保存，否则跳过
    mainState.tempValue.deviceTree.length = 0;
    mainState.tempValue.locationTree.length = 0;
    mainState.cameraTree.deviceTree.forEach((item) => {
      let selectChildren = [];
      if (item.children) {
        item.children.filter((e) => {
          if (ele.cameraIds.indexOf(e.key) !== -1) {
            selectChildren.push(e);
          }
        });
      }
      if (selectChildren.length > 0) {
        mainState.tempValue.deviceTree.push({
          Key: item.key,
          slots: { icon: 'device_icon_info' },
          cameraCount: selectChildren.length,
          title: item.title,
          type: 'device',
          children: selectChildren,
        });
      }
    });
    mainState.cameraTree.locationTree.forEach((item) => {
      let selectChildren = [];
      if (item.children) {
        item.children.filter((e) => {
          if (ele.cameraIds.indexOf(e.key) !== -1) {
            selectChildren.push(e);
          }
        });
      }
      if (selectChildren.length > 0) {
        mainState.tempValue.locationTree.push({
          Key: item.key,
          slots: { icon: 'location_parent_icon_info' },
          cameraCount: selectChildren.length,
          title: item.title,
          type: 'area',
          children: selectChildren,
        });
      }
    });
    mainState.algGroup = ele;
    mainState.showRoiSettingModal = true;
    mainState.algItem = item;
    mainState.selectedGroup = mainState.tempValue.deviceTree;
  };

  //配置ROIqueren
  const handleSettingOk = async () => {
    if (uncoveredSoil.value) { // 裸土覆盖必须设置ROI
      console.log("roiAreaType:" + roiAreaType.value)
      const hasROI = roiAreaType.value === 1 || roiAreaType.value === 2
      console.log('@hasROI', hasROI)
      if (!hasROI) {
        message.error('请绘制ROI区域');
        return
      }
      const isDrawROI = checkArea.value.length > 0
      console.log('@isDrawROI', isDrawROI)
      if (!isDrawROI) {
        message.error('请绘制ROI区域')
        return
      }
    }
    mainState.devIpListRef = await getDevIpList({ groupId: mainState.algGroup.id });
    if (mainState.devIpList.length===0){
      mainState.devIpList = mainState.devIpListRef;
    }
    await editROI({
      modelName: mainState.algItem.algorithmId,
      belongTo: mainState.cameraId,
      groupId: mainState.algGroup.id,
      checkArea: JSON.stringify(checkArea.value),
      invalidArea: JSON.stringify(invalidArea.value),
    });
    checkArea.value = [];
    invalidArea.value = [];
    reopen(mainState.devIpList, mainState.algGroup.id);
  };

  //配置ROI取消
  const handleSettingCancel = () => {
    mainState.showRoiSettingModal = false;
  };

  function loadROIImg() {
    if (cameraCtx.value) {
      try {
        cameraCtx.value.drawImage(cameraImg.value, 0, 0, 760, 428);
      } catch (e) {
        console.error(e);
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
    makEarc(ctx, xy.x, xy.y, GetRandomNum(4, 4), 0, 180, 'rgba(102,168,255,1)');

    if (roiAreaAry.value.length > 0) {
      if (xy.x > roiAreaAry.value[0].x - 38 && xy.x < roiAreaAry.value[0].x + 38 && xy.y > roiAreaAry.value[0].y - 21 && xy.y < roiAreaAry.value[0].y + 21) {
        if (roiAreaAry.value.length > 1) {
          piX = roiAreaAry.value[0].x;
          piY = roiAreaAry.value[0].y;
          ctx.clearRect(0, 0, cameraSaveCtx.value.width, cameraSaveCtx.value.height);
          makEarc(ctx, piX, piY, GetRandomNum(4, 4), 0, 180, 'rgba(102,168,255,1)');
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
            makEarc(ctx, piX, piY, GetRandomNum(4, 4), 0, 180, 'rgba(102,168,255,1)');
            ctx.lineTo(roiAreaAry.value[i].x, roiAreaAry.value[i].y);
          }
        }
        ctx.lineTo(piX, piY);
        //线条加粗

        if (roiAreaType.value === ROIAreaType.InvalidArea) {
          ctx.lineWidth = 2;
        } else {
          ctx.lineWidth = 3;
          ctx.setLineDash([5]);
        }

        ctx.closePath();
        // ctx.strokeStyle = 'rgba(255,255,255,0)'; //填充颜色

        // ctx.fill(); //填充
        ctx.stroke(); //绘制
      } else {
        ctx.strokeStyle = 'green';
        ctx.lineWidth = 3;
        ctx.setLineDash([5]);
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
        cameraSaveCtx.value.lineWidth = 2;
        cameraCtx.value.strokeStyle = 'gray';
        cameraCtx.value.lineWidth = 2;
      } else {
        cameraSaveCtx.value.strokeStyle = 'green';
        cameraSaveCtx.value.lineWidth = 3;
        cameraCtx.value.strokeStyle = 'green';
        cameraCtx.value.lineWidth = 3;
        cameraCtx.value.setLineDash([5]);
      }
      if (roiMoveToStart.value > 0 && roiAreaAry.value.length > 0) {
        piX = roiAreaAry.value[0].x;
        piY = roiAreaAry.value[0].y;
        //画点
        makEarc(ctx, piX, piY, GetRandomNum(2, 2), 0, 180, 'rgba(102,168,255,1)');
        roiAreaAry.value.push({ x: piX, y: piY });
        canvasSave(); //保存点线同步到另一个canvas
        saveCanvas(); //生成画布
      } else {
        piX = roiMoveBeforeX.value;
        piY = roiMoveBeforeY.value;
        makEarc(ctx, piX, piY, GetRandomNum(2, 2), 0, 180, 'rgba(102,168,255,1)');
        roiAreaAry.value.push({ x: piX, y: piY });
        canvasSave(); //保存点线同步到另一个canvas
      }
    } else {
      cameraSaveCtx.value.strokeStyle = 'green';
      cameraSaveCtx.value.lineWidth = 3;
      cameraCtx.value.strokeStyle = 'green';
      cameraCtx.value.lineWidth = 3;
      cameraCtx.value.setLineDash([5]);
      if (roiAreaAry.value.length > 0) {
        piX = roiMoveBeforeX.value;
        piY = roiMoveBeforeY.value;
        //画点
        makEarc(ctx, piX, piY, GetRandomNum(2, 2), 0, 180, 'rgba(102,168,255,1)');
        cameraCtx.value.setLineDash([5]);
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
        // if(!checkRoiAngle()){ //算法组说可以画凹多边形
        //   roiAreaAry.value = [];
        //   return;
        // }
        cameraCtx.value.beginPath();
        cameraCtx.value.moveTo(roiAreaAry.value[0].x, roiAreaAry.value[0].y);
        for (let i = 1; i < roiAreaAry.value.length; i++) {
          cameraCtx.value.lineTo(roiAreaAry.value[i].x, roiAreaAry.value[i].y);
          cameraCtx.value.fillStyle = 'rgba(255,255,255,0)'; //填充颜色
          cameraCtx.value.lineWidth = 3;
          cameraCtx.value.setLineDash([5]);
          cameraCtx.value.stroke(); //绘制
        }
        cameraCtx.value.closePath();
      }
    } else if (roiAreaType.value === ROIAreaType.Rect) {
      if (roiAreaAry.value.length > 1) {
        cameraCtx.value.lineWidth = 3;
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
        // cameraCtx.value.setLineDash([5]);
        cameraCtx.value.lineWidth = 2;
        cameraCtx.value.moveTo(roiAreaAry.value[0].x, roiAreaAry.value[0].y);
        for (let i = 1; i < roiAreaAry.value.length; i++) {
          cameraCtx.value.lineTo(roiAreaAry.value[i].x, roiAreaAry.value[i].y);
          cameraCtx.value.setLineDash([5]);
          // 设置填充的样式为斜线图案
          const pattern = cameraCtx.value.createPattern(createDiagonalPattern(), 'repeat');
          cameraCtx.value.fillStyle = pattern;
          // cameraCtx.value.fillStyle = 'gray'; //填充颜色

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
          cameraCtx.value.lineWidth = 5;
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
      cameraCtx.value.lineWidth = 2;
    } else {
      checkArea.value.push(roiArea);
      cameraCtx.value.lineWidth = 3;
    }

    cameraSaveCtx.value.clearRect(0, 0, cameraSaveCanvas.value.width, cameraSaveCanvas.value.height);
    cameraCtx.value.closePath(); //结束路径状态，结束当前路径，如果是一个未封闭的图形，会自动将首尾相连封闭起来
    cameraCtx.value.fill(); //填充
    cameraCtx.value.stroke(); //绘制
    roiAreaAry.value = [];
    changeLineColor();
  }

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

  function mulBtnClick(): void {
    roiAreaType.value = ROIAreaType.Polygon;
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

  function clearAllBtnRoi(): void {
    if (roiAreaType.value === ROIAreaType.Full) {
      loadROIImg();
      checkArea.value = [];
      invalidArea.value = [];
    }
  }

  //切换树
  const handleClickItem = (index: number) => {
    if (index === 0) {
      // mainState.selectedGroup.length = 0;
      // mainState.selectedGroup = mainState.treeValue.deviceTree;
      mainState.selectedGroup = mainState.tempValue.deviceTree;
      // mainState.cameraSum = mainState.selectedGroup[0].children?.length;
    } else {
      // mainState.selectedGroup.length = 0;
      // mainState.selectedGroup = mainState.treeValue.locationTree;
      mainState.selectedGroup = mainState.tempValue.locationTree;

      // mainState.cameraSum = mainState.selectedGroup[0].children?.length;
    }
  };

  //配置ROI删除
  const handleDeleteAlg = async (ele, item) => {
    mainState.devIpListRef = await getDevIpList({ groupId: ele.id });
    if (mainState.devIpList.length===0){
      mainState.devIpList = mainState.devIpListRef;
    }
    mainState.deleteItem = ele;
    await deleteAlg({ groupId: ele.id, algorithmId: item.algorithmId }, getList);
    setTimeout(() => {
      reopen(mainState.devIpList, ele.id);
    }, 500);
  };

  //获取图片
  const getImage = async (params) => {
    const dev = await imgInfo({ cameraId: params });
    if (dev) {
      mainState.loading = false;
      mainState.imgSrc = '/kyscene' + dev.imgSrc + '?' + dev.ip;
    }
  };

  const getROIById = async () => {
    checkArea.value = [];
    mainState.roiInfo = await getROI({ cameraId: mainState.cameraId, groupId: mainState.algGroup.id, algorithmId: mainState.algItem.algorithmId });
  };
  //点击树
  const handleSelect = async (item) => {
    //父节点点击不调接口
    if (item.node.dataRef.type === 'camera') {
      mainState.cameraId = item.node.dataRef.key;
      mainState.cameraName = item.node.dataRef.title;
      await getImage(mainState.cameraId);
      loadROIImg();
      checkArea.value = [];
      invalidArea.value = [];
      allImgBtnClick();
      await getROIById();
      setROI();
    }
  };

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
        cameraCtx.value.lineWidth = 3;
        cameraCtx.value.setLineDash([5]);
        cameraCtx.value.strokeStyle = 'green';
      } else {
        // 设置填充的样式为斜线图案
        const pattern = cameraCtx.value.createPattern(createDiagonalPattern(), 'repeat');
        cameraCtx.value.fillStyle = pattern;
        cameraCtx.value.lineWidth = 2;
        cameraCtx.value.strokeStyle = 'gray';
      }
      cameraCtx.value.fill();
      cameraCtx.value.stroke(); //绘制
      changeLineColor();
    }
  }

  //画roi
  const setROI = () => {
    if (mainState.roiInfo?.checkArea || mainState.roiInfo?.invalidArea) {
      cameraCtx.value.clearRect(0, 0, cameraCanvas.value.width, cameraCanvas.value.height);
      //没有值，不要被转化为null
      checkArea.value = JSON.parse(mainState.roiInfo.checkArea);
      invalidArea.value = JSON.parse(mainState.roiInfo.invalidArea);
      loadROIImg();
      if (checkArea.value.length === 0 && invalidArea.value.length === 0) {
        roiAreaType.value = ROIAreaType.Full;
        return;
      }
    }
    echoRoi(checkArea.value);
    echoRoi(invalidArea.value);
  };

  //点击上一张相机图
  const handleGoPrev = async () => {
    if (mainState.selectedGroup[0].children) {
      mainState.currentIndex = mainState.selectedGroup[0].children.findIndex((item) => item.key === mainState.cameraId);
      if (mainState.currentIndex > 0) {
        mainState.currentIndex--;
        mainState.cameraId = mainState.selectedGroup[0].children[mainState.currentIndex].key;
        mainState.roiKeys.length = 0;
        mainState.roiKeys.push(mainState.cameraId);
        await getImage(mainState.cameraId);
        loadROIImg();
        await getROIById();
        setROI();
      }
    }
  };

  //点击下一张相机图
  const handleGoNext = async () => {
    if (mainState.selectedGroup[0].children) {
      mainState.currentIndex = mainState.selectedGroup[0].children.findIndex((item) => item.key === mainState.cameraId);
      if (mainState.currentIndex < mainState.selectedGroup[0].children.length - 1) {
        mainState.currentIndex++;
        mainState.cameraId = mainState.selectedGroup[0].children[mainState.currentIndex].key;
        mainState.roiKeys.length = 0;
        mainState.roiKeys.push(mainState.cameraId);
        await getImage(mainState.cameraId);
        loadROIImg();
        await getROIById();
        setROI();
      }
    }
  };


  //开关
  const handleChecked = async (item) => {
    mainState.devIpListRef = await getDevIpList({ groupId: item.id });
    if (mainState.devIpList.length===0){
      mainState.devIpList = mainState.devIpListRef;
    }
    item.runStatus = item.runStatus ? 0 : 1;
    //todo
    handleIssued();
    await changeStatus({ id: item.id, runStatus: item.runStatus }, getList);
    for (const devIp in mainState.devIpList) {
      if (item.runStatus == 1) {
        const openUrl = '/kyscene/addjob?' + mainState.devIpList[devIp];
        const param = { group_id: item.id };
        try {
          const res: any = await axios.post(openUrl, param);
          if (res.data.code == 2000) {
            message.success(res.data.msg);
          }
          if (res.data.code == 2008) {
            message.error(res.data.msg);
          }
        } catch (error) {
          console.log('请求失败');
        }
      } else {
        const closeUrl = '/kyscene/removejob?' + mainState.devIpList[devIp];
        const param = { group_id: item.id };
        try {
          const res: any = await axios.post(closeUrl, param);
          if (res.data.code == 2000) {
            message.success(res.data.msg);
          }
          if (res.data.code == 2009) {
            message.error(res.data.msg);
          }
        } catch (error) {
          console.log('请求失败');
        }
      }
    }
  };

  //配置下发
  const handleIssued = async () => {
    mainState.currentDate = new Date().toLocaleString();
    sessionStorage.setItem('date', mainState.currentDate);
    await saveGroupAll({});
  };
  //配置下发--new
  const handleIssuedremove = async (devIpList) => {
    mainState.currentDate = new Date().toLocaleString();
    sessionStorage.setItem('date', mainState.currentDate);
    await saveGroupAll({}).then(async ()=>{
      for (const devIp in devIpList) {
        const closeUrl = '/kyscene/removejob?' + devIpList[devIp];
        const param = { group_id: mainState.groupId };
        try {
          const resOpen: any = await axios.post(closeUrl, param);
        } catch (error) {
          console.log('请求失败');
        }
      }
    });
  };
  const handleIssuedadd = async (devIpList) => {
    mainState.currentDate = new Date().toLocaleString();
    sessionStorage.setItem('date', mainState.currentDate);
    await saveGroupAll({}).then(async ()=>{
      for (const devIp in devIpList) {
        const closeUrl = '/kyscene/addjob?' + devIpList[devIp];
        const param = { group_id: mainState.groupId };
        try {
          const resOpen: any = await axios.post(closeUrl, param);
        } catch (error) {
          console.log('请求失败');
        }
      }
    });
  };
</script>
<style lang="less" scoped>
  .canvas-save {
    position: absolute;
    left: 83px;
  }
  .polling-container {
    height: calc(100vh - 72px);
    overflow-x: auto;
  }
  .polling-management {
    background: var(--background-Color);
    margin: 12px 12px 12px 12px;
    font-family: Source Han Sans CN;
    min-width: 1200px;

    .title {
      font-weight: 600;
      color: var(--headerline-text-color);
      font-size: 15px;
      padding: 24px 0 16px 24px;
    }
    .add-group {
      padding: 0 0 16px 24px;
    }
    .add-icon {
      background: url('/@/assets/svg/icon/editor_btn_addalgo_default.svg');
      width: 14px;
      height: 14px;
      display: inline-block;
    }

    .group-container {
      height: 660px;
      overflow: auto;
      width: calc(100% - 24px);
      margin: 0;
    }
    .list {
      margin: 16px 24px 16px 24px;
      border: 1px solid;
      border-color: var(--weak-split-line-color);
      border-radius: 4px;
      &:first-child {
        margin: 0 24px 16px 24px;
      }
    }
    .group {
      display: flex;
      justify-content: space-between;
      height: 57px;
      align-items: center;
      border-bottom: 1px solid var(--weak-split-line-color);
      border-radius: 1px;
    }
    .control {
      display: flex;
    }
    .switch {
      padding: 2px 0 0 24px;
      .name {
        color: var(--title-color);
        font-size: 15px;
        margin: 0 0 0 12px;
      }
    }

    .select {
      display: flex;
      justify-content: flex-end;
      align-items: center;
    }
    .alg-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      height: 48px;
      margin: 0 0 0 48px;
      border-bottom: 1px solid var(--weak-split-line-color);
      border-radius: 4px;
      &-name {
        color: var(--title-color);
        font-size: 13px;
      }
      &-icon {
        background: url('/@/assets/svg/icon/tree_icon_algorithm.svg');
        display: inline-block;
        width: 14px;
        height: 14px;
        margin: 0 4px 0 0;
      }
    }
    .last {
      border: none;
    }
    .operation {
      font-size: 13px;
      .setting {
        width: 68px;
        height: 20px;
        background-color: transparent;
        border-radius: 4px;
        color: var(--primary-Color);
        margin: 0 24px 0 0;
        &:hover {
          background-color: rgba(133, 159, 242, 0.2);
          border-radius: 4px;
        }
      }
      .edit-button {
        width: 42px;
        height: 20px;
        margin-left: 12px;
        background-color: transparent;
        color: var(--primary-Color);
        margin: 0 24px 0 0;
        &:hover {
          background-color: rgba(133, 159, 242, 0.2);
          border-radius: 4px;
        }
      }
      .delete {
        width: 42px;
        height: 20px;
        margin-left: 12px;
        background-color: transparent;
        color: var(--serious-text-color);
        margin: 0 32px 0 0;
        &:hover {
          background-color: rgba(204, 102, 102, 0.2);
          border-radius: 4px;
        }
      }
    }
    .bottom {
      width: 100%;
      height: 88px;
      bottom: 12px;
      width: 100%;
      margin: 40px 0 0 0;
      border-top: 1px solid var(--weak-split-line-color);

      .button {
        display: flex;
        justify-content: center;
        padding: 16px 0 0 0;
      }
      .tips {
        display: flex;
        justify-content: center;
        color: #a3a7b5;
        font-size: 13px;
        margin: 10px 0 21px 0;
        &-icon {
          background: url('/@/assets/svg/icon/info_icon_tips.svg');
          display: inline-block;
          width: 14px;
          height: 14px;
          margin: 2px 4px 0 0;
        }
      }
    }
  }
  .ant-switch {
    margin: 0;
    padding: 0;
    color: rgba(0, 0, 0, 0.85);
    font-size: 14px;
    font-variant: tabular-nums;
    line-height: 1.5715;
    list-style: none;
    font-feature-settings: tnum;
    position: relative;
    display: inline-block;
    box-sizing: border-box;
    min-width: 20px;
    height: 8px;
    line-height: 8px;
    vertical-align: middle;
    background-color: rgba(0, 0, 0, 0.25);
    border: 0;
    border-radius: 100px;
    cursor: pointer;
    transition: all 0.2s;
    -webkit-user-select: none;
    -moz-user-select: none;
    user-select: none;
    box-shadow: 0px 0px 1px rgba(51, 63, 102, 0.1);
  }
  :deep .ant-switch-handle {
    position: absolute;
    top: -1px;
    left: 0px;
    width: 10px;
    height: 10px;
    transition: all 0.2s ease-in-out;
    border-color: #e8ecf3;
  }
  .ant-switch-checked {
    background-color: #7089ed;
  }
  :deep .ant-switch-inner {
    display: block;
    margin: 0 7px 0 0px;
    color: #fff;
    font-size: 10px;
    transition: margin 0.2s;
  }
  :deep .ant-switch-handle::before {
    position: absolute;
    top: 0px;
    right: 0;
    bottom: 0;
    left: 0;
    background-color: #fff;
    border-radius: 9px;
    box-shadow: 0 2px 4px 0 rgb(0 35 11 / 20%);
    transition: all 0.2s ease-in-out;
    content: '';
  }
  .ant-switch-handle {
    position: absolute;
    top: -1px;
    left: 0px;
    width: 10px;
    height: 10px;
    transition: all 0.2s ease-in-out;
  }

  .ant-switch::after {
    position: absolute;
    top: -1px;
    left: 0px;
    width: 10px;
    height: 10px;
    background-color: #fff;
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.36s cubic-bezier(0.78, 0.14, 0.15, 0.86);
    content: ' ';
    border: 1px solid;
    border-color: #adb4cc;
  }
  :deep .ant-switch-handle::before {
    display: none;
  }
  .ant-switch-checked::after {
    left: 70%;
    margin-left: 5px;
    transform: translateX(-100%);
  }
  .divider {
    width: 1px;
    height: 100%;
    border-left: 1px solid var(--weak-split-line-color);
    margin: 0 8px 0 0px;
  }
</style>
