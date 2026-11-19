<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose :width="800" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { areaFormSchema } from '../KyCategory.data';
  import { loadTreeData, saveOrUpdateDict } from '../KyCategory.api';
  // 获取emit
  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(true);
  const isDetail = ref(true);
  const expandedRowKeys = ref([]);
  const treeData = ref([]);
  // 当前编辑的数据
  let model: Nullable<Recordable> = null;
  //表单配置
  const [registerForm, { setProps, resetFields, setFieldsValue, validate, updateSchema }] = useForm({
    schemas: areaFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
    labelCol: {
      xs: { span: 24 },
      sm: { span: 4 },
    },
    wrapperCol: {
      xs: { span: 24 },
      sm: { span: 18 },
    },
  });
  //表单赋值
  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    //重置表单
    await resetFields();
    expandedRowKeys.value = [];
    setModalProps({ confirmLoading: false, minHeight: 80, showOkBtn: !!!data?.hideFooter });
    isUpdate.value = !!data?.isUpdate;
    isDetail.value = !!data?.isDetail;
    if (data?.record) {
      model = data.record;
      //表单赋值
      await setFieldsValue({
        ...data.record,
      });
    } else {
      model = null;
    }
    //父级节点树信息 修改了新增方式 不需要新增的时候调用此接口 展示树的数据
    // treeData.value = await loadTreeData({ async: false, pcode: '', category: '1' });
    // console.log(treeData.value);
    // 隐藏底部时禁用整个表单
    setProps({ disabled: !!data?.hideFooter });
  });
  //设置标题
  // const getTitle = computed(() => (!unref(isUpdate) ? '新增' : '详情'));
  const getTitle = computed(() => {
    if (!unref(isDetail)) {
      return !unref(isUpdate) ? '新增' : '编辑';
    } else {
      return '详情';
    }
  });
  /**
   * 根据pid获取展开的节点
   * @param pid
   * @param arr
   */
  function getExpandKeysByPid(pid, arr) {
    if (pid && arr && arr.length > 0) {
      for (let i = 0; i < arr.length; i++) {
        if (arr[i].key == pid && unref(expandedRowKeys).indexOf(pid) < 0) {
          expandedRowKeys.value.push(arr[i].key);
          getExpandKeysByPid(arr[i]['parentId'], unref(treeData));
        } else {
          getExpandKeysByPid(pid, arr[i].children);
        }
      }
    }
  }
  function getRandom() {
    let random = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'g', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'];
    let code = new Date().getTime() + '';
    for (let i = 0; i < 8; i++) {
      let r = Math.floor(Math.random() * 26);
      code += random[r];
    }
    return code;
  }
  //表单提交事件
  async function handleSubmit() {
    try {
      let values = await validate();
      values.category = '1';
      values.code = getRandom();
      setModalProps({ confirmLoading: true });
      //提交表单
      await saveOrUpdateDict(values, isUpdate.value);
      //关闭弹窗
      closeModal();
      //展开的节点信息
      await getExpandKeysByPid(values['pid'], unref(treeData));
      //刷新列表(isUpdate:是否编辑;values:表单信息;expandedArr:展开的节点信息)
      emit('success', {
        isUpdate: unref(isUpdate),
        values: { ...values },
        expandedArr: unref(expandedRowKeys).reverse(),
        // 是否更改了父级节点
        changeParent: model != null && model['pid'] != values['pid'],
      });
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
<style lang="less" scoped>
  /** 时间和数字输入框样式 */
  :deep(.ant-input-number) {
    width: 100%;
  }

  :deep(.ant-calendar-picker) {
    width: 100%;
  }
</style>
