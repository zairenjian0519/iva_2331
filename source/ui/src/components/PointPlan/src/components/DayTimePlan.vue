<template>
  <div :class="`${prefixCls}-day`" @mouseenter="onMouseEnter" @mouseleave="onMouseLeave">
    <a-space :size="6">
      <div :class="`${prefixCls}-daylabel`"> {{ title }}</div>
      <div :class="`${prefixCls}-daydraw`" @mousedown.stop="onMouseDown">
        <div :class="`${prefixCls}-record`">
          <template v-for="(point, index) in points" :key="index">
            <a-popover placement="top" :visible="point.selected">
              <template #content>
                <a-space direction="vertical" :size="12">
                  <div class="time">
                    <a-space>
                      <a-input v-model:value="point.strTime.split(':')[0]" :min="0" :max="23" style="width: 52px" placeholder="HH" size="small" />
                      <span>:</span>
                      <a-input v-model:value="point.strTime.split(':')[1]" :min="0" :max="59" style="width: 52px" placeholder="MM" size="small" />
                    </a-space>
                  </div>
                  <div>
                    <a-space>
                      <a-button danger size="small" @click="modifyConfirm(index, 'delete')"> 删除 </a-button>
                      <a-button size="small" @click="modifyConfirm(index, 'cancel')"> 取消 </a-button>
                      <a-button type="primary" size="small" @click="modifyConfirm(index, 'confirm')"> 确定 </a-button>
                    </a-space>
                  </div>
                </a-space>
              </template>
              <div :class="[`${prefixCls}-section`, point.selected && `${prefixCls}-point-selected`]" :style="`width: ${point.width}px; left: ${point.left}px`"></div>
            </a-popover>
          </template>

          <template v-if="tempPoint.width">
            <div :class="`${prefixCls}-point`" :style="`width: ${tempPoint.width}px; left: ${tempPoint.left}px`"></div>
            <div :class="`${prefixCls}-tips`" :style="`left: ${tempPoint.left - 19}px`">
              {{ tempPoint.strTime }}
            </div>
          </template>
        </div>
      </div>
      <div :class="`${prefixCls}-daycopy`" @click="onCopy" v-show="copyVisible"></div>
      <div :class="`${prefixCls}-daypopover`">
        <a-popover placement="leftBottom" trigger="hover" :visible="isShowCopyPover">
          <template #content>
            <div>
              <div :class="`${prefixCls}-week`">
                <span>复制到...</span>
                <span><a-checkbox :indeterminate="isIndeterminate" v-model:checked="mainState.checkAll" @change="handleCheckAllChange">全选</a-checkbox></span>
              </div>
              <div style="margin: 15px 0">
                <a-checkbox-group v-model:value="mainState.checkedWeeks" @change="handleCheckedWeekChange">
                  <a-checkbox :disabled="mainState.copyValue === week.value" v-model:value="week.value" class="checkbox-class" v-for="week in mainState.weeks" :key="week.value">{{
                    week.title
                  }}</a-checkbox>
                </a-checkbox-group>
              </div>
              <a-space>
                <base-button :type="'back'" class="back-btn" @click="handleCancel" second-button="取消" />
                <base-button :type="'sure'" class="confirm-btn" @click="handleComplete" primary-buttton="完成" />
              </a-space>
            </div>
          </template>
        </a-popover>
      </div>
      <div :class="`${prefixCls}-dayclear`" @click="onClear" v-show="opVisible"></div>
    </a-space>
  </div>
</template>

<script lang="ts">
  import { defineComponent, reactive, ref, watch } from 'vue';
  import { propTypes } from '/@/utils/propTypes';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { num2Str, PlanInfo, RATE, points } from './dayTimePlan.data';
  import BaseButton from '/@/components/BaseButton/BaseButton.vue';

  export default defineComponent({
    name: 'DayPlan',
    components: {
      BaseButton,
    },
    props: {
      title: propTypes.string.isRequired,
      day: propTypes.number.isRequired,
      value: propTypes.instanceOf(PlanInfo).isRequired,
      max: propTypes.number.def(6),
      curIndex: Number,
    },
    emits: ['change', 'handleCopy', 'handleCheckedWeekChange', 'handleCheckedAllWeekChange'],
    setup(props, { emit }) {
      const { prefixCls } = useDesign('timeplan');
      let points = reactive<points[]>([]);
      const tempPoint = reactive<points>({
        tMType: 0,
        strTime: '00:00',
        width: 0,
        left: 0,
      });

      const opVisible = ref(false);
      const copyVisible = ref(false);
      const isShowCopyPover = ref(false);
      const isIndeterminate = ref(false);

      const mainState = reactive({
        weeks: [
          { title: '星期一', value: 0 },
          { title: '星期二', value: 1 },
          { title: '星期三', value: 2 },
          { title: '星期四', value: 3 },
          { title: '星期五', value: 4 },
          { title: '星期六', value: 5 },
          { title: '星期日', value: 6 },
        ],
        copyValue: 0 as number | undefined,
        checkedWeeks: [] as number[],
        checkAll: false,
        options: [],
      });

      watch(
        () => props.value,
        () => {
          points = props.value.points;
        },
        { immediate: true, deep: true }
      );

      function hiddenNewpoint() {
        tempPoint.width = 0;
        tempPoint.left = 0;
        tempPoint.tMType = 0;
        tempPoint.strTime = '';
      }

      function onMouseDown(ev: MouseEvent) {
        const left = (ev.pageX - (ev.currentTarget as HTMLDivElement).getBoundingClientRect().x) >> 0;
        tempPoint.left = left;
        tempPoint.tMType = (tempPoint.left * RATE) >> 0;
        tempPoint.strTime = num2Str(tempPoint.tMType);

        tempPoint.width = 0.4;
        tempPoint.selected = true;
        let i = 0;
        for (; i < points.length; i++) {
          if (tempPoint.left < points[i].left) {
            break;
          }
        }
        points.splice(i, 0, { ...tempPoint });
        emit('change');

        // hiddenNewpoint();
      }

      function onMouseEnter() {
        opVisible.value = true;
        copyVisible.value = true;
      }

      function onMouseLeave() {
        opVisible.value = false;
        copyVisible.value = false;
        hiddenNewpoint();
        tempPoint.selected = false;
      }

      function modifyConfirm(index: number, op: 'cancel' | 'confirm' | 'delete') {
        points[index].selected = false;
        if (op === 'cancel') {
          return;
        }
        if (op === 'delete') {
          points.splice(index, 1);
          emit('change');
          return;
        }
        if (op !== 'confirm') {
          return;
        }

        emit('change');
      }

      function onClear() {
        points.length = 0;
        emit('change');
      }

      function onCopy() {
        isShowCopyPover.value = true;
        mainState.checkAll = false; //再次点击复制时重置全选按钮
        mainState.copyValue = props.curIndex;
      }

      //复制取消
      const handleCancel = () => {
        isShowCopyPover.value = false;
      };
      //复制完成
      const handleComplete = () => {
        isShowCopyPover.value = false;
        mainState.checkAll = false; //再次点击复制时重置全选按钮
        emit('handleCopy');
      };
      //复制全选
      const handleCheckAllChange = (e: any) => {
        if (e.target.checked) {
          mainState.checkedWeeks = [0, 1, 2, 3, 4, 5, 6];
        } else {
          mainState.checkedWeeks = [];
          mainState.checkedWeeks.push(mainState.copyValue);
        }
        emit('handleCheckedAllWeekChange', mainState.checkedWeeks);
      };
      //非全选
      const handleCheckedWeekChange = (checkedValue) => {
        let checkCount = checkedValue.length;
        mainState.checkAll = checkCount === mainState.weeks.length;
        isIndeterminate.value = checkCount > 0 && checkCount < mainState.weeks.length;
        emit('handleCheckedWeekChange', checkedValue);
      };

      return {
        prefixCls,
        points,
        tempPoint,
        opVisible,
        onClear,
        onCopy,
        onMouseDown,
        onMouseEnter,
        onMouseLeave,
        modifyConfirm,
        copyVisible,
        isShowCopyPover,
        handleCancel,
        handleComplete,
        mainState,
        handleCheckAllChange,
        handleCheckedWeekChange,
        isIndeterminate,
      };
    },
  });
</script>

<style lang="less" scoped>
  @import './dayTimePlan.less';
</style>
