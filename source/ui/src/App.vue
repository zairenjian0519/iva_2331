<template>
  <ConfigProvider :locale="getAntdLocale">
    <AppProvider>
      <RouterView />
    </AppProvider>
  </ConfigProvider>
</template>

<script lang="ts" setup>
  import { ConfigProvider } from 'ant-design-vue';
  import { AppProvider } from '/@/components/Application';
  import { useTitle } from '/@/hooks/web/useTitle';
  import { useLocale } from '/@/locales/useLocale';

  // support Multi-language
  const { getAntdLocale } = useLocale();

  useTitle();
</script>
<style lang="less" scoped>
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
</style>
