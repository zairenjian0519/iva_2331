<template>
  <a-card>
    <template #title>
      <span>网口名称：{{ localFormData.networkCard }}</span>
      <a-tooltip placement="bottom">
        <template #title>
          <span>请确认业务网的ip地址再进行配置, 我们规定 <code style="color:red;">eth1</code> 网口为业务网口, <code
            style="color:red;">eth0</code> 网口为调试网口。</span>
        </template>
        <a-icon type="info-circle" style="float: right"/>
      </a-tooltip>
    </template>
    <a-form :model="localFormData"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 18 }"
            :rules="rules"
            @finish="handleFinish">
      <a-form-item label="获取IP方式" class="form-item" name="ipMode">
        <a-select v-model:value="localFormData.ipMode"
                  :options="ipModeOptions"
                  placeholder="请选择ip获取方式"
                  has-feedback
                  @change="handleIPModeChange"/>
      </a-form-item>
      <a-form-item label="IP地址" class="form-item" name="ip">
        <a-input-group compact>
          <a-input :style="[!isDHCP? {width: 'calc(100% - 65px)'}:'']"
                   v-model:value="localFormData.ip"
                   placeholder="请输入需要配置的IP地址"
                   has-feedback
                   :disabled="isDHCP"/>
          <a-button v-if="!isDHCP" type="primary" @click="handleTest">测试
          </a-button>
        </a-input-group>
      </a-form-item>
      <a-form-item label="子网掩码" class="form-item" name="subnet">
        <a-input v-model:value="localFormData.subnet"
                 placeholder="请输入子网掩码"
                 has-feedback
                 :disabled="isDHCP"/>
      </a-form-item>
      <a-form-item label="网关" class="form-item" name="gateway">
        <a-input v-model:value="localFormData.gateway"
                 placeholder="请输入网关地址"
                 has-feedback
                 :disabled="isDHCP"/>
      </a-form-item>
      <a-form-item label="dns1" class="form-item" name="dns1">
        <a-input v-model:value="localFormData.dns1"
                 placeholder="请输入DNS1地址"
                 has-feedback
                 :disabled="isDHCP"/>
      </a-form-item>
      <a-form-item label="dns2" class="form-item" name="dns2">
        <a-input v-model:value="localFormData.dns2"
                 placeholder="请输入DNS2地址"
                 has-feedback
                 :disabled="isDHCP"/>
      </a-form-item>
      <a-form-item class="form-item" :wrapper-col="{ span: 18, offset: 6 }">
        <a-button style="width:100%;" type="primary" html-type="submit" :loading="submitBtnLoading">设置为网卡参数</a-button>
      </a-form-item>

    </a-form>
  </a-card>
</template>

<script setup>
import {computed, ref, watch} from 'vue';
import {checkIP, checkIpReachable, update} from '../network.api'
import {message} from "ant-design-vue";
import {useMessage} from "/@/hooks/web/useMessage";

const submitBtnLoading = ref(false)

const props = defineProps({
  formData: Object
});

const {formData} = props;

const localFormData = ref({...formData});

const ipv4RegStr = "^(((\\d)|([1-9]\\d)|(1\\d{2})|(2[0-4]\\d)|(25[0-5]))\\.){3}((\\d)|([1-9]\\d)|(1\\d{2})|(2[0-4]\\d)|(25[0-5]))$"

const subnetRegStr = "^((128|192)|2(24|4[08]|5[245]))(\\.(0|(128|192)|2((24)|(4[08])|(5[245])))){3}$"

const validateIp = (_rule, value) => {
  if (localFormData.value.ipMode === 'auto') return Promise.resolve()
  if (!value) return Promise.reject("ip地址不能为空")
  const ipRegExp = new RegExp(ipv4RegStr)
  if (!ipRegExp.test(value)) return Promise.reject("请输入正确的ip地址")
  return Promise.resolve();
}

const validateSubnet = (_rule, value) => {
  if (localFormData.value.ipMode === 'auto') return Promise.resolve()
  if (!value) return Promise.reject("子网掩码不能为空")
  const subnetRegExp = new RegExp(subnetRegStr)
  if (!subnetRegExp.test(value)) return Promise.reject("请输入正确的子网掩码")
  return Promise.resolve();
}

const rules = {
  ipMode: [{required: true, message: '请选择ip获取方式'}],
  ip: [{required: true, validator: validateIp, trigger: 'change'}],
  subnet: [{required: true, validator: validateSubnet, trigger: 'change'}],
  gateway: [{pattern: ipv4RegStr, message: '请输入正确的网关地址', trigger: 'change'}],
  dns1: [{pattern: ipv4RegStr, message: '请输入正确的DNS地址', trigger: 'change'}],
  dns2: [{pattern: ipv4RegStr, message: '请输入正确的DNS地址', trigger: 'change'}],
}

const isDHCP = computed(() => localFormData.value.ipMode === 'auto')

const ipModeOptions = [
  {label: '静态', value: 'manual'},
  {label: 'DHCP', value: 'auto'}
];

async function handleTest() {
  if (!localFormData.value.ip || !localFormData.value.networkCard) {
    message.error("ip地址和网卡地址不能为空")
    return
  }
  await checkIP({ipAddress: localFormData.value.ip, networkCard: localFormData.value.networkCard})

}

const handleFinish = async values => {
  console.log('success', values, localFormData)
  await updateNetwork()
}

const { createConfirm } = useMessage();

async function updateNetwork() {
  submitBtnLoading.value = true;
  const networkCard = localFormData.value.networkCard;

  // eth0 网口修改ip前，检查ip是否可达
  if (networkCard === "eth0") {
    const reachable = await checkIpOk();
    if (!reachable) {
      createConfirm({
        iconType: 'warning',
        title: '提示信息',
        content: '你正在修改调试网口 ip 地址, 修改后可能造成网页访问不了, 确定修改？',
        onOk: async () => {
          await setNetworkInfo();
          submitBtnLoading.value = false;
        },
        onCancel: () => {
          submitBtnLoading.value = false;
        }
      });
      return;
    }
  }

  await setNetworkInfo();
  submitBtnLoading.value = false;
}

async function setNetworkInfo(){
  let dns = []
  if (localFormData.value.dns1 !== '') {
    dns.push(localFormData.value.dns1)
  }
  if (localFormData.value.dns2 !== '') {
    dns.push(localFormData.value.dns2)
  }
  const networkInfo = {
    // networkCard: localFormData.value.networkCard,
    name: localFormData.value.name,
    ipMethod: localFormData.value.ipMode,
    ipAddress: localFormData.value.ip,
    subnetMask: localFormData.value.subnet,
    gateway: localFormData.value.gateway ? localFormData.value.gateway : '',
    dns: dns
  }
  await update(networkInfo)
}

const checkIpOk = async () => {
  const ipAddr = localFormData.value.ip
  if (!ipAddr) return false
  return await checkIpReachable(ipAddr)
}


const handleIPModeChange = () => {
  // If DHCP is selected, clear local form data
  if (localFormData.value.ipMode === 'auto') {
    localFormData.value.ip = '';
    localFormData.value.subnet = '';
    localFormData.value.gateway = '';
    localFormData.value.dns1 = '';
    localFormData.value.dns2 = '';
  }
};

// Update localFormData when formData changes
watch(() => formData, (newFormData) => {
  Object.assign(localFormData.value, newFormData);
});
</script>

<style scoped>
.form-item {
  max-width: 85%;
}

/* 添加你需要的样式 */
</style>
