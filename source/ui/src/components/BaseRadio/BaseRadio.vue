<template>
  <div class="base-radio">
    <RadioGroup v-model:value="radioValue" @change="handleChange">
      <Radio :value="1" :style="radioStyle">通过</Radio>
      <Radio :value="2" :style="radioStyle">作废</Radio>
    </RadioGroup>
  </div>
</template>
<script lang="ts" setup>
  import { Radio, RadioGroup } from 'ant-design-vue';
  import { ref, reactive, defineEmits, computed } from 'vue';
  const props = defineProps({
    checkedPass: Boolean,
    checkedCancel: Boolean,
    value: Number,
  });
  const emit = defineEmits(['val', 'update:value', 'update:checkedCancel', 'handleChange']);
  // const radioValue = ref(props.value);

  const radioValue = computed({
    get() {
      return props.value;
    },
    set(val) {
      emit('val', val);
    },
  });

  const radioStyle = reactive({
    display: 'flex',
  });

  //改变状态
  const handleChange = (e) => {
    emit('handleChange', e);
  };
</script>
<style lang="less" scoped>
  .base-radio {
    :deep .ant-radio-inner {
      width: 12px;
      height: 12px;
      border: 0.75px solid;
      border-color: #d3d9e8;
      border-radius: 6px;
    }
    :deep .ant-radio-wrapper:hover .ant-radio,
    :deep .ant-radio:hover .ant-radio-inner,
    :deep .ant-radio-input:focus + .ant-radio-inner {
      border-color: #7089ed;
      width: 12px;
      height: 12px;
    }
    :deep .ant-radio-checked::after {
      width: 12px;
      height: 12px;
      margin-left: -6px;
      margin-top: -6px;
      background-color: #7089ed;
    }

    :deep .ant-radio-checked .ant-radio-inner::after {
      width: 12px;
      height: 12px;
      background-color: #7089ed;
      border-radius: 6px;
      position: absolute;
      top: 50%;
      left: 50%;
      margin-left: -6px;
      margin-top: -6px;
    }
    :deep .ant-radio-input:focus + .ant-radio-inner {
      box-shadow: none;
    }
  }
</style>
