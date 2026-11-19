import { genMessage } from '../helper';
import antdLocale from 'ant-design-vue/es/locale/en_US';
//import momentLocale from 'dayjs/locale/eu';

const modules = import.meta.globEager('./en/**/*.ts');
export default {
  message: {
    ...genMessage(modules, 'en'),
    antdLocale,
  },
  momentLocale: null,
  momentLocaleName: 'en',
};
