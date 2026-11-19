// 封装到一个js文件里面，避免使用时代码累赘
import { message } from 'ant-design-vue';
import { defineComponent, h, createVNode } from "vue";

/*
    h: 是content自带的参数，表示createElement
    content：文本内容
*/
message.config({
    maxCount: 1,
  });
const createElementVNode = (x, content) => {
  
    // 定义文本内容及样式（这里的样式延用的官方的）
    const innerText = h('span', { style: { color: '#4a5970', fontSize: '14px', cursor: 'pointer' },
     on: { click: () => message.destroy() }, }
     , content,);
    // 自定义关闭图标
    const innerIcon = h('CloseOutlined', {
        style: { marginLeft: '10px', color: '#ccc', cursor: 'pointer' },
        attrs: { type: 'close' },
        onClick: { click: () => message.destroy() },
    });
    let innerIcons =  h('span', {  onClick: () => message.destroy(),style: {marginLeft: '25px', color: '#409EFF', fontSize: '14px',cursor: 'pointer' }},'x')
    // let innerIcons =  h('span', {   onClick: () => message.destroy(),style: {marginLeft: '10px', color: '#4a5970', fontSize: '14px', cursor: 'pointer' }},"X")
    console.log(innerIcon);
    // 合到一个 VNode 节点中
    const container = h('span', {}, [innerText, innerIcons]);
    // 最终返回
    return container;
};
// 由于 message 有多种状态，这里需要额外处理
const dtMessage = {};

// 给 dtMessage 循环赋值不同类型的 message函数
['error', 'success', 'info', 'warning', 'warn'].forEach((type) => {
    dtMessage[type] = (content, duration, onClose) => {
        // 处理 message[type](config) 参数为对象类型
        if (typeof content === 'object') {
            return message[type]({ ...content, content: h => createElementVNode(h, content?.content) });
        }
        return message[type](h => createElementVNode(h, content), duration, onClose);
    };
});

export default dtMessage;

