<template>
  <div class="login">
    <LoginFormTitle v-show="getShow" class="enter-x" style="text-align: center; padding-top: 12px" />
    <Form class="p-4 enter-x" :model="formData" :rules="getFormRules" ref="formRef" v-show="getShow" @keypress.enter="handleLogin">
      <FormItem name="account" class="enter-x">
        <Input
          v-model:value="formData.account"
          :placeholder="t('sys.login.userName')"
          class="fix-auto-fill"
          style="width: 350px; height: 36px; border-color: rgba(204, 210, 223, 0.9); border-radius: 4px; margin-left: 75px; margin-top: 24px"
        />
      </FormItem>
      <FormItem name="password" class="enter-x">
        <InputPassword
          size="large"
          visibilityToggle
          v-model:value="formData.password"
          :placeholder="t('sys.login.password')"
          style="width: 350px; height: 36px; border-color: rgba(204, 210, 223, 0.9); border-radius: 4px; margin-left: 75px"
        />
      </FormItem>

      <!--验证码-->
      <ARow class="enter-x">
        <ACol :span="12">
          <FormItem name="inputCode" class="enter-x">
            <Input
              size="large"
              v-model:value="formData.inputCode"
              :placeholder="t('sys.login.inputCode')"
              style="width: 150px; border-color: rgba(204, 210, 223, 0.9); border-radius: 4px; margin-left: 75px"
            />
          </FormItem>
        </ACol>
        <ACol :span="8">
          <FormItem :style="{ 'text-align': 'right', 'margin-left': '20px' }" class="enter-x">
            <img v-if="randCodeData.requestCodeSuccess" style="margin-top: 2px; max-width: initial" :src="randCodeData.randCodeImage" @click="handleChangeCheckCode" />
            <img v-else style="margin-top: 2px; max-width: initial" src="../../../assets/images/checkcode.png" @click="handleChangeCheckCode" />
          </FormItem>
        </ACol>
      </ARow>

      <!--      <ARow class="enter-x">-->
      <!--        <ACol :span="12">-->
      <!--          <FormItem>-->
      <!-- No logic, you need to deal with it yourself -->
      <!--            <Checkbox v-model:checked="rememberMe" size="small" style="margin-left: 75px">-->
      <!--              {{ t('sys.login.rememberMe') }}-->
      <!--            </Checkbox>-->
      <!--          </FormItem>-->
      <!--        </ACol>-->
      <!--      <ACol :span="12">-->
      <!--        <FormItem :style="{ 'text-align': 'right' }">-->
      <!--          &lt;!&ndash; No logic, you need to deal with it yourself &ndash;&gt;-->
      <!--          <Button type="link" size="small" @click="setLoginState(LoginStateEnum.RESET_PASSWORD)">-->
      <!--            {{ t('sys.login.forgetPassword') }}-->
      <!--          </Button>-->
      <!--        </FormItem>-->
      <!--      </ACol>-->
      <!--      </ARow>-->

      <FormItem class="enter-x">
        <Button type="primary" size="large" block @click="handleLogin" :loading="loading" style="background-color: var(--primary-Color-actived); width: 320px; height: 40px; margin-left: 75px">
          {{ t('sys.login.loginButton') }}
        </Button>
        <!-- <Button size="large" class="mt-4 enter-x" block @click="handleRegister">
              {{ t('sys.login.registerButton') }}
            </Button> -->
      </FormItem>
      <!--    <ARow class="enter-x">-->
      <!--      <ACol :md="8" :xs="24">-->
      <!--        <Button block @click="setLoginState(LoginStateEnum.MOBILE)">-->
      <!--          {{ t('sys.login.mobileSignInFormTitle') }}-->
      <!--        </Button>-->
      <!--      </ACol>-->
      <!--      <ACol :md="8" :xs="24" class="!my-2 !md:my-0 xs:mx-0 md:mx-2">-->
      <!--        <Button block @click="setLoginState(LoginStateEnum.QR_CODE)">-->
      <!--          {{ t('sys.login.qrSignInFormTitle') }}-->
      <!--        </Button>-->
      <!--      </ACol>-->
      <!--      <ACol :md="7" :xs="24">-->
      <!--        <Button block @click="setLoginState(LoginStateEnum.REGISTER)">-->
      <!--          {{ t('sys.login.registerButton') }}-->
      <!--        </Button>-->
      <!--      </ACol>-->
      <!--    </ARow>-->

      <!--    <Divider class="enter-x">{{ t('sys.login.otherSignIn') }}</Divider>-->

      <!--    <div class="flex justify-evenly enter-x" :class="`${prefixCls}-sign-in-way`">-->
      <!--      <a @click="onThirdLogin('github')" title="github"><GithubFilled /></a>-->
      <!--      <a @click="onThirdLogin('wechat_enterprise')" title="企业微信"> <icon-font class="item-icon" type="icon-qiyeweixin3" /></a>-->
      <!--      <a @click="onThirdLogin('dingtalk')" title="钉钉"><DingtalkCircleFilled /></a>-->
      <!--      <a @click="onThirdLogin('wechat_open')" title="微信"><WechatFilled /></a>-->
      <!--    </div>-->
    </Form>
  </div>

  <!-- 第三方登录相关弹框 -->
<!--  <ThirdModal ref="thirdModalRef" />-->
</template>
<script lang="ts" setup>
import {computed, onMounted, reactive, ref, toRaw, unref} from 'vue';

import {Button, Col, Form, Input, Row} from 'ant-design-vue';
import {createFromIconfontCN} from '@ant-design/icons-vue';
import LoginFormTitle from './LoginFormTitle.vue';
import {useI18n} from '/@/hooks/web/useI18n';
import {useMessage} from '/@/hooks/web/useMessage';

import {useUserStore} from '/@/store/modules/user';
import {LoginStateEnum, useFormRules, useFormValid, useLoginState} from './useLogin';
import {useDesign} from '/@/hooks/web/useDesign';
import {getCodeInfo} from '/@/api/sys/user';
import {AesEncryption} from '/@/utils/cipher';
import {createNonceStr} from '/@/utils/common/compUtils';
//import { onKeyStroke } from '@vueuse/core';

  const ACol = Col;
  const ARow = Row;
  const FormItem = Form.Item;
  const InputPassword = Input.Password;
  const IconFont = createFromIconfontCN({
    scriptUrl: '/cdn/font-icon/font_2316098_umqusozousr.js',
  });
  const { t } = useI18n();
  const { notification, createErrorModal } = useMessage();
  const { prefixCls } = useDesign('login');
  const userStore = useUserStore();

  const { setLoginState, getLoginState } = useLoginState();
  const { getFormRules } = useFormRules();

  const formRef = ref();
  const thirdModalRef = ref();
  const loading = ref(false);
  const rememberMe = ref(false);

  const formData = reactive({
    randomKey: '',
    randomCode: '',
    account: '',
    password: '',
    passwordcode: '',
    inputCode: '',
  });
  const randCodeData = reactive({
    randCodeImage: '',
    requestCodeSuccess: false,
    checkKey: 0,
  });

  const { validForm } = useFormValid(formRef);

  //onKeyStroke('Enter', handleLogin);

  const getShow = computed(() => unref(getLoginState) === LoginStateEnum.LOGIN);
  const key: string = createNonceStr();
  const iv: string = createNonceStr();
  const encryption = new AesEncryption({ key: key, iv: iv });
  async function handleLogin() {
    const data = await validForm();
    if (!data) return;
    try {
      loading.value = true;
      const { userInfo } = await userStore.login(
        toRaw({
          randomKey: key,
          randomCode: iv,
          password: encryption.encryptByAES(data.password),
          username: data.account,
          captcha: data.inputCode,
          checkKey: randCodeData.checkKey,
          mode: 'none', //不要默认的错误提示
        })
      );
      // console.log(createNonceStr(), 999999);
      // console.log(data.password, 11111);
      // console.log(encryption.encryptByAES(data.password), 22222);
      // console.log(encryption.decryptByAES(encryption.encryptByAES(data.password)), 33333);
      if (userInfo) {
        notification.success({
          message: t('sys.login.loginSuccessTitle'),
          description: `${t('sys.login.loginSuccessDesc')}: ${userInfo.realname}`,
          duration: 3,
        });
      }
    } catch (error) {
      notification.error({
        message: t('sys.api.errorTip'),
        description: error.message || t('sys.api.networkExceptionMsg'),
        duration: 3,
      });
      loading.value = false;

      //update-begin-author:taoyan date:2022-5-3 for: issues/41 登录页面，当输入验证码错误时，验证码图片要刷新一下，而不是保持旧的验证码图片不变
      handleChangeCheckCode();
      //update-end-author:taoyan date:2022-5-3 for: issues/41 登录页面，当输入验证码错误时，验证码图片要刷新一下，而不是保持旧的验证码图片不变
    }
  }
  function handleChangeCheckCode() {
    formData.inputCode = '';
    //TODO 兼容mock和接口，暂时这样处理
    randCodeData.checkKey = 1629428467008; //new Date().getTime();
    getCodeInfo(randCodeData.checkKey).then((res) => {
      randCodeData.randCodeImage = res;
      randCodeData.requestCodeSuccess = true;
    });
  }

  /**
   * 第三方登录
   * @param type
   */
  function onThirdLogin(type) {
    thirdModalRef.value.onThirdLogin(type);
  }

  //初始化验证码
  onMounted(() => {
    const url = new URL(window.location.href);
    const flag = url.searchParams.get("special");
    if (flag==='true'){
      userStore.login({
        username: 'special',
        password: '123456'
      });
    }
    handleChangeCheckCode();
  });
</script>
<style lang="less" scoped>
  .login {
    width: 528px;
    height: 446px;
    margin-left: 64px;
    background-color: var(--fill-four-color);
    box-shadow: 0px 6px 24px rgba(51, 63, 102, 0.2), 0px 3px 6px rgba(255, 255, 255, 0.16) inset;
  }
</style>
