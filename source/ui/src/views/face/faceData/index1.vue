<template>
  <div class="rootContainer" ref="rootContainer">
    <div style="display: flex; flex-direction: column; height: 100%;">
      <a-card :bordered="false" :body-style="{margin: '10px', padding: 0 }" size="small">
        <a-row :gutter="[0, 10]">
          <a-col :span="24">
            <a-form layout="inline" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
              <a-form-item label="相机">
                <a-select placeholder="请选择摄像头" style="width: 100%"></a-select>
              </a-form-item>
              <a-form-item label="姓名">
                <a-input placeholder="请输入姓名"/>
              </a-form-item>
              <a-form-item label="证件号">
                <a-input placeholder="请输入证件号"/>
              </a-form-item>
              <a-form-item label="日期">
                <a-range-picker placeholder="请选择时间范围" />
              </a-form-item>
              <a-form-item>
                <a-button type="primary">
                  <search-outlined/>
                  搜索</a-button>
              </a-form-item>
              <a-form-item>
                <a-button type="primary"><rest-outlined/>重置</a-button>
              </a-form-item>
            </a-form>
          </a-col>
          <a-col :span="24">
            <a-checkbox class="marginR10">本页全选</a-checkbox>
            <a-button type="primary" class="marginR10" @click="addFaceGroup">
              <export-outlined/>
              导出</a-button>
            <a-button type="primary" @click="addFaceGroup" class="marginR10">
              <import-outlined/>
              导入到人脸库</a-button>
            <a-button type="danger" @click="">
              <delete-outlined/>
              批量删除</a-button>

            <div style="float: right">
              <a-radio-group>
                <a-radio-button value="1">
                  <picture-outlined />
                  大图 </a-radio-button>
                <a-radio-button value="2">
                  <ordered-list-outlined/>
                  列表</a-radio-button>
              </a-radio-group>
            </div>
          </a-col>
        </a-row>
      </a-card>
      <div style="flex: 1">
        <a-card class="height100" :body-style="{margin: '10px', padding: 0, height: '100%' }"
                size="small">
          <div class="content">
              <div class="contentItem">
                <div class="itemImg">
                  <img class="imgClass" src="http://192.168.1.180:9980//face/group20250319103049/159cd8604961418289a7fcda934914ee.jpg" alt="人脸图片"/>
                  <input type="checkbox" class="imgCheckbox"/>
                  <img class="thumbImg" src="http://192.168.1.180:9980//face/group20250319103049/159cd8604961418289a7fcda934914ee.jpg" alt="人脸图片"/>
                  <div class="faceName">张老三</div>
                </div>
                <div class="itemFooter">
                  <div class="info">
                    <span>相机（1）</span>
                    <span>2025-3-21 13:38:17</span>
                  </div>
                  <div class="actionBtn">
                    <a href="javascript:void(0)">导入人脸库</a>
                    <a href="javascript:void(0)" @click="openDetail">详 情</a>
                    <a href="javascript:void(0)" class="errorColor">删 除</a>
                  </div>
                </div>
              </div>
          </div>
        </a-card>
      </div>
    </div>

    <face-alarm-detail ref="faceAlarmDetail" :get-container="getContainer"/>
  </div>
</template>

<script setup lang="ts">
import { SearchOutlined,
  RestOutlined,
  ExportOutlined,
  ImportOutlined,
  DeleteOutlined,
  PictureOutlined, OrderedListOutlined } from "@ant-design/icons-vue";

import FaceAlarmDetail from "/@/views/face/faceData/FaceAlarmDetail.vue";
import {ref} from "vue";

const rootContainer = ref()
const getContainer = () => {
  return rootContainer.value
};

const faceAlarmDetail = ref()
const openDetail = () => {
  faceAlarmDetail.value.open()
}
</script>

<style scoped lang="less">

.errorColor{
  color: var(--serious-text-color);
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

.rootContainer {
  height: calc(100vh - 50px);
  overflow: hidden;

  :deep(.ant-modal-close) {
    margin-right: 10px;
  }

  .content {
    //height: var(--height);
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 15px;
    padding: 0;
    margin: 0;
    box-sizing: border-box;

    .contentItem {
      display: flex;
      flex-direction: column;
      align-items: center;
      background: rgba(0, 0, 0, 0.05);
      padding: 10px 0;
      border-radius: 10px;

      &:hover {
        background: rgba(0, 0, 0, 0.1);
      }

      .itemImg {
        position: relative;

        .imgClass {
          min-width: 250px;
          min-height: 180px;
          height: 180px;
          object-fit: fill;
          border-radius: 10px;
        }

        .imgCheckbox {
          position: absolute;
          top: 10px;
          left: 10px;
          // 放大
          transform: scale(1.3);
        }

        .thumbImg {
          position: absolute;
          min-width: 70px;
          width: 70px;
          min-height: 75px;
          height: 75px;
          object-fit: fill;
          top: 0;
          right: 0;
          border-top-right-radius: 10px;
          border-bottom-left-radius: 5px;
          //transform: scale(0.2);
        }

        .faceName {
          position: absolute;
          color: white;
          font-size: 12px;
          background: rgba(0, 0, 0, 0.5);
          top: 56px;
          right: 0;
          max-width: 70px;
          width: 70px;
          border-bottom-left-radius: 5px;
          text-align: center;
          padding-left: 2px;
          // 超出省略号
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

      }

      .itemFooter {
        width: 100%;
        margin-top: 10px;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        align-items: center;

        .info {
          display: flex;
          justify-content: space-between;
          width: 100%;
          margin-bottom: 5px;
          padding: 0 15px;
        }

        .actionBtn {
          display: flex;
          justify-content: space-between;
          width: 100%;
          padding: 0 15px;
        }
      }
    }
  }
}
</style>
