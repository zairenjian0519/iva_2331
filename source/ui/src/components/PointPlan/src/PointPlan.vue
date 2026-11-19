<template>
  <div class="wrapper">
    <div class="operation">
      <p class="divider"></p>
      <div class="reset-all" @click="handleReset">
        <i class="reset-icon"></i>
        <span>重置</span>
      </div>
      <div class="clear-all" @click="handleClearAll">
        <i class="clear-icon"></i>
        <span>全部</span>
      </div>
    </div>

    <template v-for="(plan, index) in timeplans" :key="plan.title">
      <DayTimePlan
        class="dayplan"
        :title="plan.title"
        :value="plan"
        :max="max"
        :day="index"
        @change="onDayPlanChange(index)"
        @handleCopy="handleCopy(index)"
        :curIndex="index"
        @handleCheckedWeekChange="handleCheckedWeekChange"
        @handleCheckedAllWeekChange="handleCheckedAllWeekChange"
      />
    </template>
  </div>
</template>
<script lang="ts">
  import { defineComponent, reactive, watch } from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { propTypes } from '/@/utils/propTypes';
  import { Divider } from 'ant-design-vue';
  import { PlanInfo, num2Str, WIDTH, defaultnPoint, defaultLeft } from './components/dayTimePlan.data';
  import DayTimePlan from './components/DayTimePlan.vue';

  export default defineComponent({
    name: 'TimePlan',
    components: {
      DayTimePlan,
    },
    props: {
      value: propTypes.string.isRequired,
      max: propTypes.number.def(6),
    },
    emits: ['change', 'update:value'],
    setup(props, { emit }) {
      const timeplans = reactive<PlanInfo[]>([]);
      const { t } = useI18n();
      let oldValue: string;

      const mainState = reactive({
        checkedWeek: [],
        checkedAllWeek: [],
      });

      watch(
        () => props.value,
        () => {
          if (oldValue && oldValue === props.value) {
            return;
          }
          setTime();
          emit('update:value', getTime());
        },
        { immediate: true }
      );
      function getTime() {
        return (oldValue = timeplans.map((p) => p.value).join(';'));
      }

      function setTime() {
        let { value } = props;
        timeplans.length = 0;
        let i = 0;
        if (value) {
          for (const v of value.split(';')) {
            timeplans.push(new PlanInfo({ title: t('component.timeplan.week' + i++), value: v }));
          }
        }
        while (i < 7) {
          timeplans.push(new PlanInfo({ title: t('component.timeplan.week' + i++) }));
        }
      }

      function onDayPlanChange(index: number) {
        timeplans[index].onChange();
        emitChange();
      }

      function emitChange() {
        emit('change', getTime());
        emit('update:value', getTime());
      }

      //复制完成按钮
      function handleCopy(index: number) {
        if (mainState.checkedAllWeek.length === 0) {
          mainState.checkedWeek.forEach((item) => {
            timeplans[item].setValue(timeplans[index].value);
            timeplans[item].onChange();
          });

          mainState.checkedWeek.length = 0;
        } else {
          mainState.checkedAllWeek.forEach((item) => {
            if (item !== index) {
              timeplans[item].setValue(timeplans[index].value);
              timeplans[item].onChange();
            }
          });
        }
        mainState.checkedAllWeek.length = 0;
        emitChange();
      }
      //复制全选
      const handleCheckedAllWeekChange = (allValue) => {
        mainState.checkedAllWeek = allValue;
      };
      //复制非全选
      const handleCheckedWeekChange = (checkedValue) => {
        mainState.checkedWeek = checkedValue;
      };
      //重置
      const handleReset = () => {
        timeplans.forEach((item) => {
          item.value = '06:00';
          item.points.length = 0;
          item.points.push({
            tMType: defaultnPoint,
            strTime: num2Str(defaultnPoint),
            width: WIDTH / 1440,
            left: defaultLeft,
          });
          item.onChange();
        });
        emitChange();
      };
      //   全部清除;
      const handleClearAll = () => {
        timeplans.forEach((item) => {
          item.points.length = 0;
          item.onChange();
        });
        emitChange();
      };

      return {
        timeplans,
        onDayPlanChange,
        handleCopy,
        handleCheckedAllWeekChange,
        handleCheckedWeekChange,
        mainState,
        handleReset,
        handleClearAll,
        defaultnPoint,
        defaultLeft,
        WIDTH,
        Divider,
      };
    },
  });
</script>
<style lang="less" scoped>
  .wrapper {
    position: relative;

    .operation {
      position: absolute;
      left: 790px;
      top: 0px;
      font-family: Source Han Sans CN;
      color: var(--icon-Color);
      font-size: 13px;
    }
    .divider {
      position: absolute;
      left: -12px;
      width: 0px;
      height: 38.41px;
      border: 1px solid;
      border-color: var(--weak-split-line-color);
    }
    .reset-all {
      cursor: pointer;

      .reset-icon {
        background: url('/@/assets/svg/icon/editor_btn_resetxs_default.svg');
        width: 12px;
        height: 12px;
        display: inline-block;
        margin-right: 8px;
        &:hover {
          background: url('/@/assets/svg/icon/editor_btn_resetxs_default_hover.svg');
        }
      }
    }
    .clear-all {
      cursor: pointer;
      .clear-icon {
        background: url('/@/assets/svg/icon/editor_btn_cleanxs_default.svg');
        width: 12px;
        height: 12px;
        display: inline-block;
        margin-right: 8px;
        &:hover {
          background: url('/@/assets/svg/icon/editor_btn_cleanxs_hover.svg');
        }
      }
    }
  }
</style>
