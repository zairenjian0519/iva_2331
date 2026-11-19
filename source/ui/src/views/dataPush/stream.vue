<template>
  <div style="margin: 10px">
    <a-card title="业务地址配置">
      <a-form :model="formState"
              :label-col="{ span: 9 }"
              :wrapper-col="{ span: 6 }"
              autocomplete="off"
              @finish="onFinish">
        <a-form-item label="开关" name="enable">
          <div style="display: flex;justify-content: space-between;">
            <a-switch v-model:checked="formState.enable"/>
            <a-tag :color="formState.enable?'success':'default'">
              {{ formState.enable ? '开启' : '关闭' }}
            </a-tag>
          </div>
        </a-form-item>
        <a-form-item label="业务IP" name="bizIp" :rules="rules.bizIp">
          <a-input v-model:value="formState.bizIp" placeholder="请输入映射后的地址"/>
        </a-form-item>
        <a-form-item label="业务端口" name="bizPort" :rules="rules.bizPort">
          <a-input-number max="65535" min="0" v-model:value="formState.bizPort" style="width: 100%"
                          placeholder="请输入映射后的端口"/>
        </a-form-item>
        <a-form-item :wrapper-col="{offset: 13}">
          <a-button type="primary" style="width: 135px" html-type="submit">修 改 / 保 存</a-button>
        </a-form-item>
        <a-form-item :wrapper-col="{offset: 7}">
          <p style="font-size-adjust: inherit;font-weight: bold">说明：</p>
          <p>1. 如果需要直接访问盒子中的资源，比如视频流或者其他资源，可以此处配置业务地址，外网可通过业务地址访问。</p>
          <p>2. 非专业人士，请勿乱配置，配置错误可能会导致无法访问盒子中的资源。</p>
        </a-form-item>
      </a-form>
    </a-card>
  </div>

</template>

<script setup>
import {onMounted, reactive} from "vue";
import {getStreamSetting, saveStreamSetting} from "/@/views/dataPush/dataPush.api";
import {message} from "ant-design-vue";

const formState = reactive({
  enable: false,
  bizIp: null,
  bizPort: null
})

const ipRegExpStr = "^(((\\d)|([1-9]\\d)|(1\\d{2})|(2[0-4]\\d)|(25[0-5]))\\.){3}((\\d)|([1-9]\\d)|(1\\d{2})|(2[0-4]\\d)|(25[0-5]))$"

const rules = {
  bizIp: [
    {required: true, message: '请输入映射后的地址'},
    {max: 30, message: '地址长度不能超过30'},
    {pattern: ipRegExpStr, message: '请输入正确的ip地址'}
  ],
  bizPort: [
    {required: true, message: '请输入映射后的端口'}
  ]
}

const onFinish = async (values) => {
  await saveStreamSetting(values)
  message.success("操作成功")
  await refresh()
}

const refresh = async () => {
  const streamBizInfo = await getStreamSetting()
  if (streamBizInfo) Object.assign(formState, streamBizInfo)
}

onMounted(() => {
  refresh()
})

</script>

<style scoped lang="less">

</style>
