import {BasicColumn} from '/@/components/Table';
import {FormSchema} from '/@/components/Table';
//列表数据
export const columns: BasicColumn[] = [
  {
    title: '相机型号',
    align: 'center',
    dataIndex: 'cameraType',
  },
  {
    title: '用户名',
    align: 'center',
    dataIndex: 'username',
  },
  {
    title: '密码',
    align: 'center',
    dataIndex: 'password',
  },
  {
    title: 'IP',
    align: 'center',
    dataIndex: 'ip',
  },
  {
    title: '端口',
    align: 'center',
    dataIndex: 'port',
  },
  {
    title: '厂家名称',
    align: 'center',
    dataIndex: 'factoryName_dictText',
  },
  {
    title: '码流',
    align: 'center',
    dataIndex: 'streamType_dictText',
    // format: (text, record) => {
    //   let result = ''
    //   switch (record.factoryName) {
    //     case 'hk':
    //       if (text == 'main') result = '主码流'
    //       if (text == 'sub') result = '子码流'
    //       break
    //     case 'dh':
    //       if (text == '0') result = '主码流'
    //       if (text == '1') result = '子码流'
    //       break
    //     case 'ys':
    //       if (text == 'video1') result = '主码流'
    //       if (text == 'video2') result = '子码流'
    //       break
    //     case 'tdwy':
    //       if (text == '1') result = '主码流'
    //       if (text == '2') result = '子码流'
    //       break
    //     default:
    //       result = '未知'
    //       break
    //   }
    //   return result;
    // }
  },
  {
    title: '码流格式',
    align: 'center',
    dataIndex: 'streamFormat_dictText',
  },
  {
    title: '视频协议',
    align: 'center',
    dataIndex: 'protocol',
  },
];
//查询数据
export const searchFormSchema: FormSchema[] = [
  {
    label: '厂家名称',
    field: 'factoryName',
    component: 'JDictSelectTag',
    componentProps: ({formActionType}) => {
      return {
        dictCode: 'factory_name',
        showChooseOption: false,
        onChange: () => {
          const {updateSchema, clearValidate} = formActionType;
          //清除校验
          clearValidate();
          updateSchema([
            {
              label: '码流类型',
              field: 'streamType',
            },
          ]);
        },
      };
    },
  },
  {
    label: '相机型号',
    field: 'cameraType',
    component: 'Input',
  },
];

//表单数据
export const formSchema: FormSchema[] = [
  {
    label: '厂家名称',
    field: 'factoryName',
    component: 'JDictSelectTag',
    rules: [
      {
        required: true,
        trigger: 'blur',
      },
    ],
    componentProps: ({formActionType}) => {
      return {
        dictCode: 'factory_name',
        showChooseOption: false,
        onChange: () => {
          const {updateSchema, clearValidate} = formActionType;
          //清除校验
          clearValidate();
          updateSchema([
            {
              label: '码流类型',
              field: 'streamType',
            },
          ]);
        },
      };
    },
  },
  {
    label: '相机型号',
    field: 'cameraType',
    component: 'Input',
    dynamicRules: () => {
      return [{required: true, message: '请输入相机型号!'}];
    },
  },
  {
    label: '用户名',
    field: 'username',
    component: 'Input',
    componentProps: ({formModel}) => {
      return {
        onblur: () => {
          formatStreamAddress(formModel)
        },
      };
    },
    rules: [
      {
        pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/,
        message: '以英文字母开头，只能包含英文字母、数字、下划线',
        trigger: 'blur',
      },
    ],
  },
  {
    label: '密码',
    field: 'password',
    component: 'InputPassword',
    componentProps: ({formModel}) => {
      return {
        onblur: () => {
          formatStreamAddress(formModel)
        },
      };
    },
    // rules: [
    //   {
    //     pattern: /^[a-zA-Z0-9_.~!@#$^&*()=+%\-]{6,16}$/,
    //     message: '长度应在6-16位之间并且不能有不支持的字符',
    //     trigger: 'blur',
    //   },
    // ],
  },
  {
    label: 'IP',
    field: 'ip',
    component: 'Input',
    componentProps: ({formModel}) => {
      return {
        onblur: () => {
          formatStreamAddress(formModel)
        },
      };
    },
    rules: [
      {
        pattern: /^(?:(?:1[0-9][0-9]\.)|(?:2[0-4][0-9]\.)|(?:25[0-5]\.)|(?:[1-9][0-9]\.)|(?:[0-9]\.)){3}(?:(?:1[0-9][0-9])|(?:2[0-4][0-9])|(?:25[0-5])|(?:[1-9][0-9])|(?:[0-9]))$/,
        message: '请输入正确的IP地址',
        trigger: 'blur',
      },
    ],
  },
  {
    label: '端口',
    field: 'port',
    component: 'InputNumber',
    componentProps: ({formModel}) => {
      return {
        onblur: () => {
          formatStreamAddress(formModel)
        },
      };
    },
    rules: [
      {
        pattern: /^[0-9]*$/,
        message: '请输入正确的端口号',
        trigger: 'blur',
      },
    ],
  },
  {
    label: '码流类型',
    field: 'streamType',
    component: 'JDictSelectTag',
    rules: [
      {
        required: true,
        trigger: 'blur',
      },
    ],
    componentProps: ({formModel}) => {
      return {
        dictCode: 'stream_type',
        showChooseOption: false,
        onClick: () => {
          formatStreamAddress(formModel)
        }
      }
      // switch (formModel.factoryName) {
      //   case 'dh':
      //     result.dictCode = 'stream_type_dh'
      //     break
      //   case 'ys':
      //     result.dictCode = 'stream_type_ys'
      //     break
      //   case 'tdwy':
      //     result.dictCode = 'stream_type_tdwy'
      //     break
      //   default:
      //     result.dictCode = 'stream_type'
      //     break
      // }
      // return result
    },
  },
  {
    label: '码流格式',
    field: 'streamFormat',
    component: 'JDictSelectTag',
    rules: [
      {
        required: true,
        trigger: 'blur',
      },
    ],
    // 码流格式没有用，去掉
    ifShow: ({values}) => values.factoryName == 'hk999',
    componentProps: ({formModel}) => {
      return {
        dictCode: 'stream_format',
        showChooseOption: false,
        onClick: () => {
          formatStreamAddress(formModel)
        },
      };
    },
  },
  {
    label: '视频协议',
    field: 'protocol',
    component: 'Input',
  },
  // TODO 主键隐藏字段，目前写死为ID
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
];

const formatStreamAddress = (formModel: any) => {
  console.log(formModel.streamType, formModel.factoryName)
  let type: string = 'other';
  const rtspPrefix: string = `rtsp://${formModel.username}:${formModel.password}@${formModel.ip}:${formModel.port}`
  switch (formModel.factoryName) {
    case 'hk':
      if (formModel.streamType == 'sub') type = '02'
      if (formModel.streamType == 'main') type = '01'
      formModel.protocol = `${rtspPrefix}/Streaming/Channels/1${type}`
      break
    case 'dh':
      if (formModel.streamType == 'sub') type = '1'
      if (formModel.streamType == 'main') type = '0'
      formModel.protocol = `${rtspPrefix}/cam/realmonitor?channel=1&subtype=${type}`;
      break
    case 'ys':
      if (formModel.streamType == 'sub') type = '1'
      if (formModel.streamType == 'main') type = '0'
      formModel.protocol = `${rtspPrefix}/unicast/c1/s${type}`
      break
    case 'tdwy':
      if (formModel.streamType == 'sub') type = '2'
      if (formModel.streamType == 'main') type = '1'
      formModel.protocol = `${rtspPrefix}/1/${type}`
      break
    default:
      if (formModel.streamType == 'sub') type = '02'
      if (formModel.streamType == 'main') type = '01'
      formModel.protocol = `${rtspPrefix}/Streaming/Channels/1${type}`
      break
  }
}
