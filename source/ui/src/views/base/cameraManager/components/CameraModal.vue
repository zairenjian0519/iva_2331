<template>
  <div ref="offlineModal">
    <Modal
      ref="cameraModel"
      :width="modalWidth"
      :title="mainState.isUpdate ? '相机信息' : '新建相机'"
      v-model:visible="mainState.visible"
      :okText="mainState.isUpdate ? '编辑' : '新建'"
      :okButtonProps="okButtonProps"
      :get-container="getContainer"
      class="fixStyle"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <Tabs v-model:activeKey="mainState.activeKey">
        <TabPane :key="0" tab="相机配置">
          <a-form class="formClass" :model="cameraFrom" initial :rules="rules" ref="fromRef">
            <a-row :gutter="24" style="margin-left: 35px; margin-top: 18px">
              <a-col :span="7">
                <label>相机名称</label>
                <a-form-item label="" has-feedback name="cameraName"
                             :label-col="{ span: 0, offset: 0 }" :wrapper-col="{ span: 24 }">
                  <a-input v-model:value="cameraFrom.cameraName"/>
                </a-form-item>
              </a-col>
              <a-col :span="7">
                <label>相机归属</label>
                <a-form-item label="" has-feedback name="cameraOwnership"
                             :label-col="{ span: 0, offset: 0 }" :wrapper-col="{ span: 24 }">
                  <JDictSelectTag :show-choose-option="false" type="select"
                                  :value="cameraFrom.cameraOwnership"
                                  @change="cameraOwnershipChange"
                                  dict-code="device_management,device_name,id"/>
                </a-form-item>
              </a-col>
              <a-col :span="7" style="margin-right: 5px">
                <label>自定义分组</label>

                <a-form-item label="" has-feedback name="location"
                             :label-col="{ span: 0, offset: 0 }" :wrapper-col="{ span: 24 }">
                  <!-- 下拉 -->
                  <!-- <JDictSelectTag :show-choose-option="false" type="select" @change="locationChange" :value="cameraFrom.location" dict-code="ky_category,name,id,category='1'" /> -->
                  <TreeSelect
                    :tree-data="cameraFrom.locationTree"
                    v-model:value="cameraFrom.location"
                    ref="treeSelect"
                    :get-popup-container="
                      (triggerNode) => {
                        return triggerNode.parentNode;
                      }
                    "
                    @change="handleChange"
                    :multiple="false"
                    :tree-icon="true"
                    :tree-checkable="false"
                    tree-default-expand-all
                    :show-checked-strategy="ShowParent"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24" style="margin-left: 35px">
              <a-col :span="7">
                <label>相机厂商</label>
                <a-form-item label="" has-feedback name="factory"
                             :label-col="{ span: 0, offset: 0 }" :wrapper-col="{ span: 24 }">
                  <JDictSelectTag placeholder="请选择" :show-choose-option="false"
                                  :value="cameraFrom.factory" @change="factoryChange"
                                  dict-code="factory_name"/>
                </a-form-item>
              </a-col>
              <a-col :span="14" class="cameraTypeStyle">
                <label>相机型号</label>
                <a-form-item label="" has-feedback name="cameraType"
                             :label-col="{ span: 0, offset: 0 }" :wrapper-col="{ span: 24 }">
                  <!--                  :class="[cameraFrom.factory === 'other' && cameraFrom.cameraTypeName === '其他' ? 'disabled' : '']"  增加其他相机厂商都为手动输入 和其他同级-->
                  <JDictSelectTag
                    placeholder="请选择"
                    type="select"
                    :show-choose-option="false"
                    @change="cameraTypeChange"
                    :value="cameraFrom.cameraType"
                    :dict-code="`camera_factory,camera_type,id,factory_name = '${cameraFrom.factory}'`"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24" style="margin-left: 35px">
              <a-col :span="21">
                <label>相机编码</label>
                <a-form-item label="" has-feedback name="cameraNumber"
                             :label-col="{ span: 0, offset: 0 }" :wrapper-col="{ span: 24 }">
                  <a-input style="width: 498px" v-model:value="cameraFrom.cameraNumber"
                           placeholder="非必填"/>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24" style="margin-left: 35px">
              <a-col :span="21">
                <label>视频流地址</label>
                <!-- <a-checkbox v-model:checked="mainState.isChecked" @change="handleStreamChange">手动输入</a-checkbox> -->
                <a-form-item label="" has-feedback name="streamAddress"
                             :label-col="{ span: 0, offset: 0 }" :wrapper-col="{ span: 24 }">
                  <a-input style="width: 498px" v-model:value="cameraFrom.streamAddress"/>
                </a-form-item>
              </a-col>
              <div style="display: flex; align-items: center; margin-left: 20px">
                <span><img :src="infoIcon" alt=""/></span>
                <span>请手动输入，或者通过以下配置项自动生成视频流地址</span>
              </div>
            </a-row>
            <a-row :gutter="24" style="margin-left: 35px; margin-top: 20px">
              <a-col :span="7">
                <label>码流</label>
                <a-form-item label="" :label-col="{ span: 0, offset: 0 }"
                             :wrapper-col="{ span: 16 }">
                  <div class="tab-control">
                    <div
                      v-for="(item, index) of ['主码流', '子码流']"
                      :key="index"
                      :class="{ active: mainState.streamTypes.indexOf(cameraFrom.streamType) === index }"
                      @click="streamTypeItem(index)"
                      class="tab-control-item">
                      <span>{{ item }}</span>
                    </div>
                  </div>
                </a-form-item>
              </a-col>
            </a-row>
            <div>
              <a-divider class="divider"/>
            </div>
            <a-row :gutter="24" style="margin-left: 35px">
              <a-col :span="24">
                <a-form-item>
                  <BaseSwitch v-model:checked="checkeds" @handle-change="handleChecked"/>
                  <span style="margin-left: 8px"> 配置项</span>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24" style="margin-left: 35px" v-show="checkeds">
              <a-col :span="7">
                <label>用户名</label>
                <a-form-item label="" has-feedback name="username"
                             :label-col="{ span: 0, offset: 0 }" :wrapper-col="{ span: 24 }">
                  <a-input v-model:value="cameraFrom.username" defaultValue="非必填"
                           @change="streamAddressStr"/>
                </a-form-item>
              </a-col>
              <a-col :span="14">
                <label>密码</label>
                <a-form-item label="" has-feedback name="password"
                             :label-col="{ span: 0, offset: 0 }" :wrapper-col="{ span: 24 }">
                  <a-input-password v-model:value="cameraFrom.password" defaultValue="非必填"
                                    @change="streamAddressStr"/>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24" style="margin-left: 35px" v-show="checkeds">
              <a-col :span="14">
                <label>IP地址</label>
                <a-form-item label="" has-feedback name="ip" :label-col="{ span: 0, offset: 0 }"
                             :wrapper-col="{ span: 24 }">
                  <a-input style="width: 328px" v-model:value="cameraFrom.ip"
                           @change="streamAddressStr"/>
                </a-form-item>
              </a-col>
              <a-col :span="7">
                <label>端口号</label>
                <a-form-item label="" has-feedback name="port" :label-col="{ span: 0, offset: 0 }"
                             :wrapper-col="{ span: 24 }">
                  <a-input class="portInputStyle" v-model:value="cameraFrom.port"
                           @change="streamAddressStr"/>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24" style="margin-left: 35px" v-show="checkeds">
              <a-col :span="7">
                <label>通道</label>
                <a-form-item label="" name="channel" :label-col="{ span: 0, offset: 0 }"
                             :wrapper-col="{ span: 16 }">
                  <a-input v-model:value="cameraFrom.channel" @change="streamAddressStr"/>
                </a-form-item>
              </a-col>
              <!--              <a-col :span="7" style="margin-bottom: 30px">
                              <div v-show="cameraFrom.factory === 'hk'">
                                <label>码流格式</label>
                                <a-form-item label="" :label-col="{ span: 0, offset: 0 }"
                                             :wrapper-col="{ span: 16 }">
                                  <div class="tab-control">
                                    <div
                                      v-for="(item, index) of ['H264', 'H265', 'MPEG-4']"
                                      :key="index"
                                      :class="{ active: mainState.streamFormats.indexOf(cameraFrom.streamFormat) === index }"
                                      @click="streamFormatItem(index)"
                                      class="tab-control-item"
                                    >
                                      <span>{{ item }}</span>
                                    </div>
                                  </div>
                                </a-form-item>
                              </div>
                            </a-col>-->
            </a-row>
          </a-form>
        </TabPane>
        <TabPane :key="1" tab="推流设置">
          <a-row :gutter="24" style="margin-left: 35px">
            <a-col :span="21">
              <label>视频流地址</label>
              <a-form-item label="" has-feedback name="streamAddressPush"
                           :label-col="{ span: 0, offset: 0 }" :wrapper-col="{ span: 24 }">
                <a-input style="width: 498px" v-model:value="cameraFrom.streamAddressPush"/>
              </a-form-item>
            </a-col>
            <div style="display: flex; align-items: center; margin-left: 20px">
              <span><img :src="infoIcon" alt=""/></span>
              <span>请手动输入，或者通过以下配置项自动生成视频流地址</span>
            </div>
          </a-row>
          <a-row :gutter="24" style="margin-left: 35px; margin-top: 20px">
            <a-col :span="7">
              <label>码流</label>
              <a-form-item label="" :label-col="{ span: 0, offset: 0 }" :wrapper-col="{ span: 16 }">
                <div class="tab-control">
                  <div
                    v-for="(item, index) of ['主码流', '子码流']"
                    :key="index"
                    :class="{ active: mainState.streamTypesPush.indexOf(cameraFrom.streamTypePush) === index }"
                    @click="streamTypePushItem(index)"
                    class="tab-control-item">
                    <span>{{ item }}</span>
                  </div>
                </div>
              </a-form-item>
            </a-col>
          </a-row>
          <!--          <div>
                      <a-divider class="divider"/>
                    </div>-->
          <!--          <a-row :gutter="24" style="margin-left: 35px">
                      <a-col :span="24">
                        <a-form-item>
                          <BaseSwitch v-model:checked="checkeds" @handle-change="handleChecked"/>
                          <span style="margin-left: 8px"> 配置项</span>
                        </a-form-item>
                      </a-col>
                    </a-row>-->
          <!--          <a-row :gutter="24" style="margin-left: 35px" v-show="checkeds">
                      <a-col :span="7" style="margin-bottom: 30px">
                        <div v-show="cameraFrom.factory === 'hk'">
                          <label>码流格式</label>
                          <a-form-item label="" :label-col="{ span: 0, offset: 0 }"
                                       :wrapper-col="{ span: 16 }">
                            <div class="tab-control">
                              <div
                                v-for="(item, index) of ['H264', 'H265', 'MPEG-4']"
                                :key="index"
                                :class="{ active: mainState.streamFormatsPush.indexOf(cameraFrom.streamFormatPush) === index }"
                                @click="streamFormatPushItem(index)"
                                class="tab-control-item">
                                <span>{{ item }}</span>
                              </div>
                            </div>
                          </a-form-item>
                        </div>
                      </a-col>
                    </a-row>-->
        </TabPane>
        <a-tab-pane :key="2" tab="NVR相机添加">
          <CameraNVR ref="cameraNvr"/>
        </a-tab-pane>
      </Tabs>
      <div class="import" @click="handleShowImport">
        <i class="import-icon"></i>
        <span class="import-btn">批量导入</span>
      </div>
      <camera-import v-model:visible="mainState.isImportClose"
                     @handleImportCancel="handleImportCancel"/>
    </Modal>
  </div>
</template>

<script lang="ts" setup>
import {Checkbox, Col, message, Modal, Row, TabPane, Tabs, TreeSelect} from 'ant-design-vue';
import {computed, defineExpose, onMounted, reactive, ref, watch} from 'vue';
import BaseSwitch from '/@/components/BaseSwitch/BaseSwitch.vue';
import JDictSelectTag from '/@/components/Form/src/jeecg/components/JDictSelectTag.vue';
import CameraImport from './CameraImport.vue';
import {
  cameraTree,
  checkCameraName,
  checkLicense,
  queryByDeviceId,
  savaAllByDev,
  saveOrUpdate
} from '/@/views/base/cameraManager/CameraManager.api';
import {cameraData} from '/@/views/base/cameraManager/CameraManager.data';
import infoIcon from '/@/assets/svg/icon/info_icon_warning.svg';
import CameraNVR from "./CameraNVR.vue";

const offlineModal = ref<HTMLElement | null>(null);

const emit = defineEmits(['handleOk', 'handleCancel']);

const modalWidth = ref(600)

const okButtonProps = reactive({
  loading: false
})

interface selectValue {
  value: string;
  label: string;
}

const ARow = Row;
const ACol = Col;
const ACheckbox = Checkbox;

const cameraNvr = ref();
const fromRef = ref();
const cameraModel = ref();
const value = ref<string[]>([]);
const ShowParent = TreeSelect.SHOW_PARENT;

let cameraFrom = reactive({
  id: '',
  cameraName: '',
  location: '',
  cameraOwnership: '',
  username: '',
  password: '',
  streamAddress: '',
  cameraType: '',
  streamType: 'main',
  streamFormat: 'h264',
  ip: '',
  port: '',
  workType: '',
  factory: '',
  channel: '1',
  isConfig: '1',
  streamAddressPush: '',
  streamTypePush: 'main',
  streamFormatPush: 'h264',
  locationTree: [],
  cameraTypeName: '',
  cameraNumber: '',
});

onMounted(async () => {
  const cameraRes = await cameraTree();
  cameraFrom.locationTree = cameraRes.locationTree;
  // cameraRes.locationTree.forEach((item) => {
  // item.value = item.key;
  // if (item.children) {
  //   item.children.forEach((ele) => {
  //     ele.value = ele.key;
  //   });
  // }
  recursionTree(cameraRes.locationTree, 0);
  // });
});
//递归给树节点赋value
const recursionTree = (data, deep) => {
  if (++deep > 10) {
    throw new Error('层级嵌套超过10层');
  }
  data.forEach((item) => {
    item.value = item.key;
    if (item.children) {
      item.children.forEach((ele) => {
        ele.value = ele.key;
        recursionTree(item.children, deep);
      });
    }
  });
};

let checkeds = computed(() => {
  return cameraFrom.isConfig === '1';
});
// 制定modal挂载的节点
const getContainer = () => {
  return offlineModal.value;
};

const mainState = reactive({
  labelCol: {span: 0, offset: 10},
  wrapperCol: {span: 14},
  isNvr: true,
  workTypeIndex: 0 as number,
  streamFormats: ['h264', 'h265', 'mpeg-4'],
  streamFormatsPush: ['h264', 'h265', 'mpeg-4'],
  streamTypes: ['main', 'sub'],
  streamTypesPush: ['main', 'sub'],
  options: [] as selectValue[],
  isUpdate: false,
  visible: false,
  oldOwnerShip: '',
  activeKey: 0 as number,
  //导入框是否关闭
  isImportClose: false as Boolean,
  isChecked: false,
  //相机型号
  isType: true,
  //中间变量
  tempIp: '',
  item: {} as cameraData,
});

// 初始化form
watch(
  () => mainState.visible && !mainState.isUpdate,
  (val) => {
    if (val) {
      cameraFrom.cameraName = '';
      cameraFrom.location = '';
      cameraFrom.cameraOwnership = '';
      cameraFrom.username = '';
      cameraFrom.password = '';
      cameraFrom.streamAddress = '';
      cameraFrom.streamAddressPush = ''
      cameraFrom.cameraType = '';
      cameraFrom.ip = '';
      cameraFrom.port = '';
      cameraFrom.workType = '';
      cameraFrom.factory = '';
      cameraFrom.channel = '';
      cameraFrom.cameraNumber = '';
    }
  },
  {
    deep: true,
    immediate: true,
  }
)

//
watch(
  () => mainState.activeKey,
  (val) => {
    if (val === 1) {
      if (!cameraFrom.streamTypePush) {
        cameraFrom.streamAddressPush = cameraFrom.streamAddress;
        cameraFrom.streamTypePush = cameraFrom.streamType
      }
    }
    if (val === 2) {
      modalWidth.value = 800
    } else {
      modalWidth.value = 600
    }
  },
  {
    immediate: true,
    deep: true,
  }
)

const getStreamTypeByFactory = (cameraStreamType: string) => {
  let streamType: string;
  const factory = cameraFrom.factory
  if (cameraStreamType == 'main') {
    switch (factory) {
      case 'hk':
        streamType = '01'
        break
      case 'dh':
        streamType = '0'
        break
      case 'ys':
        streamType = '0'
        break
      case 'tdwy':
        streamType = '1'
        break
      default:
        streamType = 'main'
        break
    }
  } else if (cameraStreamType == 'sub') {
    switch (factory) {
      case 'hk':
        streamType = '02'
        break
      case 'dh':
        streamType = '1'
        break
      case 'ys':
        streamType = '1'
        break
      case 'tdwy':
        streamType = '2'
        break
      default:
        streamType = 'sub'
        break
    }
  } else {
    streamType = 'sub'
  }
  return streamType;
}

const streamAddressStr = () => {
  const streamType = getStreamTypeByFactory(cameraFrom.streamType);
  const rtspPrefix: string = `rtsp://${cameraFrom.username}:${cameraFrom.password}@${cameraFrom.ip}:${cameraFrom.port}`
  if (cameraFrom.factory === 'hk') {
    //rtsp://user:password@192.168.1.77:554/Streaming/Channels/102
    cameraFrom.streamAddress = `${rtspPrefix}/Streaming/Channels/${cameraFrom.channel}${streamType}`
  } else if (cameraFrom.factory === 'dh') {
    //rtsp://user:password@192.168.1.77:554/cam/realmonitor?channel=1&subtype=1
    cameraFrom.streamAddress = `${rtspPrefix}/cam/realmonitor?channel=${cameraFrom.channel}&subtype=${streamType}`
  } else if (cameraFrom.factory === 'ys') {
    //rtsp://user:password@192.168.1.100:554/unicast/c1/s1
    cameraFrom.streamAddress = `${rtspPrefix}/unicast/c${cameraFrom.channel}/s${streamType}`
  } else if (cameraFrom.factory === 'tdwy') {
    //rtsp://admin:admin@192.168.42.134:554/1/2
    cameraFrom.streamAddress = `${rtspPrefix}/${cameraFrom.channel}/${streamType}`
  }
};
const cameraOwnershipChange = (val) => {
  cameraFrom.cameraOwnership = val;
}

// 推流地址，就是zlm拉流地址，使用主码流
const streamAddressStrPush = () => {
  const streamType = getStreamTypeByFactory(cameraFrom.streamTypePush);
  const rtspPrefix: string = `rtsp://${cameraFrom.username}:${cameraFrom.password}@${cameraFrom.ip}:${cameraFrom.port}`
  if (cameraFrom.factory === 'hk') {
    cameraFrom.streamAddressPush = `${rtspPrefix}/Streaming/Channels/${cameraFrom.channel}${streamType}`
  } else if (cameraFrom.factory === 'dh') {
    cameraFrom.streamAddressPush = `${rtspPrefix}/cam/realmonitor?channel=${cameraFrom.channel}&subtype=${streamType}`
  } else if (cameraFrom.factory === 'ys') {
    cameraFrom.streamAddressPush = `${rtspPrefix}/unicast/c${cameraFrom.channel}/s${streamType}`
  } else if (cameraFrom.factory === 'tdwy') {
    cameraFrom.streamAddressPush = `${rtspPrefix}/${cameraFrom.channel}/${streamType}`
  } else {
    cameraFrom.streamAddressPush = cameraFrom.streamAddress
  }
};

//自定义分组树的切换
const handleChange = (value, label, extra) => {
  cameraFrom.location = value;
};
// //是否选择Nvr
// const handleStreamChange = (e: Event) => {
//   if (e?.target?.checked === true) {
//     cameraFrom.streamAddress = '';
//     cameraFrom.isConfig = '0';
//     cameraFrom.ip = '';
//   } else {
//     cameraFrom.isConfig = '1';
//     cameraFrom.ip = mainState.item.ip;
//     cameraFrom.channel = mainState.item.channel;
//     cameraFrom.username = mainState.item.username;
//     cameraFrom.password = mainState.item.password;
//     cameraFrom.port = mainState.item.port;
//     streamAddressStr();
//     streamAddressStrPush();
//   }
// };
const locationChange = (val) => {
  cameraFrom.location = val;
};

const factoryChange = (val) => {
  cameraFrom.factory = val;
  //重新初始化streamType和streamTypes
  // switch (val) {
  //   case 'hk':
  //     cameraFrom.streamType = 'main'
  //     mainState.streamTypes = ['main', 'sub']
  //     break
  //   case 'dh':
  //     cameraFrom.streamType = '1'
  //     mainState.streamTypes = ['0', '1']
  //     break
  //   case 'ys':
  //     cameraFrom.streamType = 'video2'
  //     mainState.streamTypes = ['video1', 'video2']
  //     break
  //   case 'tdwy':
  //     cameraFrom.streamType = '2'
  //     mainState.streamTypes = ['1', '2']
  //     break
  //   default:
  //     break
  // }
  // cameraFrom.streamTypePush = cameraFrom.streamType
  // mainState.streamTypesPush = mainState.streamTypes
  // streamAddressStr();
  //streamAddressStrPush();
  cameraFrom.isConfig = '1'
  cameraFrom.streamAddress = '';
  if (val === 'other') cameraFrom.isConfig = '0';
  cameraFrom.cameraType = ''; //切换厂商后要清空型号重新选择
};

//切换相机型号时引用模板
const cameraTypeChange = async (val) => {
  if (!val) {
    return;
  }
  cameraFrom.cameraType = val;
  const dataInfo = await queryByDeviceId({id: val});
  cameraFrom.streamFormat = dataInfo.streamFormat ? dataInfo.streamFormat : 'h264';
  cameraFrom.username = dataInfo.username ? dataInfo.username : 'admin';
  cameraFrom.password = dataInfo.password;
  cameraFrom.streamType = dataInfo.streamType;
  cameraFrom.streamTypePush = dataInfo.streamTypesPush
  cameraFrom.streamAddress = dataInfo.protocol;
  cameraFrom.ip = dataInfo.ip ? dataInfo.ip : '192.168.1.100';
  cameraFrom.port = dataInfo.port ? dataInfo.port : 554;
  cameraFrom.channel = dataInfo.channel ? dataInfo.channel : 1
  cameraFrom.cameraTypeName = dataInfo.cameraType;
  //streamAddressStr();
  //streamAddressStrPush();
};

const handleChecked = () => {
  //手动输入不修改配置项
  if (cameraFrom.factory == 'other') {
    cameraFrom.isConfig = '0';
    message.error('手动输入请直接修改视频流地址，无法修改配置项');
  } else {
    cameraFrom.isConfig = cameraFrom?.isConfig === '1' ? '0' : '1';
  }
}

const initCameraModel = (obj: { isUpdate: boolean; datas?: cameraData }) => {
  mainState.isUpdate = obj.isUpdate;
  const datas = obj.datas;
  mainState.item = obj.datas;
  cameraFrom.cameraTypeName = mainState.item?.cameraType_dictText;
  if (datas) {
    if (datas.isConfig == '0') {
      cameraFrom.isConfig = '0';
    }
    mainState.oldOwnerShip = obj.isUpdate ? datas['cameraOwnership'] : '';
    for (let key in datas) {
      cameraFrom[key] = datas[key] || datas[key] === 0 ? datas[key] : undefined;
    }
  }
  mainState.visible = true;
};

const streamTypeItem = (index: number) => {
  cameraFrom.streamType = mainState.streamTypes[index]
  streamAddressStr();
  //streamAddressStrPush();
};

const streamFormatItem = (index: number) => {
  cameraFrom.streamFormat = mainState.streamFormats[index]
  streamAddressStr()
  //streamAddressStrPush()
}

const streamTypePushItem = (index: number) => {
  cameraFrom.streamTypePush = mainState.streamTypesPush[index]
  // streamAddressStr();
  streamAddressStrPush();
};
const streamFormatPushItem = (index: number) => {
  cameraFrom.streamFormatPush = mainState.streamFormatsPush[index];
  // streamAddressStr();
  streamAddressStrPush();
};

const handleSubmit = async () => {
  okButtonProps.loading = true
  try {
    if (mainState.activeKey == 2) {
      await cameraNvr.value.saveData()
      mainState.visible = false;
      emit('handleOk', false);
      return
    }
    await fromRef.value.validate();
    if (cameraFrom.factory == 'other') {
      cameraFrom.isConfig = '0';
      cameraFrom.ip = '';
      cameraFrom.password = '';
      cameraFrom.username = '';
      cameraFrom.port = '';
      cameraFrom.channel = '';
      // cameraFrom.streamType = '';
      cameraFrom.streamFormat = '';
    }
    if (!cameraFrom.streamAddressPush) cameraFrom.streamAddressPush = cameraFrom.streamAddress
    if (!cameraFrom.streamTypePush) cameraFrom.streamTypePush = cameraFrom.streamType
    //cameraFrom.streamAddressPush = cameraFrom.streamAddress
    await saveOrUpdate(cameraFrom, mainState.isUpdate);

    await fromRef.value.clearValidate();
    const deviceId = cameraFrom.cameraOwnership;
    if (mainState.isUpdate) {
      if (deviceId === mainState.oldOwnerShip) {
        await savaAllByDev({devId: deviceId});
      } else {
        await savaAllByDev({devId: mainState.oldOwnerShip});
        setTimeout(() => {
          savaAllByDev({devId: deviceId});
        }, 1000)
      }
    } else {
      await savaAllByDev({devId: deviceId});
    }
    await fromRef.value.resetFields();
    mainState.visible = false;
    emit('handleOk', mainState.isUpdate);
    mainState.activeKey = 0
  } finally {
    okButtonProps.loading = false
  }
};

const handleCancel = async () => {
  await fromRef.value.clearValidate();
  await fromRef.value.resetFields();
  mainState.visible = false;
  cameraFrom.isConfig = '1';
  mainState.activeKey = 0
};

//相机批量导入弹窗
const handleImportCancel = () => {
  mainState.isImportClose = false;
};
const handleShowImport = () => {
  mainState.isImportClose = true;
};

defineExpose({
  initCameraModel,
});

const cameraNameCheck = async (obj: any, value: string) => {
  if (!value) {
    return Promise.reject('请输入相机名称');
  } else if (value.length > 32) {
    return Promise.reject('输入相机名称过长');
  } else {
    const bool = await checkCameraName(mainState.isUpdate ? {
      cameraName: cameraFrom.cameraName,
      id: cameraFrom.id
    } : {cameraName: cameraFrom.cameraName});
    if (!bool) {
      return Promise.reject('输入相机名称重复');
    }
    return Promise.resolve();
  }
};

const factory = async (obj: any, value: string) => {
  if (!cameraFrom.factory) {
    return Promise.reject('请选择厂商');
  } else {
    return Promise.resolve();
  }
};

const cameraOwnership = async (obj: any, value: string) => {
  if (!cameraFrom.cameraOwnership) {
    return Promise.reject('请选择相机归属');
  } else if (!(await checkLicenseHandle(value))) {
    return Promise.reject('设备支持相机数量达到上限');
  } else {
    return Promise.resolve();
  }
};
const cameraNumber = async (obj: any, value: string) => {
  if (!value) {
    return Promise.resolve();
  } else if (!/^[A-Za-z0-9_-]+$/.test(value)) {
    return Promise.reject('只能包含英文字母、数字、短横线(-)、下划线(_)');
  }
};

const checkLicenseHandle = async (value) => {
  return await checkLicense({id: value});
};

const username = (obj: any, value: string) => {
  if (cameraFrom.factory === 'other') {
    return Promise.resolve();
  }
  if (!value) {
    return Promise.reject('请输入用户名');
  } else if (value.length > 32) {
    return Promise.reject('输入用户名过长');
  } else if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(value)) {
    return Promise.reject('以英文字母开头，只能包含英文字母、数字、下划线');
  } else {
    return Promise.resolve();
  }
};

const password = (obj: any, value: string) => {
  if (cameraFrom.factory === 'other') {
    return Promise.resolve();
  }
  if (!value) {
    return Promise.reject('请输入密码');
  } else if (value.length > 16) {
    return Promise.reject('输入密码过长');
  }
    // else if (value.length < 6) {
    //   return Promise.reject('输入密码不能少于6位'); //不做最短长度限制 有些厂商默认密码为5位
  // }
  else if (/[\u4E00-\u9FA5]|[\uFE30-\uFFA0]/g.test(value)) {
    return Promise.reject('密码不能包含中文');
  } else {
    return Promise.resolve();
  }
};
const streamAddress = async (obj: any, value: string) => {
  if (!value) {
    return Promise.reject('请输入码流地址');
  } else {
    return Promise.resolve();
  }
};

const streamAddressPush = (obj: any, value: string) => {
  if (!value) {
    return Promise.reject('请输入码流地址');
  } else {
    return Promise.resolve();
  }
};

const channel = async (obj: any, value: string) => {
  if (cameraFrom.factory === 'other') {
    return Promise.resolve();
  }
  if (!value) {
    return Promise.reject('请输入通道号');
  } else if (!/^[0-9]*$/.test(value)) {
    return Promise.reject('请输入正确通道号');
  } else {
    return Promise.resolve();
  }
};

const cameraType = async (obj: any, value: string) => {
  if (!cameraFrom.cameraType) {
    return Promise.reject('请选择型号');
  } else {
    return Promise.resolve();
  }
};

const ip = async (obj: any, value: string) => {
  if (cameraFrom.factory === 'other') {
    return Promise.resolve();
  }
  if (!value) {
    return Promise.reject('请输入IP地址');
  } else if (!/^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$/.test(value)) {
    return Promise.reject('请输入正确的IP地址');
  } else {
    return Promise.resolve();
  }
};

const port = async (obj: any, value: string) => {
  if (cameraFrom.factory === 'other') {
    return Promise.resolve();
  }
  if (!value) {
    return Promise.reject('请输入端口号');
  } else if (!/^[0-9]*$/.test(value)) {
    return Promise.reject('请输入正确端口号');
  } else {
    return Promise.resolve();
  }
};

//表单校验
const rules = {
  cameraName: [
    {
      required: true,
      validator: cameraNameCheck,
      trigger: 'blur',
    },
  ],
  cameraOwnership: [
    {
      required: true,
      validator: cameraOwnership,
      trigger: 'blur',
    },
  ],
  username: [
    {
      required: true,
      validator: username,
      trigger: 'blur',
    },
  ],
  password: [
    {
      required: true,
      validator: password,
      trigger: 'blur',
    },
  ],
  streamAddress: [
    {
      required: true,
      validator: streamAddress,
      trigger: 'blur',
    },
  ],
  factory: [
    {
      required: true,
      validator: factory,
      trigger: 'blur',
    },
  ],
  cameraType: [
    {
      required: true,
      validator: cameraType,
      trigger: 'blur',
    },
  ],
  cameraNumber: [
    {
      validator: cameraNumber,
      trigger: 'blur',
    },
  ],
  channel: [
    {
      required: true,
      validator: channel,
      trigger: 'blur',
    },
  ],
  ip: [
    {
      required: true,
      validator: ip,
      trigger: 'blur',
    },
  ],
  port: [
    {
      required: true,
      validator: port,
      trigger: 'blur',
    },
  ],
  streamAddressPush: [
    {
      required: true,
      validator: streamAddressPush,
      trigger: 'blur',
    },
  ],
};
</script>

<style lang="less" scoped>
/deep/ .ant-tabs-nav-list {
  position: relative;
  display: flex;
  transition: transform 0.3s;
  left: 37px;
}

.fixStyle {
  font-size: 13px;

  /deep/ .ant-form-item-explain-error {
    //报错字号
    font-size: 13px;
  }

  .cameraStyleSelect {
    /deep/ .ant-select-selector {
      position: relative;
      min-width: 88px;
      width: 120px;
      height: 27px;
      border-radius: 4px;
      border-color: var(--border-color);
    }
  }

  .formClass {
    font-size: 13px;

    .ant-row /deep/ label {
      font-size: 13px;
    }

    .checkbox {
      display: block;
    }

    .ant-input {
      position: relative;
      min-width: 88px;
      width: 160px;
      height: 27px;
      border-radius: 4px;
      border-color: var(--border-color);

      &:hover {
        border-color: #adb4cc;
      }

      &:active {
        border-color: #738ce6;
      }
    }

    .disabled {
      opacity: 0.4;
      pointer-events: none;
    }

    /deep/ .ant-select-selector {
      position: relative;
      width: 160px;
      min-width: 88px;
      height: 27px;
      border-color: var(--border-color);
      border-radius: 4px;

      &:hover {
        border-color: #adb4cc;
      }

      &:active {
        border-color: #738ce6;
      }
    }

    .ant-form-item-label > label {
      font-size: 2px;
    }

    .divider {
      width: 585px;
    }

    .ant-input-password {
      position: relative;
      min-width: 88px;
      width: 325px;
      height: 27px;
      border-radius: 4px;
      border-color: var(--border-color);
      margin-right: 30px !important;

      &:hover {
        border-color: #adb4cc;
      }

      &:active {
        border-color: #738ce6;
      }
    }

    .cameraNumberStyle {
      /deep/ .ant-select-selector {
        width: 240px;
        margin-left: 20px;
      }
    }
  }

  .cameraTypeStyle {
    /deep/ .ant-select-selector {
      width: 330px;
    }
  }

  .tab-control {
    display: flex;
    text-align: center;
    height: 27px;
    line-height: 24px;
    background-color: var(--background-Color);
    border: 2px solid;
    border-color: var(--netural-color);
    border-radius: 4px;
    width: 160px;
    // flex: 1;
    //margin-left: 24px;
    &-item {
      width: 160px;
      cursor: pointer;
      background: var(--netural-color);

      span {
        font-family: Source Han Sans CN;
        //color: var(--secondary-text-color);
        color: var(--cancel-text-color);
        font-size: 12px;
      }
    }

    .active {
      //background: var(--netural-color);
      background-color: var(--background-Color);
      width: 160px;

      span {
        font-family: Source Han Sans CN;
        //color: #8a8d99;
        color: var(--secondary-text-color);
        font-size: 12px;
      }
    }
  }
}

:deep .ant-modal-content {
  //width: 600px;
  position: relative;
}

.import {
  position: absolute;
  bottom: 10px;
  left: 36px;
  cursor: pointer;

  .import-icon {
    display: inline-block;
    background: url('/@/assets/svg/icon/editor_btn_import_hover.svg');
    width: 14px;
    height: 14px;
  }

  .import-btn {
    font-family: Source Han Sans CN;
    color: var(--primary-Color);
    font-size: 15px;
  }
}

:deep .ant-modal-footer {
  .ant-btn {
    width: 64px;
    height: 24px;
    background-color: var(--fill-color);
    border-radius: 4px;
    color: var(--secondary-text-color);
    display: flex;
    align-items: center;
    cursor: pointer;

    &:hover {
      background-color: var(--fill-two-color);
      color: var(--primary-Color);
    }

    &:active {
      background-color: var(--input-text-color);
      border-color: #7089ed;
      color: var(--primary-Color-actived);
    }
  }
}

:deep .ant-modal-footer {
  display: flex;
  justify-content: flex-end;
}

:deep .ant-modal-footer {
  .ant-btn-primary {
    width: 80px;
    height: 24px;
    background-color: var(--primary-Color);
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 4px;
    color: rgba(255, 255, 255, 0.95);
    border: none;
    cursor: pointer;

    &:hover {
      background-color: var(--primary-Color-hover);
      color: rgba(255, 255, 255, 0.95);
    }

    &:active {
      background-color: var(--primary-Color-actived);
    }

    &:disabled {
      background-color: var(--fill-color);
    }
  }
}

:deep .ant-modal-content {
  //弹窗 X位置
  .ant-modal-close {
    margin-right: 12px;
  }
}
</style>
