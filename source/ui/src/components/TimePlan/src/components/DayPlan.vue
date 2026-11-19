<template>
  <div :class="`${prefixCls}-day`" @mouseenter="onMouseEnter" @mouseleave="onMouseLeave">
    <a-space :size="6">
      <div :class="`${prefixCls}-daylabel`">
        {{ title }}
      </div>
      <div :class="`${prefixCls}-daydraw`" @mousedown.stop="onMouseDown">
        <div :class="`${prefixCls}-record`">
          <template v-for="(section, index) in sections" :key="index">
            <a-popover placement="top" :visible="section.selected" @visibleChange="(e) => visibleChange(e, section)">
              <template #content>
                <a-space direction="vertical" :size="12">
                  <div>
                    <a-space>
                      <a-input-number v-model:value="section.bgnH" :min="0" :max="23" style="width: 52px" placeholder="HH" size="small" />
                      <span>:</span>
                      <a-input-number v-model:value="section.bgnM" :min="0" :max="59" style="width: 52px" placeholder="MM" size="small" />
                      <span>--</span>
                      <a-input-number v-model:value="section.endH" :min="0" :max="23" style="width: 52px" placeholder="HH" size="small" />
                      <span>:</span>
                      <a-input-number v-model:value="section.endM" :min="0" :max="59" style="width: 52px" placeholder="MM" size="small" />
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
              <div :class="[`${prefixCls}-section`, section.selected && `${prefixCls}-section-selected`]" :style="`width: ${section.width}px; left: ${section.left}px`"></div>
            </a-popover>
          </template>
          <template v-if="tempSection.width">
            <div :class="`${prefixCls}-section`" :style="`width: ${tempSection.width}px; left: ${tempSection.left}px`"></div>
            <div :class="`${prefixCls}-tips`" :style="`left: ${tempSection.left - 19}px`">
              {{ tempSection.sBgn }}
            </div>
            <div :class="`${prefixCls}-tips`" :style="`left: ${tempSection.left + tempSection.width - 19}px`">
              {{ tempSection.sEnd }}
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
  import { num2Str, PlanInfo, RATE, Section, TIMES, WIDTH } from './dayPlan.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import BaseButton from '/@/components/BaseButton/BaseButton.vue';

  const { createMessage } = useMessage();

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
    emits: ['change', 'popVisibleChange', 'handleCopy', 'handleCheckedWeekChange', 'handleCheckedAllWeekChange'],
    setup(props, { emit }) {
      const { prefixCls } = useDesign('timeplan');

      const sections = ref<Section[]>([]);

      const tempSection = reactive<Section>({
        nBgn: 0,
        nEnd: 0,
        sBgn: '00:00',
        sEnd: '00:00',
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
        copyValue: 0 as number,
        checkedWeeks: [] as number[],
        checkAll: false,
        options: [],
      });

      watch(
        () => props.value,
        () => {
          sections.value = props.value.sections;
        },
        { immediate: true }
      );

      function hiddenNewSection() {
        tempSection.width = 0;
        tempSection.left = 0;
        tempSection.nBgn = 0;
        tempSection.nEnd = 0;
        tempSection.sBgn = '';
        tempSection.sEnd = '';
      }

      function getSectionByPoint(x: number) {
        const res = {} as { point: Section; next: Section };
        for (const s of sections.value) {
          if (s.left > x) {
            res.next = s;
            break;
          }
        }
        for (const s of sections.value) {
          if (s.left <= x && s.left + s.width >= x) {
            res.point = s;
            break;
          }
        }
        return res;
      }

      function insertSection(moveBeginX: number, left: number, xMax: number) {
        if (sections.value.length >= props.max) {
          return;
        }

        tempSection.left = left;
        tempSection.nBgn = (tempSection.left * RATE) >> 0;
        tempSection.sBgn = num2Str(tempSection.nBgn);

        let tempX: number;

        function onDrawMouseMove(evMv: MouseEvent) {
          evMv.stopPropagation();
          evMv.preventDefault();

          tempX = evMv.clientX >> 0;
          if (tempX < moveBeginX) {
            tempX = moveBeginX + 1;
          }
          if (tempSection.left + (tempX -= moveBeginX) >= xMax) {
            tempX = tempSection.width = (xMax - tempSection.left) >> 0 || 1;
          } else {
            tempSection.width = tempX;
          }

          tempSection.nEnd = ((tempSection.left + tempX) * RATE) >> 0;
          if (tempSection.nEnd > TIMES) {
            tempSection.nEnd = TIMES;
          }
          tempSection.sEnd = num2Str(tempSection.nEnd);
        }

        function onDrawMouseUp(evUp: MouseEvent) {
          document.body.removeEventListener('mousemove', onDrawMouseMove, true);
          document.body.removeEventListener('mouseup', onDrawMouseUp, true);
          evUp.stopPropagation();
          evUp.preventDefault();

          tempX = evUp.clientX >> 0;
          if (tempX > moveBeginX) {
            let i = 0;
            for (; i < sections.value.length; i++) {
              if (tempSection.left < sections.value[i].left) {
                break;
              }
            }
            sections.value.splice(i, 0, { ...tempSection });
            emit('change');
          }
          hiddenNewSection();
        }

        document.body.addEventListener('mousemove', onDrawMouseMove, true);
        document.body.addEventListener('mouseup', onDrawMouseUp, true);
      }

      function onMouseDown(ev: MouseEvent) {
        const left = ev.pageX - (ev.currentTarget as HTMLDivElement).getBoundingClientRect().x;
        const res = getSectionByPoint(left);

        if (res.point) {
          // moveSection(res.point);
        } else {
          let max = WIDTH;
          if (res.next) {
            max = res.next.left - 1;
          }
          insertSection(ev.pageX >> 0, left >> 0, max);
        }
      }

      function onMouseEnter() {
        opVisible.value = true;
        copyVisible.value = true;
      }

      function onMouseLeave() {
        opVisible.value = false;
        copyVisible.value = false;
      }

      function visibleChange(visible: boolean, section: Section) {
        emit('popVisibleChange');

        for (const s of sections.value) {
          s.selected = false;
        }
        if (!(section.selected = visible)) {
          return;
        }
        section.bgnH = (section.nBgn / 60) >> 0;
        section.bgnM = section.nBgn % 60;
        section.endH = (section.nEnd / 60) >> 0;
        section.endM = section.nEnd % 60;
      }

      function modifyConfirm(index: number, op: 'cancel' | 'confirm' | 'delete') {
        sections.value[index].selected = false;
        if (op === 'cancel') {
          return;
        }
        if (op === 'delete') {
          sections.value.splice(index, 1);
          emit('change');
          return;
        }
        if (op !== 'confirm') {
          return;
        }
        const s = sections.value[index];
        let nBgn = (s.bgnH as number) * 60 + (s.bgnM as number),
          nEnd = (s.endH as number) * 60 + (s.endM as number);

        if (nBgn === nEnd) {
          createMessage.error('开始时间与结束时间需不同');
          return;
        }

        if (nBgn > nEnd) {
          [nEnd, nBgn] = [nBgn, nEnd];
        }
        if (sections.value[index - 1]?.nEnd >= nBgn) {
          nBgn = sections.value[index - 1].nEnd + 1;
        }
        if (sections.value[index + 1]?.nBgn <= nEnd) {
          nEnd = sections.value[index + 1].nBgn - 1;
        }

        s.nBgn = nBgn;
        s.nEnd = nEnd;
        s.sBgn = num2Str(nBgn);
        s.sEnd = num2Str(nEnd);
        s.left = (nBgn / RATE) >> 0;
        s.width = ((nEnd - nBgn) / RATE) >> 0;
        emit('change');
      }

      function onClear() {
        sections.value.length = 0;
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
        sections,
        tempSection,
        opVisible,
        onClear,
        onCopy,
        onMouseDown,
        onMouseEnter,
        onMouseLeave,
        visibleChange,
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
  @import './timeplan.less';
</style>
