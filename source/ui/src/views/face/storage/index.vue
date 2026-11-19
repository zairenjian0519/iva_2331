<template>
  <div class="rootContainer" ref="faceContainer">
    <a-row class="height100">
      <a-col :span="5">
        <a-card class="height100" :body-style="{margin: 0, padding: 0 }" size="small">
          <template #title>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span class="font16">人员组</span>
              <div>
                <a-button type="primary" class="marginR10" @click="addFaceGroup">
                  <plus-outlined/>
                  新建
                </a-button>
                <a-button type="primary" @click="editFaceGroup">
                  <edit-outlined/>
                  修改
                </a-button>
              </div>
            </div>
          </template>
          <div class="group">
            <a-button v-for="item in faceGroupArr"
                      :type="groupBtnKey==item.id? 'primary' : 'dashed'"
                      class="width100"
                      :key="item.id"
                      @click="loadFaces(item.id)">
              {{ item.groupName }}
            </a-button>
            <a-button v-if="faceGroupArr.length <= 0" type="primary" class="width100" @click="initGroup"
                      :loading="initGroupBtnLoading">
              <reload-outlined v-if="!initGroupBtnLoading"/>
              初始化人员库
            </a-button>
            <a-button v-if="faceGroupArr.length > 0" class="width100" @click="initGroupFace" :loading="initGroupFaceBtnLoading">
              <reload-outlined v-if="!initGroupFaceBtnLoading"/>
              {{ initGroupFaceBtnLoading? '正在初始化' : '初始化人脸数据' }}
            </a-button>
            <a-button type="danger" class="width100" @click="deleteFaceGroup">
              <delete-outlined/>
              删除人员库
            </a-button>
          </div>
        </a-card>
      </a-col>
      <a-col :span="19">
        <a-card class="height100" size="small" :body-style="{margin: 0, padding: 0 }">
          <template #title>
            <div class="rightHeadBtns">
              <span class="font16">人脸库</span>
              <a-button type="primary" @click="addFaceImg">
                <upload-outlined/>
                上传人脸
              </a-button>
            </div>
          </template>
          <div class="groupDetailHeader">
            <a-form ref="searchForm" :model="searchFormState" layout="inline">
              <a-row :gutter="[5, 5]">
                <a-col :span="3">
                  <a-form-item :label-col="{span: 0}">
                    <div style="margin-left: 10px">
                      <a-checkbox @change="pageSelectChecked">本页全选</a-checkbox>
                    </div>
                  </a-form-item>
                </a-col>
                <a-col :span="4">
                  <a-form-item label="姓名" name="faceName" :label-col="{span: 5}">
                    <a-input v-model:value="searchFormState.faceName"
                             placeholder="姓名，支持模糊匹配"/>
                  </a-form-item>
                </a-col>
                <a-col :span="5">
                  <a-form-item label="证件号码" name="faceIdCard" :label-col="{span: 8}">
                    <a-input v-model:value="searchFormState.faceIdCard" placeholder="支持模糊匹配"/>
                  </a-form-item>
                </a-col>
                <a-col :span="4">
                  <a-form-item label="手机号码" name="facePhone" :label-col="{span: 8}">
                    <a-input v-model:value="searchFormState.facePhone" placeholder="支持模糊匹配"/>
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item :label-col="{span: 0}">
                    <div style="display: inline-flex; justify-content: space-around">
                      <a-button type="primary"
                                @click="searchFace"
                                class="marginR10"
                                :loading="searchBtnState.searchBtnLoading">
                        <search-outlined v-if="!searchBtnState.searchBtnLoading"/>
                        搜索
                      </a-button>
                      <a-button type="primary"
                                @click="exportFaceData"
                                class="marginR10">
                        <export-outlined/>
                        导出
                      </a-button>
                      <a-button type="primary"
                                @click="importFaceData"
                                class="marginR10">
                        导入
                      </a-button>
                      <a-button type="danger"
                                @click="batchDelFaces"
                                :loading="batchDelBtnLoading">
                        <delete-outlined/>
                        批量删除
                      </a-button>
                    </div>
                  </a-form-item>
                </a-col>
              </a-row>

            </a-form>
          </div>
          <div class="groupDetail">
            <div v-for="item in faceDetailArr" :key="item.id">
              <div class="gridItem">
                <img alt="人脸图片" :src="item.imgUrl" @click="toggleCheckbox(item.id)"/>
                <input type="checkbox"
                       class="imgCheckbox"
                       :value="item.id"
                       :checked="selectedItems.includes(item.id)"
                       @change="toggleCheckbox(item.id)"/>
              </div>
              <div class="imgFooter">
                <a-button type="link"
                          size="small"
                          class="nameBtn"
                          @click="editFaceDetail(item)">{{ item.faceName }}
                </a-button>
                <a-button type="link" size="small"
                          class="sideBtn"
                          :loading="deleteItemBtnLoading"
                          @click="deleteItem(item.id)">删除
                </a-button>
              </div>
              <div class="footerIdCard">
                <a-tooltip :title="item.faceIdCard" placement="top">
                  证件号：{{ item.faceIdCard ? item.faceIdCard : '暂未提供' }}
                </a-tooltip>
              </div>
            </div>
          </div>
          <div class="footerPagination">
            <a-pagination v-model:current="pageState.pageNo"
                          v-model:page-size="pageState.pageSize"
                          :show-quick-jumper="true"
                          :show-size-changer="true"
                          :total="pageState.total"
                          :show-total="total => `总 ${total} 条`"
                          :page-size-options="pageState.pageSizeOptions"
                          @change="handlePageChange"/>
          </div>
        </a-card>
      </a-col>
    </a-row>
    <a-modal
      :body-style="{ margin: 0, marginTop: '10px', padding: 0 }"
      :title="title"
      v-model:visible="faceGroupModalVisible"
      :get-container="getFaceContainer"
      :ok-button-props="{ loading: groupBtnState.addBtnLoading }"
      @ok="submitFaceGroup">

      <a-form ref="groupForm"
              :model="formState"
              :rules="groupRules"
              :label-col="{ span: 5 }"
              :wrapper-col="{ span: 16 }">
        <a-form-item label="人员库名称" name="groupName">
          <a-input v-model:value="formState.groupName" placeholder="请输入人员库名称"/>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      :body-style="{margin: 0 , marginTop: '10px', padding: 0 }"
      :title="isFaceDetailUpdate? '修改人脸信息' : '新增人脸信息'"
      v-model:visible="faceImgModalVisible"
      :get-container="getFaceContainer"
      ok-text="上传人脸"
      :confirm-loading="faceBtnState.uploadBtnLoading"
      @ok="submitFaceImg">
      <a-form ref="faceForm" :model="faceFormState" :rules="faceFormRules" :label-col="{span: 7}"
              :wrapper-col="{span: 11}">
        <a-form-item label="上传人脸">
          <a-upload
            v-model:file-list="fileList"
            list-type="picture-card"
            :multiple="false"
            :max-count="1"
            :before-upload="beforeUpload"
            @remove="handleRemove"
            @preview="handlePreview"
          >
            <div v-if="fileList.length < 1">
              <plus-outlined/>
              <div class="ant-upload-text">上传人脸</div>
            </div>
          </a-upload>
        </a-form-item>
        <a-form-item label="姓名" name="faceName">
          <a-input v-model:value="faceFormState.faceName" placeholder="请输入人员姓名"/>
        </a-form-item>
        <a-form-item label="手机号" name="facePhone">
          <a-input v-model:value="faceFormState.facePhone" placeholder="请输入人员手机号"/>
        </a-form-item>
        <a-form-item label="证件号" name="faceIdCard">
          <a-input v-model:value="faceFormState.faceIdCard" placeholder="请输入人员证件号"/>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-input v-model:value="faceFormState.remark" placeholder="请输入备注"/>
        </a-form-item>
      </a-form>
      <p style="text-align: center">
        注意：仅支持<code style="color: red;"> jpg/png </code>格式图片，图片大小不超过<code
        style="color: red"> 2M </code>
      </p>
    </a-modal>

    <export-modal ref="exportModal" :get-container="getFaceContainer"/>
    <import-modal ref="importFaceDataModal" :get-container="getFaceContainer"
                  @refresh="refreshPageData" @init-face-group="initFaceGroupData"/>
  </div>
</template>

<script setup lang="ts">
import {
  DeleteOutlined,
  EditOutlined,
  ExportOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'
import {onMounted, onUnmounted, reactive, ref, unref} from "vue";
import {
  add,
  deleteRow,
  getList,
  getObj,
  initDefaultGroup,
  initFaceGroup,
  update
} from "/@/views/face/storage/group.api";
import {
  deleteLocalImg,
  faceDetailAdd,
  faceDetailBatchDelete,
  faceDetailDelete,
  faceDetailUpdate,
  listByGroupId, syncBaiduYun
} from "/@/views/face/storage/detail.api";
import {useMessage} from "/@/hooks/web/useMessage";
import {UploadProps} from "ant-design-vue";
import ExportModal from "/@/views/face/storage/ExportModal.vue";
import ImportModal from "/@/views/face/storage/ImportModal.vue";
import {connectWebSocket, offWebSocket, onWebSocket} from "/@/hooks/web/useWebSocketnew";
import {useUserStore} from "/@/store/modules/user";

const faceContainer = ref()
const getFaceContainer = () => {
  return faceContainer.value
}

const pageState = reactive({
  pageSize: 10,
  pageNo: 1,
  total: 0,
  pageSizeOptions: ['10', '20', '30', '40', '50']
})

// 人脸组
const title = ref('新增人员组')
const faceGroupModalVisible = ref(false)
const isFaceGroupUpdate = ref(false)
const groupForm = ref()
const faceGroupArr = ref([])
const groupBtnKey = ref("")
const groupBtnState = reactive({
  addBtnLoading: false,
  delBtnLoading: false
})

const groupRules = {
  groupName: [{required: true, message: '请输入人员组名称'}]
}

const formState = reactive({
  groupName: '',
})

const addFaceGroup = () => {
  isFaceGroupUpdate.value = false
  title.value = '新增人员组'
  faceGroupModalVisible.value = true
}

const editFaceGroup = async () => {
  if (!groupBtnKey.value) {
    useMessage().createMessage.warn("请选择人员组")
    return
  }
  try {
    const groupItem = await getObj({id: groupBtnKey.value})
    if ("默认人员组" === groupItem.groupName) {
      useMessage().createMessage.warn("默认人员组不能编辑")
      return
    }
    formState.groupName = groupItem.groupName
    title.value = '编辑人员组'
    isFaceGroupUpdate.value = true
    faceGroupModalVisible.value = true
  } catch (err) {
    console.log(err)
  }
}

const getFaceGroups = async () => {
  try {
    faceGroupArr.value = await getList()
  } catch (err) {
    useMessage().createMessage.error("获取人员组信息失败")
  }

}

const submitFaceGroup = async () => {
  const valid = groupForm.value.validate()
  if (!valid) return
  try {
    groupBtnState.addBtnLoading = true
    if (isFaceGroupUpdate.value) {
      await update({id: groupBtnKey.value, groupName: formState.groupName})
    } else {
      await add(formState)
    }
    await getFaceGroups()
    faceGroupModalVisible.value = false
    groupForm.value.resetFields()
  } catch (err) {
    console.log(err)
  } finally {
    groupBtnState.addBtnLoading = false
  }
}

const faceDetailArr = ref([])
const loadFaces = (id: string) => {
  groupBtnKey.value = id
  // 查询该分组人脸照片集合
  const params = {
    groupId: id,
    pageNo: pageState.pageNo,
    pageSize: pageState.pageSize
  }
  getPageData(params)
}

const refreshPageData = () => {
  const params = {
    groupId: groupBtnKey.value,
    pageNo: pageState.pageNo,
    pageSize: pageState.pageSize
  }
  getPageData(params)
}

const getPageData = async (params: any) => {
  listByGroupId(params).then(data => {
    if (!data || data.records.length == 0) {
      faceDetailArr.value = []
    } else {
      const rows = data.records
      rows.forEach(item => {
        item.imgUrl = 'http://' + window.location.hostname + ":9980/" + item.imgUrl
      })
      faceDetailArr.value = rows
      pageState.total = data.total
      pageState.pageNo = data.current
      pageState.pageSize = data.size
    }
  })
}

const initGroupBtnLoading = ref(false)
const initGroup = async ()=> {
  // const response = await projectKey();
  // let projectCode = response.projectKey;
  // if (!projectCode) {
  //   useMessage().createMessage.error("请在设备管理中设置项目编号")
  //   return
  // }
  initGroupBtnLoading.value = true
  try {
    await initDefaultGroup()
    await getFaceGroups() // 刷新组信息
  }finally {
    initGroupBtnLoading.value = false
  }
}

const initFaceGroupData = () => {
  if (groupBtnKey.value) {
    initFaceGroup({faceGroupId: groupBtnKey.value})
  }
}

const deleteFaceGroup = async () => {
  if (groupBtnKey.value) {
    // const groupItem = await getObj({id: groupBtnKey.value})
    // if ("默认人员组" === groupItem.groupName) {
    //   useMessage().createMessage.warn("默认人员组不能删除")
    //   return
    // }
    useMessage().createConfirm({
      iconType: 'warning',
      title: '删除提示',
      content: '确认删除该组文件及信息？',
      okButtonProps: {
        loading: groupBtnState.delBtnLoading
      },
      onOk: async () => {
        try {
          groupBtnState.delBtnLoading = true
          await deleteRow({id: groupBtnKey.value})
          await getFaceGroups()
          groupBtnKey.value = ''
          faceDetailArr.value = []
        } finally {
          groupBtnState.delBtnLoading = false
        }
      }
    })
  } else {
    useMessage().createMessage.error("请选择人员组信息")
  }
}

// 人脸图片
const faceImgModalVisible = ref(false)
const fileList = ref<UploadProps['fileList']>([])
const faceForm = ref()
const faceBtnState = reactive({
  uploadBtnLoading: false
})

const faceFormState = reactive({
  faceName: '',
  facePhone: '',
  faceIdCard: '',
  remark: '',
})

const faceFormRules = {
  faceName: [
    {
      required: true,
      validator: (_, value) => {
        const nameRegex = /^[\u4e00-\u9fa5a-zA-Z]{2,20}$/;
        if (!value) {
          return Promise.reject('请输入人员姓名');
        }
        if (!nameRegex.test(value)) {
          return Promise.reject('姓名格式错误，仅支持中文或英文字母，长度 2-20');
        }
        return Promise.resolve();
      }
    }
  ],
  facePhone: [
    {
      validator: (_, value) => {
        const phoneRegex = /^1[3-9]\d{9}$/;
        if (value && !phoneRegex.test(value)) {
          return Promise.reject('手机号格式错误，请输入 11 位有效手机号码');
        }
        return Promise.resolve();
      }
    }
  ],
  faceIdCard: [
    {
      validator: (_, value) => {
        const idCardRegex = /^\d{15}$|^\d{18}$|^\d{17}[\dXx]$/;
        if (value && !idCardRegex.test(value)) {
          return Promise.reject('身份证号码格式不正确');
        }
        return Promise.resolve();
      }
    }
  ],
  remark: [
    {max: 50, message: '最多输入 50 个字符'}
  ]
}

const handleRemove: UploadProps['onRemove'] = file => {
  const index = fileList.value.indexOf(file);
  const newFileList = fileList.value.slice();
  newFileList.splice(index, 1);
  fileList.value = newFileList;
  if (currentFaceDetail.id) {
    // 删除磁盘上的图片文件
    deleteLocalImg({id: currentFaceDetail.id})
  }
}

const handlePreview: UploadProps['onPreview'] = file => {
  if (file.originFileObj) {
    const blobUrl = URL.createObjectURL(file.originFileObj);
    window.open(blobUrl, '_blank'); // 直接打开 Blob URL
  } else if (file.url) {
    window.open(file.url, '_blank')
  } else {
    useMessage().createMessage.error("图片加载失败")
  }
};

const beforeUpload = (file: UploadProps['fileList'][number]) => {
  fileList.value = [...fileList.value, file];
  return false;
}

const checkPic = () => {
  if (fileList.value.length < 1) {
    useMessage().createMessage.error("请选择图片")
    return false
  }
  const file = fileList.value[0]
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
  if (!isJpgOrPng) {
    useMessage().createMessage.error('仅支持 jpg 和 png 格式的照片!');
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) {
    useMessage().createMessage.error('图片必须小于 2MB!');
    return false
  }
  return true
}

const resetFaceDetail = () => {
  fileList.value = []
  faceFormState.faceName = ''
  faceFormState.facePhone = ''
  faceFormState.faceIdCard = ''
  faceFormState.remark = ''
  // faceForm.value.resetFields()
}

const isFaceDetailUpdate = ref(false)
const addFaceImg = () => {
  if (!groupBtnKey.value) {
    useMessage().createMessage.warn("请选择人员组")
    return
  }
  resetFaceDetail()
  isFaceDetailUpdate.value = false
  faceImgModalVisible.value = true
}

let currentFaceDetail
const editFaceDetail = (params: any) => {
  console.log(params)
  fileList.value = [
    {
      uid: params.id,
      name: params.faceName,
      status: 'done',
      url: params.imgUrl,
    }
  ]
  faceFormState.faceName = params.faceName
  faceFormState.facePhone = params.facePhone
  faceFormState.faceIdCard = params.faceIdCard
  faceFormState.remark = params.remark
  currentFaceDetail = params
  isFaceDetailUpdate.value = true
  faceImgModalVisible.value = true
}

const submitFaceImg = async () => {
  if (isFaceDetailUpdate.value && fileList.value.length === 0) {
    useMessage().createMessage.error("请上传图片")
    return
  }

  const shouldCheck = isFaceDetailUpdate.value
    ? !!fileList.value[0]?.originFileObj  // 有上传文件时才校验
    : true                                // 非详情模式强制校验

  if (shouldCheck && !checkPic()) return

  const valid = await faceForm.value.validate()
  if (!valid) return
  try {
    const params: any = {
      file: fileList.value[0].originFileObj,
      data: {
        faceGroup: groupBtnKey.value,
        faceName: faceFormState.faceName,
        facePhone: faceFormState.facePhone,
        faceIdCard: faceFormState.faceIdCard,
        remark: faceFormState.remark
      }
    }
    faceBtnState.uploadBtnLoading = true
    if (isFaceDetailUpdate.value) {
      params.data.id = currentFaceDetail.id
      await faceDetailUpdate(params)
    } else {
      await faceDetailAdd(params)
    }
    resetFaceDetail()
    refreshPageData()
    initFaceGroupData()
  } catch (err) {
    console.log(err)
  } finally {
    faceBtnState.uploadBtnLoading = false
    faceImgModalVisible.value = false
  }
}

// 响应式数组，用于存储选中的 checkbox id
const selectedItems = ref<string[]>([])
// 全选
const pageSelectChecked = (e: any) => {
  if (e.target.checked) {
    selectedItems.value = faceDetailArr.value.map(item => item.id);
  } else {
    selectedItems.value = [];
  }
}

// 切换 checkbox 状态的函数
const toggleCheckbox = (id: string) => {
  const index = selectedItems.value.indexOf(id);
  if (index === -1) {
    selectedItems.value.push(id); // 选中
  } else {
    selectedItems.value.splice(index, 1); // 取消选中
  }
}

const deleteItemBtnLoading = ref(false)
const deleteItem = async (id: string) => {
  useMessage().createConfirm({
    iconType: 'warning',
    title: '警告',
    content: '删除后不可恢复，确定删除该人员吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        deleteItemBtnLoading.value = true
        await faceDetailDelete({id: id})
        refreshPageData()
      } finally {
        deleteItemBtnLoading.value = false
      }
    }
  })
}

const batchDelBtnLoading = ref(false)
const batchDelFaces = async () => {
  if (!selectedItems || selectedItems.value.length === 0) {
    useMessage().createMessage.error("请选择人员")
    return
  }
  useMessage().createConfirm({
    iconType: 'warning',
    title: '警告',
    content: '删除后不可恢复，确定删除选中人员吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await faceDetailBatchDelete({ids: selectedItems.value.join(',')})
        refreshPageData()
        initFaceGroupData()
      } finally {
        batchDelBtnLoading.value = false
      }
    }
  })
}

// 搜索
const searchFormState = reactive({
  faceName: '',
  facePhone: '',
  faceIdCard: ''
})

const searchBtnState = reactive({
  searchBtnLoading: false
})
const searchFace = () => {
  if (!groupBtnKey.value) {
    useMessage().createMessage.warn("请选择人员组")
    return
  }
  const params = {
    groupId: groupBtnKey.value,
    pageNo: pageState.pageNo,
    pageSize: pageState.pageSize,
    faceName: searchFormState.faceName,
    facePhone: searchFormState.facePhone,
    faceIdCard: searchFormState.faceIdCard
  }
  searchBtnState.searchBtnLoading = true
  getPageData(params).finally(() => {
    searchBtnState.searchBtnLoading = false
  })
}

const exportModal = ref()
const exportFaceData = () => {
  if (!groupBtnKey.value) {
    useMessage().createMessage.warn("请选择人员组")
    return
  }
  exportModal.value.open(groupBtnKey.value)
}

const importFaceDataModal = ref()
const importFaceData = () => {
  if (!groupBtnKey.value) {
    useMessage().createMessage.warn("请选择人员组")
    return
  }
  importFaceDataModal.value.open(groupBtnKey.value)
}

// 翻页
const handlePageChange = (pageNo: number, pageSize: number) => {
  // console.log(pageNo, pageSize)
  pageState.pageNo = pageNo
  pageState.pageSize = pageSize
  refreshPageData()
}

const initGroupFaceBtnLoading = ref(false)
const initGroupFace = async ()=> {
  if (!groupBtnKey.value) {
    useMessage().createMessage.warn("请选择人员组")
    return
  }
  initGroupFaceBtnLoading.value = true
  await syncBaiduYun({groupKey: groupBtnKey.value});
}

const userStore = useUserStore();
const initWebsocket = () => {
  let userId = unref(userStore.getUserInfo).id;
  const index = window.location.host.indexOf(':');
  const hostIp = window.location.host.slice(0, index);
  const faceSyncWebSocket = `ws://${hostIp}:8088/kyscene` + '/faceSyncWebSocket/' + userId
  connectWebSocket(faceSyncWebSocket);
}

const onWebSocketMessage = (msg:any) => {
  // console.log('faceSyncWebSocket', msg)
  if (!msg) return;
  if(initGroupFaceBtnLoading.value && !msg.SyncState) {
    useMessage().createMessage.success("初始化结束")
  }
  initGroupFaceBtnLoading.value = msg.SyncState

}

onMounted(() => {
  getFaceGroups()
  initWebsocket()
  onWebSocket(onWebSocketMessage);
})

onUnmounted(() => {
  offWebSocket(onWebSocketMessage)
})
</script>

<style scoped lang="less">
.rootContainer {
  :deep(.ant-modal-close) {
    margin-right: 10px;
  }
}

.width100 {
  width: 100%;
}

.height100 {
  height: 100%;
}

.marginR10 {
  margin-right: 10px;
}

.font16 {
  font-size: 16px;
}

.rootContainer {
  height: 100%;
  overflow-y: hidden;

  .group {
    padding: 10px;
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    width: 100%;
    height: 100%;
    overflow-y: auto;
    overflow-x: hidden;
  }

  .groupDetail {
    height: 100%;
    padding: 10px;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 15px;

    .gridItem {
      position: relative;
      display: inline-flex;

      img {
        width: 120px;
        min-width: 120px;
        height: 172px;
        min-height: 172px;
        display: block;
      }

      .imgCheckbox {
        position: absolute;
        top: 5px;
        right: 5px;
        z-index: 10;
        transform: scale(1.2);
        border: none;
      }
    }

    .imgFooter {
      width: 100%;
      display: inline-flex;
      justify-content: space-between;
      align-items: center;
      padding-right: 13px;

      .nameBtn {
        padding: 0;
        color: var(--primary-Color);
      }

      .sideBtn {
        padding: 0;
        color: var(--serious-text-color);
      }
    }

    .footerIdCard {
      width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      padding-right: 13px;
    }
  }

  .footerPagination {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background-color: white;
    padding: 10px 0;
    text-align: center;
    border-top: 1px solid #e8e8e8;
    z-index: 10;
  }

  .rightHeadBtns {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .groupDetailHeader {
    width: 100%;
    height: 50px;
    padding-top: 10px;
    border-bottom: var(--border-color) dashed 1px
  }
}
</style>
