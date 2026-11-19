<template>
  <div class="alarm-detail">
    <alarm-header @handle-alarm-search="handleAlarmSearch" @handle-alarm-reset="handleAlarmReset" />
    <div class="bottom">
      <alarm-search
        :is-show-table="mainState.showTable"
        @handle-show-table="handleShowTable"
        :all-count="mainState.allCount"
        :common-count="mainState.commonCount"
        :serious-count="mainState.seriousCount"
        :remind-count="mainState.remindCount"
        :warning-count="mainState.warningCount"
        :normal-count="mainState.normalCount"
        @click-audit="handleClickAudit"
        @click-level="handleClickLevel"
        @handle-export="handleExport"
        @handle-batch-delete="handleBatchDelete"
        @clear-all-alarm="deleteAllAlarm"
      />
      <template v-if="mainState.showTable">
        <template v-if="mainState.isTableLoading && !mainState.field && !mainState.order">
          <div class="loading"> <img src="/@/assets/svg/icon/loading_onwhite.gif" class="loading-image" alt="" /></div>
        </template>
        <template v-else>
          <alarm-table
            :table-data="mainState.tableData"
            @handle-sort-change="handleSortChange"
            @selected-rows="handleSelectedRows"
            @handle-table-audit="handleTableAudit"
            @handle-table-delete="handleTableDelete"
          >
            <div class="pagination"
              ><base-pagination
                :total="mainState.total"
                :page-size-options="['10', '20', '40', '100']"
                background="#738ce6"
                @handle-change="handleChange"
                @handle-show-size-change="handleShowSizeChange"
                :default-page-size="10"
                :page-size="mainState.pageSize"
                :current="mainState.currentPage"
            /></div>
          </alarm-table>
        </template>
      </template>
      <template v-else>
        <template v-if="!mainState.isCardLoading && mainState.cardTotal === 0">
          <a-empty description="暂无数据" />
        </template>
        <template v-else-if="mainState.isCardLoading">
          <div class="loading"> <img src="/@/assets/svg/icon/loading_onwhite.gif" alt="" class="loading-image" /></div>
        </template>

        <template v-else>
          <alarm-card :card-list="mainState.cardData" @handle-selected="handleSelected" :total="mainState.total" @handle-card-audit="handleCardAudit" @handle-card-delete="handleCardDelete">
            <div class="pagination">
              <base-pagination
                :total="mainState.cardTotal"
                :page-size-options="['20', '40', '100']"
                background="#738ce6"
                @handle-change="handleCardChange"
                @handle-show-size-change="handleCardShowSizeChange"
                :default-page-size="20"
                :page-size="mainState.pageCardSize"
                :current="mainState.currentCardPage"
                class="page"
            /></div>
          </alarm-card>
        </template>
      </template>
    </div>
    <alarm-modal
      v-model:visible="mainState.isShowModal"
      @val="showModal"
      @handle-ok="handleOk"
      @show-big-img="goImg"
      :index="mainState.curIndex"
      @handle-cancel="handleCancel"
      :detail-info="mainState.vertifyInfo"
      :detail-list="mainState.detailList"
      @handle-back="handleBack"
      @handle-commit="handleCommit"
      @go-next="handleGoNext"
      @go-prev="handleGoPrev"
      @handle-filter-serious="handleFilterSerious"
    />
  </div>
</template>
<script lang="ts" setup>
import {onMounted, reactive} from 'vue';
import AlarmHeader from './components/AlarmHeader.vue';
import AlarmCard from './components/AlarmCard.vue';
import AlarmSearch from './components/AlarmSearch.vue';
import AlarmTable from './components/AlarmTable.vue';
import {
  auditsResult,
  batchDelete,
  clearAlarmByManual,
  deleteOne,
  getAlarmCount,
  list
} from './AlarmDetails.api';
import {useMessage} from '/@/hooks/web/useMessage';

import {AuditResult, AuditStatus, searchParams, tableItem} from './AlarmDetails.data';
import AlarmModal from '/@/components/AlarmModal/AlarmModal.vue';
import {useMethods} from '/@/hooks/system/useMethods';
import BasePagination from '/@/components/BasePagination/BasePagination.vue';
import {timeFormat} from '/@/utils/date/dateFormat';
import dayjs from 'dayjs';

const { handleExportXls } = useMethods();
  const { createMessage, createConfirm } = useMessage();
  const dateTime = new Date();
  const mainState = reactive({
    showTable: false,
    tableData: [] as tableItem[],
    detailList: [] as tableItem[],
    cardData: [] as tableItem[],
    currentPage: 1 as number,
    currentCardPage: 1 as number,
    pageSize: 10 as number,
    pageCardSize: 20 as number,
    total: 0 as number,
    cardTotal: 0 as number,
    searchContion: {
      startTime: timeFormat(dayjs(dateTime).subtract(7, 'day')),
      endTime: timeFormat(dayjs(dateTime)),
    } as searchParams,
    allCount: 0 as number,
    seriousCount: 0 as number,
    warningCount: 0 as number,
    commonCount: 0 as number,
    remindCount: 0 as number,
    normalCount: 0 as number,
    auditResult: '',
    auditStatus: '',
    alarmLevel: '',
    field: '' as string,
    order: '' as string,
    selectedCard: [] as string[],
    selectedRows: [] as string[],
    curIndex: 0 as number,
    isShowModal: false,
    vertifyInfo: {},
    isCardLoading: false,
    isTableLoading: false,
    timer: '',
    hostUrl: window.location.host,
  });

  onMounted(async () => {
    await getAlarmInfoCard();
  });

  function getAlarmInfoList() {
    getAlarmCount({
      field: mainState.field ? mainState.field : '',
      order: mainState.order ? mainState.order : '',
      cameraNames: mainState.searchContion.cameraNames ? mainState.searchContion.cameraNames : '',
      categorys: mainState.searchContion.categorys ? mainState.searchContion.categorys : '',
      endTime: mainState.searchContion.endTime ? mainState.searchContion.endTime : '',
      startTime: mainState.searchContion.startTime ? mainState.searchContion.startTime : '',
      auditResult: mainState.auditResult || '',
      auditStatus: mainState.auditStatus || '',
      alarmLevel: mainState.alarmLevel || '',
      pageNo: mainState.currentPage,
      pageSize: mainState.pageSize,
      isRotate: mainState.searchContion.isRotate,
    }).then((res) => {
      mainState.allCount = res.allCount ? res.allCount : 0;
      mainState.remindCount = res.remindCount ? res.remindCount : 0;
      mainState.commonCount = res.commonCount ? res.commonCount : 0;
      mainState.warningCount = res.warningCount ? res.warningCount : 0;
      mainState.seriousCount = res.seriousCount ? res.seriousCount : 0;
      mainState.normalCount = res.normalCount ? res.normalCount : 0;
    });
    queryList();
  }
  function getAlarmInfoCard() {
    getAlarmCount({
      field: mainState.field ? mainState.field : '',
      order: mainState.order ? mainState.order : '',
      cameraNames: mainState.searchContion.cameraNames ? mainState.searchContion.cameraNames : '',
      categorys: mainState.searchContion.categorys ? mainState.searchContion.categorys : '',
      endTime: mainState.searchContion.endTime ? mainState.searchContion.endTime : '',
      startTime: mainState.searchContion.startTime ? mainState.searchContion.startTime : '',
      auditResult: mainState.auditResult || '',
      auditStatus: mainState.auditStatus || '',
      alarmLevel: mainState.alarmLevel || '',
      pageNo: mainState.currentCardPage,
      pageSize: mainState.pageCardSize,
      isRotate: mainState.searchContion.isRotate,
    }).then((res) => {
      mainState.allCount = res.allCount ? res.allCount : 0;
      mainState.remindCount = res.remindCount ? res.remindCount : 0;
      mainState.commonCount = res.commonCount ? res.commonCount : 0;
      mainState.warningCount = res.warningCount ? res.warningCount : 0;
      mainState.seriousCount = res.seriousCount ? res.seriousCount : 0;
      mainState.normalCount = res.normalCount ? res.normalCount : 0;
    });
    queryCard();
  }

  //表格数据
  const queryList = async () => {
    mainState.isTableLoading = true;
    const tableData = await list({
      field: mainState.field ? mainState.field : '',
      order: mainState.order ? mainState.order : '',
      cameraNames: mainState.searchContion.cameraNames ? mainState.searchContion.cameraNames : '',
      categorys: mainState.searchContion.categorys ? mainState.searchContion.categorys : '',
      endTime: mainState.searchContion.endTime ? mainState.searchContion.endTime : '',
      startTime: mainState.searchContion.startTime ? mainState.searchContion.startTime : '',
      auditResult: mainState.auditResult || '',
      auditStatus: mainState.auditStatus || '',
      alarmLevel: mainState.alarmLevel || '',
      pageNo: mainState.currentPage,
      pageSize: mainState.pageSize,
      isRotate: mainState.searchContion.isRotate,
    });
    if (tableData) {
      mainState.isTableLoading = false;
    }
    /**
     * 临时修改 一台服务器对应一台设备的情况
     * 直接取地址栏ip为图片路径
     */

    for (const data in tableData.alarmDetailList.records) {
      const targetData = tableData.alarmDetailList.records[data];
      const picPath = targetData.picPath;
      const newStr = '/kyscene' + picPath + '?' + targetData.alarmId;
      targetData.thumbnailPath = newStr;
      targetData.picPath = newStr;
      // 人脸库照片
      const faceDetailDo = targetData.faceDetailDo;
      const faceGroupDo = targetData.faceGroupDo;
      if (faceDetailDo && faceGroupDo) {
        targetData.faceUrl = 'http://' + window.location.hostname + ':9980/face/' + faceGroupDo.groupPath + '/' + faceDetailDo.imageNewName;
      }
      // console.log(newStr, 'newStr');
    }

    mainState.tableData = tableData.alarmDetailList.records;
    mainState.total = tableData.alarmDetailList.total; //修改单页总数
    mainState.detailList = tableData.alarmDetailList.records;
  };

  //卡片数据
  const queryCard = async () => {
    mainState.isCardLoading = true;
    const cardData = await list({
      field: mainState.field ? mainState.field : '',
      order: mainState.order ? mainState.order : '',
      cameraNames: mainState.searchContion.cameraNames ? mainState.searchContion.cameraNames : '',
      categorys: mainState.searchContion.categorys ? mainState.searchContion.categorys : '',
      endTime: mainState.searchContion.endTime ? mainState.searchContion.endTime : '',
      startTime: mainState.searchContion.startTime ? mainState.searchContion.startTime : '',
      auditResult: mainState.auditResult || '',
      auditStatus: mainState.auditStatus || '',
      alarmLevel: mainState.alarmLevel || '',
      pageNo: mainState.currentCardPage,
      pageSize: mainState.pageCardSize,
      isRotate: mainState.searchContion.isRotate,
    });
    if (cardData) {
      mainState.isCardLoading = false;
      console.log('@cardData', cardData)
    }

    mainState.cardData = cardData.alarmDetailList.records;

    for (const data in cardData.alarmDetailList.records) {
      const targetData = cardData.alarmDetailList.records[data];
      const picPath = targetData.picPath;
      // const newStr = 'http://' + mainState.hostUrl + '/kyscene' + picPath + '?' + targetData.alarmId;
      const newStr = 'http://' + mainState.hostUrl + picPath;
      targetData.thumbnailPath = newStr;
      targetData.picPath = newStr;
      console.log(targetData, 'targetData')
      // 人脸库照片
      const faceDetailDo = targetData.faceDetailDo;
      const faceGroupDo = targetData.faceGroupDo;
      if (faceDetailDo && faceGroupDo) {
        targetData.faceUrl = 'http://' + window.location.hostname + ':9980/face/' + faceGroupDo.groupPath + '/' + faceDetailDo.imageNewName;
      }
      // console.log(newStr, 'newStr');
    }

    mainState.cardTotal = cardData.alarmDetailList.total;

    //存放总数,用于搜索分页

    mainState.detailList = cardData.alarmDetailList.records;
  };
  //切换每页展示条数
  const handleShowSizeChange = async (sizeObj) => {
    mainState.currentPage = sizeObj.current;
    mainState.pageSize = sizeObj.size;
    queryList();
  };

  //切换页码
  const handleChange = (changeObj) => {
    mainState.currentPage = changeObj.page;
    mainState.pageSize = changeObj.pageSize;
    queryList();
  };

  //切换卡片每页展示条数
  const handleCardShowSizeChange = async (sizeObj) => {
    mainState.currentCardPage = sizeObj.current;
    mainState.pageCardSize = sizeObj.size;
    queryCard();
  };

  //切换卡片页码
  const handleCardChange = (changeObj) => {
    mainState.currentCardPage = changeObj.page;
    mainState.pageCardSize = changeObj.pageSize;
    queryCard();
  };

  const handleShowTable = async () => {
    mainState.showTable = !mainState.showTable;
    if (mainState.showTable) {
      mainState.currentPage = 1;
      queryList();
    } else {
      mainState.currentCardPage = 1;
      queryCard();
    }
  };

  //检索
  const handleAlarmSearch = (searchParams) => {
    mainState.currentPage = 1; //每次查询都将页码置为1
    mainState.currentCardPage = 1;
    mainState.searchContion = searchParams;
    getAlarmInfoList();
    getAlarmInfoCard();
  };
  //重置
  const handleAlarmReset = (data) => {
    const curTime = new Date();
    data.camera.length = 0;
    data.alarm.length = 0;
    mainState.searchContion.categorys = ''; //重置按钮检索后，未清空相机名和事件类型
    mainState.searchContion.isRotate = null;
    mainState.searchContion.cameraNames = '';
    mainState.searchContion.endTime = timeFormat(dayjs(curTime));
    mainState.searchContion.startTime = timeFormat(dayjs(curTime).subtract(7, 'day'));
    getAlarmInfoList();
    getAlarmInfoCard();
  };

  //点结果
  const handleClickAudit = ({ result, status }: { result: AuditResult; status: AuditStatus }) => {
    mainState.auditResult = result;
    mainState.auditStatus = status;
    if (mainState.showTable) {
      mainState.currentPage = 1;
      getAlarmInfoList();
    } else {
      mainState.currentCardPage = 1;
      getAlarmInfoCard();
    }
  };

  //点等级
  const handleClickLevel = (level: string) => {
    mainState.alarmLevel = level;
    if (mainState.showTable) {
      mainState.currentPage = 1;
      queryList();
    } else {
      mainState.currentCardPage = 1;
      queryCard();
    }
  };

  //点排序
  const handleSortChange = (field, order) => {
    mainState.field = field;
    mainState.order = order;
    queryList();
  };

  //导出
  const handleExport = async () => {
    if (mainState.showTable) {
      await handleExportXls('事件检索表格页', '/alarmDetail/alarmDetail/exportXls', { selections: mainState.selectedRows.join(',') });
    } else {
      await handleExportXls('事件检索卡片页', '/alarmDetail/alarmDetail/exportXls', { selections: mainState.selectedCard.join(',') });
    }
  };

  //卡片页项选中触发
  const handleSelected = (selectedCard) => {
    mainState.selectedCard = selectedCard;
  };

  //表格页行选中触发
  const handleSelectedRows = (selectedRows) => {
    mainState.selectedRows = selectedRows;
  };

  //上一个
  const handleGoPrev = () => {
    //获取到当前的是第几张,才可以对index进行++ --
    mainState.curIndex = mainState.detailList.findIndex((item) => item.id === mainState.vertifyInfo.id);
    if (mainState.curIndex > 0) {
      mainState.curIndex--;
      mainState.vertifyInfo = mainState.detailList[mainState.curIndex];
    }
  };
  //下一个
  const handleGoNext = () => {
    mainState.curIndex = mainState.detailList.findIndex((item) => item.id === mainState.vertifyInfo.id);
    if (mainState.curIndex < mainState.detailList.length - 1) {
      mainState.curIndex++;
      mainState.vertifyInfo = mainState.detailList[mainState.curIndex];
    }
  };
  const goImg = (el) => {
    mainState.detailList.forEach((item) => {
      if (item.id === el.id) {
        mainState.vertifyInfo = item;
      }
    });
  };

  //严重筛选
  const handleFilterSerious = () => {
    mainState.tableData = mainState.tableData.filter((item) => {
      return item.alarmLevel === '4';
    });
  };

  const showModal = () => {
    mainState.isShowModal = false;
  };

  const handleOk = () => {
    mainState.isShowModal = false;
  };
  const handleCancel = () => {
    mainState.isShowModal = false;
    queryList();
    queryCard();
  };

  const handleBack = () => {
    mainState.isShowModal = false;
    queryList();
    queryCard();
  };
  //卡片审核
  const handleCardAudit = (item) => {
    mainState.vertifyInfo = item;
    mainState.isShowModal = true;
    console.log('@', item)
  };
  //表格审核
  const handleTableAudit = (item) => {
    mainState.vertifyInfo = item;
    mainState.isShowModal = true;
  };
  /**
   * 成功回调
   */
  const handleSuccess = () => {
    if (mainState.showTable) {
      // mainState.currentPage = 1; //审核后页码不跳回到第一页
      mainState.selectedRows = [];
      // queryList(); //审核后不加载列表数据以免审核过慢
    } else {
      mainState.selectedCard = [];
      // mainState.currentCardPage = 1; //审核后页码不跳回到第一页
      // queryCard(); //审核后不加载列表数据以免审核过慢
    }
  };
  //是否删除完了最后一页
  const deleteAll = () => {
    if (mainState.showTable) {
      let newPage = Math.ceil((mainState.total - mainState.selectedRows.length) / mainState.pageSize);
      let currentPage = mainState.currentPage > newPage ? newPage : mainState.currentPage;
      mainState.currentPage = currentPage < 1 ? 1 : currentPage;
    } else {
      let newPage = Math.ceil((mainState.cardTotal - mainState.selectedCard.length) / mainState.pageCardSize);
      let currentCardPage = mainState.currentCardPage > newPage ? newPage : mainState.currentCardPage;
      mainState.currentCardPage = currentCardPage < 1 ? 1 : currentCardPage;
    }
  };
  const deleteSuccess = () => {
    if (mainState.showTable) {
      deleteAll();
      mainState.selectedRows = [];
      getAlarmInfoList();
    } else {
      deleteAll();
      mainState.selectedCard = [];
      getAlarmInfoCard();
    }
  };

  // 一键删除
  const deleteAllAlarm = async () => {
     createConfirm({
      iconType: 'warning',
      content: '确定删除所有告警信息？',
      onOk: async () => {
        await clearAlarmByManual()
        deleteSuccess()
      },
    });
  }

  // 批量删除
  const handleBatchDelete = () => {
    if (mainState.selectedRows.length === 0 && mainState.selectedCard.length === 0) {
      createMessage.info('未选择任何数据。。。');
      return;
    } else {
      if (mainState.showTable) {
         batchDelete({ ids: mainState.selectedRows }, deleteSuccess);
      } else {
         batchDelete({ ids: mainState.selectedCard }, deleteSuccess);
      }
    }
  };
  //卡片删除
  const handleCardDelete = async (item) => {
    deleteAll();
    await deleteOne({ id: item.id }, deleteSuccess);
  };
  // 表格删除
  const handleTableDelete = async (item) => {
    deleteAll();
    await deleteOne({ id: item.id }, deleteSuccess);
  };

  const handleCommit = async (auditParams) => {
    //将结果给后端
    await auditsResult({ auditReason: auditParams.auditReason, auditResult: auditParams.auditResult, alarmId: mainState.vertifyInfo.id }, handleSuccess);
    // console.log(auditRes, 'auditRes');
    handleGoNext();
    mainState.isShowModal = true;
    // await queryList();
    if (mainState.showTable) {
      queryList();
    } else {
      queryCard();
    }
  };
</script>
<style lang="less" scoped>
  .alarm-detail {
    height: calc(100vh - 110px);
    overflow-x: auto;

    .bottom {
      background-color: var(--background-Color);
      width: auto;
      min-width: 1200px;
    }
    .ant-empty {
      margin: 0 8px;
      font-size: 14px;
      line-height: 1.5715;
      text-align: center;
      position: absolute;
      top: 50%;
      left: 50%;
    }
  }

  .loading {
    position: absolute;
    top: 50%;
    left: 50%;
    &-image {
      width: 32px;
      height: 32px;
    }
  }
  .pagination {
    display: flex;
    justify-content: flex-end;
    margin: 12px 24px 0 12px;
  }
</style>
