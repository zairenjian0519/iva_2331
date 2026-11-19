<template>
  <div class="statistic-container">
    <div style="margin: 12px; padding-top: 24px; padding-left: 24px; padding-bottom: 16px; background-color: #ffffff">
      <alarm-header @handle-alarm-search="handleAlarmSearch" @handle-alarm-reset="handleStatisticReset" :is-statistic="(mainState.isSearchStatis = true)" class="header" />
    </div>
    <div class="static-main">
      <a-row :gutter="24" justify="space-around">
        <a-col :span="6">
          <div class="alarm-num">
            <template v-if="mainState.isLoading">
              <div class="loading"> <img src="../../../assets/svg/icon/loading_onwhite.gif" class="loading-image" alt="" /></div
            ></template>
            <template v-else>
              <div class="sum">
                <p class="title">事件数量</p>
                <div class="alarm-sum">{{ mainState.allCount }}</div>
                <div class="rate-box">
                  <template v-if="parseInt(mainState.rate) > 0">
                    <i class="increase-icon"></i>
                    <base-tooltip tooltip-title="环比增长率">
                      <span class="rate">{{ mainState.rate }}</span>
                    </base-tooltip>
                    <i class="icon-information"></i>
                  </template>
                  <template v-else>
                    <i class="decrease-icon"></i>
                    <base-tooltip tooltip-title="环比增长率">
                      <span class="rate">{{ mainState.rate }}</span>
                    </base-tooltip>
                    <i class="icon-information"></i>
                  </template>
                </div>
              </div>

              <div class="alarm-level">
                <div class="alarm-level-info">
                  <div class="label">严重</div>
                  <div class="serious-num">{{ mainState.seriousCount > 0 ? mainState.seriousCount : '-' }}</div>
                </div>
                <div class="alarm-level-info">
                  <div class="label"> 警告</div>
                  <div class="warning-num">{{ mainState.warningCount > 0 ? mainState.warningCount : '-' }}</div>
                </div>
                <div class="alarm-level-info">
                  <div class="label">一般</div>
                  <div class="common-num">{{ mainState.commonCount > 0 ? mainState.commonCount : '-' }}</div>
                </div>
                <div class="alarm-level-info">
                  <div class="label">提醒</div>
                  <div class="remind-num">{{ mainState.remindCount > 0 ? mainState.remindCount : '-' }}</div>
                </div>
              </div>
              <Divider type="vertical" class="divider" />
              <div class="selected">
                <div class="camera-selected">
                  <i class="camera-icon"></i>
                  <base-tooltip tooltip-title="相机数量">
                    <div class="camera-num" v-if="!mainState.isSelectedCamera">{{ mainState.cameraAmount }}</div>
                    <div class="camera-num" v-else>{{ mainState.searchParams.cameraNames.split(',').length }}</div>
                    <!--                <span class="camera-num" v-else>-</span>-->
                  </base-tooltip>
                </div>
                <div class="algorithm-selected">
                  <i class="algorithm-icon"></i>
                  <base-tooltip tooltip-title="算法数量">
                    <!-- <template #title>算法数量</template> -->
                    <div class="algorithm-num">{{ mainState.categoryAmount }}</div>
                    <!--                <span class="algorithm-num" v-else>-</span>-->
                  </base-tooltip>
                </div>
              </div></template
            >
          </div>
          <a-space direction="vertical" size="middle" />

          <div id="typeMain" class="alarm-type">
            <template v-if="mainState.isLoading">
              <div class="loading"> <img src="../../../assets/svg/icon/loading_onwhite.gif" class="loading-image" alt="" /></div
            ></template>
          </div>
        </a-col>
        <a-col :span="12">
          <div class="alarm-date">
            <div class="alarm-date-check">
              <a-checkbox v-model:checked="mainState.isDateShow" @change.stop="handleDateChange"><span class="text">上一周期</span></a-checkbox>
            </div>
            <div id="dateMain" class="date">
              <template v-if="mainState.isLoading">
                <div class="loading"> <img src="../../../assets/svg/icon/loading_onwhite.gif" class="loading-image" alt="" /></div></template
            ></div>
          </div>
          <a-space direction="vertical" size="middle" />
          <div class="alarm-hour">
            <div class="alarm-hour-check">
              <a-checkbox v-model:checked="mainState.isHourShow" @change="handleHourChange"> <span class="text">上一周期</span></a-checkbox>
            </div>
            <div id="hourMain" class="hour"
              ><template v-if="mainState.isLoading">
                <div class="loading"> <img src="../../../assets/svg/icon/loading_onwhite.gif" class="loading-image" alt="" /></div></template
            ></div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="statistic-camera">
            <div class="dropdown">
              <a-dropdown>
                <div class="dropdown-box" @click.prevent>
                  {{ mainState.buttonName }}
                  <i class="dropdown-icon"></i>
                </div>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="1" @click="handleChangeCamera">相机 </a-menu-item>
                    <a-menu-item key="2" @click="handleChangeLocation"> 分组 </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
            <div id="cameraMain" class="camera"
              ><template v-if="mainState.isLoading">
                <div class="loading"> <img src="../../../assets/svg/icon/loading_onwhite.gif" class="loading-image" alt="" /></div></template
            ></div>
          </div>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { onBeforeUnmount, onMounted, reactive, watch } from 'vue';
  import echarts from '/@/utils/lib/echarts';
  // import cameraIcon from '/@/assets/svg/icon/filter_icon_camera.svg';
  // import dropDownIcon from '/@/assets/svg/icon/icon_arrowdown_default.svg';
  import { getStatistics, statisticsSearch, getStatisticsMainCount, getStatisticsByCamera, getStatisticsByType, getStatisticsByDate, getStatisticsByHour } from './AlarmStatistics.api';
  import AlarmHeader from '/src/views/aiCheck/alarmDetails/behavioralData/components/AlarmHeader.vue';
  import BaseTooltip from '/@/components/BaseTooltip/BaseTooltip.vue';
  import mitt from '/@/utils/mitt/index';
  import { dateCount, lastDateCount, cameraCount, typeCountList, hourCount, lastHourCount, searchConditionParams, locationCount } from './AlarmStatistics.data';
  import dayjs from 'dayjs';
  import { Divider, Dropdown } from 'ant-design-vue';
  import { timeFormat } from '/@/utils/date/dateFormat';
  import { Checkbox } from 'ant-design-vue';
  const ACheckbox = Checkbox;
  const ADropdown = Dropdown;

  const mainState = reactive({
    rate: '' as string, //增长率
    allCount: 0 as number, //事件总数
    remindCount: 0 as number, //提醒事件数
    commonCount: 0 as number, //一般事件数
    warningCount: 0 as number, //警告事件数
    seriousCount: 0 as number, //严重事件数
    cameraCountList: [] as cameraCount[], //事件统计图（相机）
    typeCountList: [] as typeCountList[], //事件统计图（类型）
    hourCountList: [] as hourCount[], //24小时事件态势图
    lastHourCountList: [] as lastHourCount[], //24小时事件态势图（上一周期）
    dateCountList: [] as dateCount[], //事件统计（日期）
    tempDateCountList: [] as dateCount[], //事件统计（日期）
    lastDateCountList: [] as lastDateCount[], //事件统计（日期）-（上一周期）
    locationCountList: [] as locationCount[],
    isSelectedCamera: false,
    isSelectedCategory: false,
    searchParams: {
      cameraNames: '',
      categorys: '',
    } as searchConditionParams,
    isDateShow: false,
    isHourShow: false,
    isSearchStatis: false,
    //临时保存
    tempCameraList: [] as cameraCount[],
    cameraCount: 0 as number,
    cameraTitle: '相机总数',
    buttonName: '相机',
    cameraAmount: 0 as number,
    categoryAmount: 0 as number,
    isLoading: true as Boolean,
    titleColor: '',
  });

  const dateTime = new Date();

  const params = {
    startTime: timeFormat(dayjs(dateTime).subtract(7, 'day')),
    endTime: timeFormat(dayjs(dateTime)),
    // startTime: '2022-09-01 01:10:50',
    // endTime: '2022-09-10 01:10:50',
    categorys: '',
    cameraNames: '',
  };

  //上一周期日期
  const handleDateChange = async (e) => {
    mainState.isDateShow = e.target.checked;
    // console.log(mainState.isDateShow, '1111');
    if (mainState.isDateShow) {
      mainState.dateCountList = mainState.lastDateCountList;
      echartsInitType();
      echartsInitDate();
      echartsInitHour();
      echartsInitCamera();
    } else {
      // const res = await getStatistics(params);
      getCountInfo();
    }
  };
  //上一周期小时
  const handleHourChange = async (e) => {
    mainState.isHourShow = e.target.checked;
    if (mainState.isHourShow) {
      mainState.hourCountList.forEach((item) => {
        item.value = 0;
      });
      echartsInitType();
      echartsInitDate();
      echartsInitHour();
      echartsInitCamera();
    } else {
      getCountInfo();
    }
  };

  //相机和区域的切换
  const handleChangeLocation = () => {
    mainState.buttonName = '分组';
    mainState.cameraTitle = '分组数量';
    mainState.cameraCountList = mainState.locationCountList;
    mainState.cameraCount = mainState.cameraCountList.length;
    //之前统计的是事件数量
    // mainState.locationCountList.forEach((item) => {
    //   mainState.cameraCount += item.value;
    // });
    echartsInitCamera();
  };

  const handleChangeCamera = () => {
    mainState.cameraTitle = '相机数量';
    mainState.buttonName = '相机';
    mainState.cameraCountList = mainState.tempCameraList;
    mainState.cameraCount = mainState.tempCameraList.length;
    echartsInitCamera();
  };

  mitt.on('changeTheme', () => {
    echartsInitType();
    echartsInitDate();
    echartsInitHour();
    echartsInitCamera();
  });

  const echartsInitType = () => {
    const typeOption = {
      title: {
        text: '事件类型',
        subtext: '{algTitle|算法数量}  {algNum|' + mainState.typeCountList.length + '}',
        padding: [32, 32, 0, 32],
        fontSize: 14,
        textStyle: {
          fontSize: 14,
          fontWeight: 600,
          fontFamily: 'Source Han Sans CN',
          color: getComputedStyle(document.documentElement).getPropertyValue('--title-color'),
        },

        subtextStyle: {
          rich: {
            algTitle: {
              fontSize: 13,
              fontWeight: 500,
              fontFamily: 'Source Han Sans CN',
              color: '#adb4cc',
            },
            algNum: {
              fontSize: 13,
              fontWeight: 500,
              fontFamily: 'Source Han Sans CN',
              color: '#adb4cc',
            },
          },
          // },
        },
        itemGap: 310,
        textAlign: 'left',
      },
      graphic: {
        type: 'text',
        left: 'center',
        top: '38%',
        style: {
          // text: 'name + '\n\r' + '\n\r' + mainState.searchParams.categorys.split(',').length,
          textAlign: 'center',
          fill: '#575b66',
          fontWeight: 600,
          padding: [12, 12, 12, 12],
          fontFamily: 'Source Han Sans CN',
          fontSize: 14,
        },
      },

      tooltip: {
        trigger: 'item',
      },
      legend: [
        {
          x: 'left',
          orient: 'vertical',
          padding: [16, 32, 16, 8],
          bottom: '0',
          itemWidth: 12,
          itemHeight: 6,
          data: mainState.typeCountList.slice(0, mainState.typeCountList.length / 2),
          textStyle: {
            fontSize: 13,
            fontFamily: 'Source Han Sans CN',
            color: '#5c5e66',
            // innerWidth: 300,

            ellipsis: '...',
            rich: {
              title: {
                width: 70,
                height: 13,
                fontFamily: 'Source Han Sans CN',
                color: '#5c5e66',
                fontSize: 13,
                overflow: 'truncate',
                minWidth: 40,
              },
              num: {
                width: 44,
                height: 13,
                fontFamily: 'Source Han Sans CN',
                color: '#5c5e66',
                fontSize: 13,
                textAlign: 'right',
                minWidth: 40,
                padding: [16, 16, 16, 0],
              },
            },
          },
          itemStyle: {
            // borderWidth: 12,
          },

          formatter: function (name) {
            let data = typeOption.series[0].data.slice(0, typeOption.series[0].data.length / 2);
            // console.log(data, name, 'daatda');

            let targetValue;

            for (let index = 0; index < data.length; index++) {
              if (data[index].name === name) {
                targetValue = data[index].value;
                // console.log(targetValue, 'targetValue');
              }
            }
            name = name.length > 6 ? name.substr(0, 6) + '...' : name;
            // '{algTitle|算法数量}  {algNum|' + mainState.typeCountList.length + '}',
            // '{title|' + data.name + '}' + '\n\r' + '{active|' + data.value + '}' + '{only|' + '次}';
            let value = '{title|' + name + '}{num|' + targetValue + '}';
            if (targetValue === undefined) {
              value = '';
            }

            return value;
          },
        },
        {
          x: 'right',
          orient: 'vertical',
          align: 'left',
          padding: [16, 32, 16, 8],
          itemWidth: 12,
          itemHeight: 6,
          bottom: '0',
          data: mainState.typeCountList.slice(mainState.typeCountList.length / 2),
          textStyle: {
            fontSize: 13,
            fontFamily: 'Source Han Sans CN',
            color: '#5c5e66',

            // innerWidth: 300,
            rich: {
              title: {
                width: 80,
                height: 13,
                fontFamily: 'Source Han Sans CN',
                color: '#5c5e66',
                fontSize: 13,
                overflow: 'truncate',
                minWidth: 80,
              },
              num: {
                width: 44,
                height: 13,
                fontFamily: 'Source Han Sans CN',
                color: '#5c5e66',
                fontSize: 13,
                textAlign: 'right',
                minWidth: 44,
                padding: [16, 16, 16, 0],
              },
            },
          },
          itemStyle: {
            // borderWidth: 12,
          },

          formatter: function (name) {
            let data = typeOption.series[0].data.slice(typeOption.series[0].data.length / 2);
            // console.log(data, name, 'daatda');

            let target;

            for (let index = 0; index < data.length; index++) {
              if (data[index].name === name) {
                target = data[index].value;
                // console.log(targetValue, 'targetValue');
              }
            }
            name = name.length > 6 ? name.substr(0, 6) + '...' : name;
            // '{algTitle|算法数量}  {algNum|' + mainState.typeCountList.length + '}',
            // '{title|' + data.name + '}' + '\n\r' + '{active|' + data.value + '}' + '{only|' + '次}';
            let value = '{title|' + name + '}{num|' + target + '}';
            if (target === undefined) {
              value = '';
            }

            return value;
          },
        },
      ],

      //在这改颜色
      color: ['#eef0fb', '#dde0f7', '#ccd1f3', '#bcc2ef', '#abb2ea', '#9aa3e6', '#8993e2', '#7884de', '#6974c2', '#5a63a7', '#4b538b', '#3c426f', '#2d3253', '#1e2138', '#1e2138'],

      series: [
        {
          name: '事件类型',
          type: 'pie',
          radius: ['40%', '70%'],
          //控制移动
          center: ['50%', '40%'],
          top: -32,
          avoidLabelOverlap: false,
          itemStyle: {
            borderWidth: 2,
          },
          label: {
            show: false,
            position: 'center',
            // color: '#4c4a4a',
          },
          emphasis: {
            label: {
              show: true,
              formatter: function (data) {
                const str = '{title|' + data.name + '}' + '\n\r' + '{active|' + data.value + '}' + '{only|' + '次}';
                return str;
              },

              rich: {
                title: {
                  fontFamily: 'Source Han Sans CN',
                  fontWeight: 600,
                  color: '#575b66',
                  fontSize: 13,
                  // padding: [24, 12, 12, 12],
                },
                active: {
                  fontFamily: 'DIN Condensed',
                  fontWeight: 700,
                  color: '#575b66',
                  fontSize: 24,
                  width: 18,
                  height: 24,
                  padding: 32,
                },
                only: {
                  fontFamily: 'Source Han Sans CN',
                  fontWeight: 600,
                  color: '#575b66',
                  fontSize: 13,
                },
              },
            },
          },

          labelLine: {
            show: false,
          },
          data: mainState.typeCountList,
        },
      ],

      media: [
        {
          query: {
            maxWidth: 229,
          },
          option: {
            radius: ['20', '50%'],
            center: ['10%', '20%'],
            title: {
              text: '事件类型',
              subtext: '{algTitle|算法数量}  {algNum|' + mainState.typeCountList.length + '}',

              fontSize: 14,

              textStyle: {
                fontSize: 14,
                fontWeight: 600,
                fontFamily: 'Source Han Sans CN',
                color: '#adb4cc',
              },

              subtextStyle: {
                rich: {
                  algTitle: {
                    fontSize: 13,
                    fontWeight: 500,
                    fontFamily: 'Source Han Sans CN',
                    color: '#adb4cc',
                    padding: [-210, 0, 0, 0],
                  },
                  algNum: {
                    fontSize: 13,
                    fontWeight: 500,
                    fontFamily: 'Source Han Sans CN',
                    color: '#adb4cc',
                    padding: [-210, 0, 0, 0],
                  },
                },
              },
              itemGap: 330,
              textAlign: 'left',
            },
            emphasis: {
              label: {
                show: true,
                formatter: function (data) {
                  const str = '{title|' + data.name + '}' + '\n\r' + '{active|' + data.value + '}' + '{only|' + '次}';
                  return str;
                },

                rich: {
                  title: {
                    fontFamily: 'Source Han Sans CN',
                    fontWeight: 600,
                    color: '#575b66',
                    fontSize: 13,
                    padding: [24, 0, 0, 0],
                  },
                  active: {
                    fontFamily: 'DIN Condensed',
                    fontWeight: 700,
                    color: '#575b66',
                    fontSize: 24,
                    width: 16,
                    height: 24,
                  },
                  only: {
                    fontFamily: 'Source Han Sans CN',
                    fontWeight: 600,
                    color: '#575b66',
                    fontSize: 13,
                  },
                },
              },
            },
            padding: 0,
            fontSize: 14,

            series: [
              {
                top: -150,
              },
            ],
            legend: [
              {
                x: 'left',
                right: 24,
                orient: 'vertical',
                type: 'scroll',
                align: 'auto',
                top: '50%',
                bottom: 12,
                left: 24,

                itemWidth: 12,
                itemHeight: 6,
                itemGap: 12,
                data: mainState.typeCountList.slice(0),
                textStyle: {
                  fontSize: 13,
                  fontFamily: 'Source Han Sans CN',
                  color: '#5c5e66',

                  rich: {
                    title: {
                      height: 13,
                      fontFamily: 'Source Han Sans CN',
                      color: '#5c5e66',
                      // align: 'left',
                      fontSize: 13,
                      width: 'auto',
                    },
                    num: {
                      width: 44,
                      height: 13,
                      fontFamily: 'Source Han Sans CN',
                      color: '#5c5e66',
                      fontSize: 13,
                      align: 'right',
                    },
                  },
                },

                formatter: function (name) {
                  let data = mainState.typeCountList;
                  let targetValue;

                  for (let index = 0; index < data.length; index++) {
                    if (data[index].name === name) {
                      targetValue = data[index].value;
                      // console.log(targetValue, 'targetValue');
                    }
                  }
                  name = name.length > 6 ? name.substr(0, 6) + '...' : name;

                  // '{algTitle|算法数量}  {algNum|' + mainState.typeCountList.length + '}',
                  // '{title|' + data.name + '}' + '\n\r' + '{active|' + data.value + '}' + '{only|' + '次}';
                  let value = '{title|' + name + '}{num|' + targetValue + '}';
                  if (targetValue === undefined) {
                    value = '';
                  }

                  return value;
                },
              },
              {
                x: 'right',
                data: [],
              },
            ],
          },
        },
      ],
    };
    typeOption.legend[0].data = typeOption.series[0].data.slice(0, typeOption.series[0].data.length / 2);
    typeOption.legend[1].data = typeOption.series[0].data.slice(typeOption.series[0].data.length / 2);
    // typeOption.media[0].option.legend.data = typeOption.series[0].data;

    const typeChartDom = document.getElementById('typeMain')!;
    const typeChart = echarts.init(typeChartDom);
    typeChart.clear();

    typeOption && typeChart.setOption(typeOption, true);
    console.log(typeOption, 'typeOption');

    window.addEventListener(
      'resize',
      () => {
        typeChart.resize();
      },
      false
    );
  };
  const echartsInitDate = () => {
    //事件统计（日期）
    const dateOption = {
      title: {
        text: '事件统计（日期）',
        padding: [32, 32, 0, 32],
        fontSize: 14,
        textStyle: {
          fontSize: 14,
          fontWeight: 600,
          fontFamily: 'Source Han Sans CN',
          color: getComputedStyle(document.documentElement).getPropertyValue('--title-color'),
        },
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow',
        },
      },

      legend: {
        orient: 'horizontal',
        itemWidth: 12,
        itemHeight: 6,
        itemGap: 12,
        bottom: '10',

        left: 32,
        right: 32,
        padding: [12, 0, 0, 32],
        textStyle: {
          // fontSize: 13,
          // fontFamily: 'Source Han Sans CN',
          // color: '#5c5e66',
          padding: [12, 0, 12, 8],
          // innerWidth: 300,
          width: 22,
          height: 11,
          fontFamily: 'Source Han Sans CN',
          color: '#8a8d99',
          fontsize: 11,
          // padding: [],
        },
        itemStyle: {
          borderWidth: 12,
        },
        // textStyle: {},
        axisLabel: {
          inside: false,
          textStyle: {
            color: '#adb4cc',
            fontSize: 11,
            fontFamily: 'Source Han Sans CN',
          },
        },

        formatter: function (name) {
          return `${name} `;
        },
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '10%',
        containLabel: true,
        height: 240,
      },
      toolbox: {
        top: 28,
        right: 32,
        iconStyle: {
          color: '#ffffff',
        },
        emphasis: {
          iconStyle: {
            color: '#738ce6',
          },
        },
        feature: {
          dataView: { show: false, readOnly: false },
          magicType: {
            show: true,
            type: ['line', 'bar'],
          },
          restore: { show: false },
          saveAsImage: { show: false },
        },
      },

      xAxis: [
        {
          type: 'category',
          splitNumber: 1,
          min: 0,
          // max: 7,
          interval: 10,
          boundaryGap: [0.01, 0.01],

          scale: true,

          data: mainState.dateCountList.map((item) => {
            return item.date.replace(/\-/g, '/').substring(5);
          }),
          axisLine: {
            lineStyle: {
              color: 'rgba(211, 217, 232, 0.4)',
              borderType: 'solid',
            },
          },
          axisTick: {
            show: false,
          },
          axisLabel: {
            show: true,
            // color: 'rgba(211, 217, 232, 0.4)',

            textStyle: {
              width: 32,
              height: 11,
              fontFamily: 'Source Han Sans CN',
              color: '#adb4cc',
              fontSize: 11,
              textAlign: 'center',
              padding: [0, 48, 0, 48],
            },
          },
        },
      ],
      yAxis: [
        {
          type: 'value',
          axisLine: {
            show: false,
          },
          splitLine: {
            // show: true,
            lineStyle: {
              type: 'dashed',
            },
          },

          axisLabel: {
            //控制显示纵坐标
            show: false,
            valueAnimation: true,
            textStyle: {
              width: 32,
              height: 11,
              fontFamily: 'Source Han Sans CN',
              color: '#adb4cc',
              fontSize: 11,
              textAlign: 'center',
            },
          },
        },
      ],
      series: [
        {
          name: '严重',
          type: 'bar',
          stack: 'Ad',
          barWidth: 12,

          emphasis: {
            focus: 'series',
          },
          data: mainState.dateCountList.map((item) => {
            return item.seriousValue;
          }),

          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(214, 132, 132, 1)' },
              { offset: 1, color: 'rgba(204, 102, 102, 1)' },
            ]),
            barBorderRadius: [2, 2, 0, 0],
          },
          // label: {
          //   show: true,
          //   position: 'top',
          //   valueAnimation: true,
          // },
        },
        {
          name: '警告',
          type: 'bar',
          stack: 'Ad',
          barWidth: 12,
          emphasis: {
            focus: 'series',
          },
          data: mainState.dateCountList.map((item) => {
            return item.warningValue;
          }),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(235, 163, 124, 1)' },
              { offset: 1, color: 'rgba(230, 140, 92, 1)' },
            ]),
            barBorderRadius: [2, 2, 0, 0],
          },
        },
        {
          name: '一般',
          type: 'bar',
          stack: 'Ad',
          barWidth: 12,
          emphasis: {
            focus: 'series',
          },
          data: mainState.dateCountList.map((item) => {
            return item.commonValue;
          }),
          itemStyle: {
            normal: {
              //这里是颜色
              // backgroundImage: 'linear-gradient(180deg,#d68484 0%,#cc6666 100%)',
              color: '#f2c261',
            },
            barBorderRadius: [2, 2, 0, 0],
          },
        },

        {
          name: '提醒',
          type: 'bar',
          stack: 'Ad',
          barWidth: 12,
          emphasis: {
            focus: 'series',
          },
          data: mainState.dateCountList.map((item) => {
            return item.remindValue;
          }),
          itemStyle: {
            //这里是颜色
            // backgroundImage: 'linear-gradient(180deg,#d68484 0%,#cc6666 100%)',
            color: '#7884de',
            barBorderRadius: [2, 2, 0, 0],
          },
          // label: {
          //   show: true,
          //   position: 'top',

          //   valueAnimation: true,
          // },
        },
        {
          name: '',
          type: 'bar',
          barWidth: 12,
          stack: '',
          label: {
            normal: {
              show: true,
              formatter: function (params) {
                if (params.value > 0) {
                  return params.value;
                } else {
                  return '';
                }
              },
              position: 'top',
              fontFamily: 'DIN Condensed',
              fontWeight: 700,
              color: '#575b66',
              fontSize: 14,
            },
          },
          itemStyle: {
            normal: {
              color: 'rgba(128,128,128,0)',
            },
          },
          z: -1,
          //不同系列的柱间距离，为百分比,如果想要两个系列的柱子重叠，可以设置 barGap 为 '-100%'。
          barGap: '-100%',
          data: mainState.dateCountList.map((item) => {
            return item.value;
          }),
        },
      ],
    };
    const dateChartDom = document.getElementById('dateMain')!;
    const dateChart = echarts.init(dateChartDom);
    window.addEventListener(
      'resize',
      () => {
        dateChart.resize();
      },
      false
    );
    dateOption && dateChart.setOption(dateOption, true);
  };
  const echartsInitHour = () => {
    const hourOption = {
      title: {
        text: '24h事件态势',
        padding: [32, 32, 0, 32],
        fontSize: 14,
        textStyle: {
          fontSize: 14,
          fontWeight: 600,
          fontFamily: 'Source Han Sans CN',
          color: getComputedStyle(document.documentElement).getPropertyValue('--title-color'),
        },
      },
      color: ['#7983de', '#7986cb'],
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow',
        },
      },
      xAxis: {
        type: 'category',
        data: mainState.hourCountList.map((item) => {
          return item.hour;
        }),
        axisTick: {
          show: false,
        },
        splitLine: {
          // show: true,
          lineStyle: {
            type: 'dashed',
          },
        },
        axisLine: {
          lineStyle: {
            color: 'rgba(211, 217, 232, 0.4)',
            borderType: 'solid',
          },
        },
        axisLabel: {
          show: true,
          textStyle: {
            fontFamily: 'Source Han Sans CN',
            color: '#adb4cc',
            fontSize: 11,
            textAlign: 'center',
          },
        },
      },
      yAxis: {
        type: 'value',
        splitLine: {
          // show: true,
          lineStyle: {
            type: 'dashed',
          },
        },
        axisLine: {
          lineStyle: {
            color: 'rgba(211, 217, 232, 0.4)',
            borderType: 'solid',
          },
        },
        axisLabel: {
          show: true,
          textStyle: {
            fontFamily: 'Source Han Sans CN',
            color: '#adb4cc',
            fontSize: 11,
            textAlign: 'center',
          },
        },

        data: ['10', '20', '30', '40', '50'],
      },
      legend: {
        orient: 'horizontal',
        itemWidth: 32,
        itemHeight: 2,
        itemGap: 24,
        bottom: '20',
        // left: 32,
        // right: 32,
        padding: [12, 0, 0, 32],
        data: [
          {
            name: '近一周',
            type: 'line',

            textStyle: {
              fontFamily: 'Source Han Sans CN',
              color: '#8a8d99',
              fontSize: 11,
            },
            icon: 'rectangle',
          },
          {
            name: '上一周期',
            textStyle: {
              fontFamily: 'Source Han Sans CN',
              color: '#8a8d99',
              fontSize: 11,
            },
            // icon: '',
          },
        ],
      },
      grid: {
        left: '32',
        right: '32',
        bottom: '10%',
        containLabel: true,
        height: 251,
      },
      series: [
        {
          name: '近一周',
          data: mainState.hourCountList.map((item) => {
            return item.value;
          }),
          type: 'line',
          smooth: true,
          symbol: 'circle',
          // symbolSize: 8,
          areaStyle: {},
        },
        {
          name: '上一周期',
          data: mainState.lastHourCountList.map((item) => {
            return item.value;
          }),
          type: 'line',
          smooth: true,
          // symbol: 'circle',
          // symbolSize: 8,
          itemStyle: {
            width: 8,
            height: 8,
            backgroundColor: '#7986cb',
          },
          lineStyle: {
            color: '#5470C6',
            width: 2,
            type: 'dotted',
          },
        },
      ],
    };
    const hourChartDom = document.getElementById('hourMain')!;
    const hourChart = echarts.init(hourChartDom);
    window.addEventListener(
      'resize',
      () => {
        hourChart.resize();
      },
      false
    );
    hourOption && hourChart.setOption(hourOption, true);
  };

  const echartsInitCamera = () => {
    const cameraOption = {
      title: {
        text: '事件统计（相机）',
        subtext: '{cameraTitle|' + mainState.cameraTitle + '}  {cameraNum|' + mainState.cameraCount + '}', //修改根据选择相机和区域变副标题名称
        padding: [24, 32, 0, 32],
        fontSize: 14,
        textStyle: {
          fontSize: 14,
          fontWeight: 600,
          fontFamily: 'Source Han Sans CN',
          color: getComputedStyle(document.documentElement).getPropertyValue('--title-color'),
        },
        subtextStyle: {
          rich: {
            cameraTitle: {
              fontSize: 13,
              fontWeight: 500,
              fontFamily: 'Source Han Sans CN',
              color: '#adb4cc',
              padding: [24, 0, 18, 0],
            },
            cameraNum: {
              fontSize: 13,
              fontWeight: 500,
              fontFamily: 'Source Han Sans CN',
              color: '#adb4cc',
              padding: [24, 0, 18, 0],
            },
          },
        },
      },
      grid: {
        show: false,
        left: '32',
        right: '48',
        // bottom: '200',
        top: '100',
        containLabel: true,
        // height: 400,
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow',
        },
      },
      legend: {},

      xAxis: {
        type: 'value',
        show: false,
        axisLabel: {
          show: true,
          interval: 0,
        },
        splitLine: {
          show: false,
        },
        axisTick: {
          show: false,
        },
        axisLine: {
          show: false,
        },
      },
      yAxis: {
        type: 'category',
        // offset: 0,
        position: 'left',
        data: mainState.cameraCountList.map((item) => {
          return item.name;
        }),
        axisLine: {
          show: false,
        },
        axisTick: {
          show: false,
        },
        splitLine: {
          show: false,
        },

        min: 0,
        // max: 18,
        boundaryGap: [0.01, 0.01],

        inverse: true,
        axisLabel: {
          show: true,
          interval: 0,
          overflow: 'break',
          width: '132',
          inside: true,
          verticalAlign: 'bottom',
          lineHeight: 32,
        },
      },

      series: [
        {
          type: 'bar',
          barWidth: 8,
          barGap: '-100%',
          margin: 20,
          stack: '',
          data: mainState.cameraCountList.map((item) => {
            return item.value;
          }),
          showBackground: true,
          backgroundStyle: {
            color: '#e9edf2',
            borderRadius: 15,
          },

          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(152, 162, 229, 1)' },
              { offset: 1, color: 'rgba(120, 132, 222, 1)' },
            ]),
            barBorderRadius: [2, 2, 2, 2],
            padding: [2, 2, 2, 2],
          },
          label: {
            show: true,
            position: 'right',
            valueAnimation: true,
            fontFamily: 'BebasNeue-Regular',
            color: '#8a8d99',
            fontSize: 14,
          },
        },
      ],
      dataZoom: [
        {
          id: 'dataZoomY',
          yAxisIndex: [0, 1],
          show: true, // 是否显示滑动条，不影响使用
          type: 'slider', // 这个 dataZoom 组件是 slider 型 dataZoom 组件
          // start: 100, // 默认显示0%
          // startValue: 2000000, // 从末尾开始--进行倒序
          endValue: 4, // 一次性展示5个
          width: 5,
          borderColor: 'transparent',
          fillerColor: 'rgba(205,205,205,1)',
          zoomLock: true,
          showDataShadow: false, // 是否显示数据阴影 默认auto
          backgroundColor: '#fff',
          showDetail: false, // 即拖拽时候是否显示详细数值信息 默认true
          realtime: true, // 是否实时更新
          filterMode: 'filter',
          handleIcon: 'circle',
          handleStyle: {
            color: 'rgba(205,205,205,1)',
            borderColor: 'rgba(205,205,205,1)',
          },
          handleSize: '80%',
          moveHandleSize: 0,
          maxValueSpan: 15,
          minValueSpan: 15,
          brushSelect: false, // 刷选功能，设为false可以防止拖动条长度改变
        },
        {
          type: 'inside',
          yAxisIndex: [0, 1],
          start: 0, // 默认显示0%
          zoomOnMouseWheel: false, // 滚轮是否触发缩放
          moveOnMouseMove: true, // 鼠标滚轮触发滚动
          moveOnMouseWheel: true,
        },
      ],
    };
    const cameraChartDom = document.getElementById('cameraMain')!;
    const cameraChart = echarts.init(cameraChartDom);
    setTimeout(() => {
      let width = cameraChart.getWidth();
      let opt = cameraChart.getOption();
      let grid = opt.grid[0];
      let right = grid.right;
      let left = grid.left;
      var x = width - left - right;
      cameraChart.setOption({
        series: [
          {
            label: {
              show: true,
              position: 'insideLeft',
              offset: [x, 0],
            },
          },
          2,
        ],
      });
    });
    window.addEventListener(
      'resize',
      () => {
        cameraChart.resize();
      },
      false
    );

    cameraOption && cameraChart.setOption(cameraOption, true);
  };
  //初始化

  onBeforeUnmount(() => {
    const typeChartDom = document.getElementById('typeMain')!;
    typeChartDom.removeAttribute('_echarts_instance_');
    const dateChartDom = document.getElementById('dateMain')!;
    dateChartDom.removeAttribute('_echarts_instance_');
    const hourChartDom = document.getElementById('hourMain')!;
    hourChartDom.removeAttribute('_echarts_instance_');
    const cameraChartDom = document.getElementById('cameraMain')!;
    cameraChartDom.removeAttribute('_echarts_instance_');
  });

  onMounted(async () => {
    await getCountInfo();
  });

  function getCountInfo() {
    getStatisticsMainCount(params).then((res) => {
      mainState.rate = res.rate;
      mainState.allCount = res.allCount;
      mainState.remindCount = res.remindCount;
      mainState.commonCount = res.commonCount;
      mainState.warningCount = res.warningCount;
      mainState.seriousCount = res.seriousCount;
      mainState.cameraAmount = res.cameraAmount;
      mainState.categoryAmount = res.categoryAmount;
      if (res) {
        mainState.isLoading = false;
      }
    });
    getStatisticsByCamera(params).then((res) => {
      mainState.cameraCount = res.cameraCountList.length;
      mainState.cameraCountList = res.cameraCountList;
      mainState.locationCountList = res.locationCountList;
      mainState.tempCameraList = res.cameraCountList;
      if (res) {
        mainState.isLoading = false;
      }
      echartsInitCamera();
    });
    getStatisticsByType(params).then((res) => {
      mainState.typeCountList = res.typeCountList;
      if (res) {
        mainState.isLoading = false;
      }
      echartsInitType();
    });
    getStatisticsByDate(params).then((res) => {
      mainState.dateCountList = res.dateCountList;
      mainState.lastDateCountList = res.lastDateCountList;
      mainState.tempDateCountList = res.dateCountList;
      if (res) {
        mainState.isLoading = false;
      }
      echartsInitDate();
    });
    getStatisticsByHour(params).then((res) => {
      mainState.hourCountList = res.hourCountList;
      mainState.lastHourCountList = res.lastHourCountList;
      if (res) {
        mainState.isLoading = false;
      }
      echartsInitHour();
    });
  }
  //搜索
  const handleAlarmSearch = async (params) => {
    mainState.searchParams = params;
    if (mainState.searchParams.cameraNames != '') {
      mainState.isSelectedCamera = true;
    }
    const paramsObj = { cameraNames: params.cameraNames, categorys: params.categorys, startTime: params.startTime, endTime: params.endTime };
    getStatisticsMainCount(paramsObj).then((res) => {
      mainState.rate = res.rate;
      mainState.allCount = res.allCount;
      mainState.remindCount = res.remindCount;
      mainState.commonCount = res.commonCount;
      mainState.warningCount = res.warningCount;
      mainState.seriousCount = res.seriousCount;
      mainState.cameraAmount = res.cameraAmount;
      mainState.categoryAmount = res.categoryAmount;
      if (res) {
        mainState.isLoading = false;
      }
    });
    getStatisticsByCamera(paramsObj).then((res) => {
      mainState.cameraCount = res.cameraCountList.length;
      mainState.cameraCountList = res.cameraCountList;
      mainState.locationCountList = res.locationCountList;
      mainState.tempCameraList = res.cameraCountList;
      if (res) {
        mainState.isLoading = false;
      }
      echartsInitCamera();
    });
    getStatisticsByType(paramsObj).then((res) => {
      mainState.typeCountList = res.typeCountList;
      if (res) {
        mainState.isLoading = false;
      }
      echartsInitType();
    });
    getStatisticsByDate(paramsObj).then((res) => {
      mainState.dateCountList = res.dateCountList;
      mainState.lastDateCountList = res.lastDateCountList;
      mainState.tempDateCountList = res.dateCountList;
      if (res) {
        mainState.isLoading = false;
      }
      echartsInitDate();
    });
    getStatisticsByHour(paramsObj).then((res) => {
      mainState.hourCountList = res.hourCountList;
      mainState.lastHourCountList = res.lastHourCountList;
      if (res) {
        mainState.isLoading = false;
      }
      echartsInitHour();
    });
  };

  //重置
  const handleStatisticReset = async (resetParams) => {
    mainState.isSelectedCamera = false;
    mainState.isSelectedCategory = false;
    resetParams.camera.length = 0;
    resetParams.alarm.length = 0;
    // queryStatisticList();
    resetParams.cameraNames = '';
    resetParams.categorys = '';
    //不能把调接口抽成一个方法，不生效
    mainState.searchParams.cameraNames = '';
    mainState.searchParams.categorys = '';
    getCountInfo();
  };
</script>

<style lang="less" scoped>
  .statistic-container {
    overflow-x: auto;
  }
  .header {
    min-width: 1576px !important;
  }
  .static-main {
    min-width: 1600px;

    .loading {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100%;
      width: 100%;
      &-image {
        width: 32px;
        height: 32px;
      }
    }
    .text {
      font-family: Source Han Sans CN;
      color: #a7adc0;
      font-size: 12px;
    }
    :deep .ant-checkbox-indeterminate .ant-checkbox-inner {
      background-color: #7089ed;
      border-color: #7089ed;
      &::after {
        width: 6px;
        height: 1.5px;
        background-color: #ffffff;
        border-radius: 0.4px;
      }
    }

    :deep .ant-checkbox-checked .ant-checkbox-inner {
      background-color: #738ce6;
      border-color: #738ce6;
    }
    :deep .ant-checkbox-wrapper:hover .ant-checkbox-inner,
    :deep .ant-checkbox:hover .ant-checkbox-inner,
    :deep .ant-checkbox-input:focus + .ant-checkbox-inner {
      border-color: #738ce6;
    }

    .alarm-num {
      width: 100%;
      background: var(--background-Color);
      height: 200px;
      display: flex;
      flex: 1;
      flex-shrink: 1;
      justify-content: space-around;
      margin: 0 12px 12px 12px;

      .sum {
        .title {
          font-size: 14px;
          font-weight: 600;
          font-family: Source Han Sans CN;
          color: var(--title-color);
          font-size: 14px;
          padding: 32px 0 0 32px;
        }

        .alarm-sum {
          font-family: 'BebasNeue-Regular';
          font-weight: 700;
          color: var(--title-color);
          font-size: 36px;
          padding: 0 0 0 32px;
        }
        .rate-box {
          display: flex;
          align-items: center;
          padding: 24px;
        }
        .rate {
          font-family: Source Han Sans CN;
          color: #575b66;
          font-size: 12px;
          padding: 12px 4px 0 4px;
        }

        .increase-icon {
          display: inline-block;
          width: 24px;
          height: 16px;
          background: url('/@/assets/svg/icon/icon_trend_increase.svg');
        }

        .decrease-icon {
          display: inline-block;
          width: 24px;
          height: 16px;
          background: url('/@/assets/svg/icon/icon_trend_decrease.svg');
        }
        .icon-information {
          display: inline-block;
          width: 16px;
          height: 16px;
          background: url('/@/assets/svg/icon/info_icon_tips.svg');
        }
      }
      .alarm-level {
        padding: 64px 0 0 0;
        &-info {
          display: flex;
        }
        .label {
          font-family: Source Han Sans CN;
          font-weight: 550;
          color: #8a8d99;
          font-size: 14px;
          line-height: 2;
          min-width: 28px;
        }
        .serious-num,
        .warning-num,
        .common-num,
        .remind-num {
          font-family: DIN Condensed;
          font-weight: 700;
          padding-left: 12px;
          padding-top: 3px;
          min-width: 22px;
        }
        .serious-num {
          color: var(--serious-text-color);
        }
        .warning-num {
          color: var(--common-text-color);
        }
        .common-num {
          color: var(--common-text-color);
        }
        .remind-num {
          color: var(--remind-text-color);
          margin-bottom: 44px;
        }
      }
      .divider {
        width: 2px;
        height: 96px;
        background-color: rgba(211, 216, 230, 0.6);
        border-radius: 0.5px;
        top: 72px;
      }
      .selected {
        padding-top: 70px;

        .camera-selected,
        .algorithm-selected {
          display: flex;
          align-items: center;
          min-width: 72px;
        }
        .camera-num,
        .algorithm-num {
          font-family: DIN Condensed;
          font-weight: 700;
          color: #adb4cc;
          font-size: 16px;
          margin-left: 16px;
          min-width: 40px;
        }
        .camera-icon {
          width: 40px;
          height: 40px;
          display: inline-block;
          background: url('/@/assets/svg/icon/icon_cameraxl_default.svg');
        }

        .algorithm-icon {
          width: 40px;
          height: 40px;
          display: inline-block;
          background: url('/@/assets/svg/icon/icon_algoxl_default.svg');
        }
      }
    }
    .alarm-type {
      background: var(--background-Color);
      width: 100%;
      height: 552px;
      margin: 0 12px 0 12px;
    }

    .alarm-date {
      position: relative;
      width: 100%;

      &-check {
        position: absolute;
        z-index: 999;
        top: 30px;
        right: 100px;
      }
      .date {
        width: 100%;
        height: 360px;
        margin: 0 12px 12px 0px;
        background: var(--background-Color);
      }
    }
    .alarm-hour {
      position: relative;
      width: 100%;
      &-check {
        position: absolute;
        z-index: 999;
        top: 32px;
        right: 32px;
      }
      .hour {
        background: var(--background-Color);
        width: 100%;
        height: 392px;
      }
    }
    .statistic-camera {
      position: relative;
      width: 100%;
      .dropdown {
        position: absolute;
        z-index: 999;
        top: 20px;
        right: 32px;
      }
      .dropdown-box {
        width: 64px;
        height: 20px;
        background-color: var(--netural-color);
        border-radius: 4px;
        display: flex;
        justify-content: center;
        align-items: center;
        font-family: Source Han Sans CN;
        color: #adb4cc;
        font-size: 12px;
        cursor: pointer;
      }
      .dropdown-icon {
        display: inline-block;
        background: url('/@/assets/svg/icon/icon_arrowdown_default.svg');
        width: 8px;
        height: 8px;
        margin-left: 8px;
      }
      .camera {
        background: var(--background-Color);
        width: 100%;
        height: 764px;
        margin: 0 12px 0 -12px;
      }
    }
  }
</style>
