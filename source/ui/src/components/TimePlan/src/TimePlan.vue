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
      <DayPlan
        class="dayplan"
        :title="plan.title"
        :value="plan"
        :max="max"
        :day="index"
        @change="onDayPlanChange(index)"
        @popVisibleChange="onPopVisibleChange(index)"
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
  import { PlanInfo, num2Str, WIDTH, defaultnBgn, defaultnEnd, defaultLeft } from './components/dayPlan.data';
  import DayPlan from './components/DayPlan.vue';

  export default defineComponent({
    name: 'TimePlan',
    components: {
      DayPlan,
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

      function onPopVisibleChange(index: number) {
        for (const plan of timeplans) {
          if (plan == timeplans[index]) {
            continue;
          }
          for (const s of timeplans[index].sections) {
            s.selected !== false;
          }
        }
      }
      //复制完成按钮
      function handleCopy(index: number) {
        // console.log(mainState.checkedAllWeek, 'index');
        if (mainState.checkedAllWeek.length === 0) {
          // console.log(mainState.checkedAllWeek, 'checkedAllWeek');
          mainState.checkedWeek.forEach((item) => {
            timeplans[item].setValue(timeplans[index].value);
            timeplans[item].onChange();
            // onDayPlanChange(item);
            //怎么消除引用
            // timeplans[item].value = timeplans[index].value;
            // timeplans[item].sections.length = 0;
            // let tempRes: PlanInfo[] = [];
            // for (let i = 0; i < timeplans[index].sections.length; i++) {
            //   tempRes.push(JSON.parse(JSON.stringify(timeplans[index].sections[i])));
            // }
            // timeplans[item].sections.push(...tempRes);
            // for (const s of timeplans[index].sections) {
            //   s.selected = false;
            // }
          });
          mainState.checkedWeek.length = 0;
          // console.log(timeplans, 'timeplans');
        } else {
          mainState.checkedAllWeek.forEach((item) => {
            if (item !== index) {
              //怎么消除引用
              timeplans[item].setValue(timeplans[index].value);
              timeplans[item].onChange();
              // onDayPlanChange(item);
              // timeplans[item].sections.length = 0;
              // let tempAllRes: PlanInfo[] = [];
              // for (let i = 0; i < timeplans[index].sections.length; i++) {
              //   tempAllRes.push(JSON.parse(JSON.stringify(timeplans[index].sections[i])));
              // }
              // timeplans[item].sections.push(...tempAllRes);
              // for (const s of timeplans[index].sections) {
              //   s.selected = false;
              // }
            }
          });
        }
        mainState.checkedAllWeek.length = 0;
        emitChange();
        // console.log(timeplans, 'timeplans');
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
          item.value = '06:00-18:00';
          item.sections.length = 0;
          item.sections.push({
            nBgn: defaultnBgn,
            nEnd: defaultnEnd,
            sBgn: num2Str(defaultnBgn),
            sEnd: num2Str(defaultnEnd),
            width: WIDTH / 2,
            left: defaultLeft,
          });
          item.onChange();
        });
        emitChange();
      };
      //全部清除
      const handleClearAll = () => {
        timeplans.forEach((item) => {
          item.sections.length = 0;
          item.onChange();
        });
        emitChange();
      };

      return {
        timeplans,
        onPopVisibleChange,
        onDayPlanChange,
        handleCopy,
        handleCheckedAllWeekChange,
        handleCheckedWeekChange,
        mainState,
        handleReset,
        handleClearAll,
        defaultnBgn,
        defaultnEnd,
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
