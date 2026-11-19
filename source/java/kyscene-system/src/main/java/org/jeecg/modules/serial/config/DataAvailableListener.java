package org.jeecg.modules.serial.config;


import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.devwebsocket.SerialWebSocket;
import org.jeecg.modules.serial.service.SerialPortService;
import org.jeecg.modules.serial.utils.Crc16Modbus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * 串口事件监听
 */
public class DataAvailableListener implements SerialPortEventListener {

    private static final Logger log = LoggerFactory.getLogger(DataAvailableListener.class);

    private final SerialPort serialPort;

    private final SerialPortService serialPortService;

    public DataAvailableListener(SerialPort serialPort) {
        this.serialPort = serialPort;
        serialPortService = SpringContextUtils.getBean(SerialPortService.class);
    }

    /**
     * SerialPort关于事件的静态成员变量
     * BI Break interrupt中断
     * OE Overrun error错误
     * FE Framing error错误
     * PE Parity error奇偶检验错误
     * CD Carrier detect载波侦听
     * CTS Clear to send清除以传送
     * DSR Data set ready数据备妥
     * RI Ring indicator响铃侦测
     * OUTPUT_BUFFER_EMPTY 输出缓冲区空
     * DATA_AVAILABLE 串口中的可用数据
     */
    @Override
    public void serialEvent(SerialPortEvent event) {
        switch (event.getEventType()) {
            case SerialPortEvent.BI:
                log.error("串口 {} 数据中断，准备关闭串口", serialPort.getName());
                // serialPort.close();
                break;
            case SerialPortEvent.OE:
                log.warn("串口 {} 溢出错误", serialPort.getName());
                break;
            case SerialPortEvent.FE:
                log.warn("串口 {} 帧错误", serialPort.getName());
                break;
            case SerialPortEvent.PE:
                log.warn("串口 {} 奇偶校验错误", serialPort.getName());
                break;
            case SerialPortEvent.CD:
            case SerialPortEvent.CTS:
            case SerialPortEvent.DSR:
            case SerialPortEvent.RI:
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                break;
            case SerialPortEvent.DATA_AVAILABLE:
                byte[] readBuffer = SerialPortUtil.readData(serialPort);
                String hex = Crc16Modbus.byteTo16String(readBuffer);
                log.info("串口 {} 返回的数据: {}", SerialPortUtil.formatPortName(serialPort.getName()), hex);
                // 更新串口状态: 在线
                serialPortService.updateBySerialStatus(SerialPortUtil.formatPortName(serialPort.getName()), 1);
                // 发送websocket通知
                JSONObject message = new JSONObject();
                message.put("serialName", SerialPortUtil.formatPortName(serialPort.getName()));
                message.put("data", hex);
                message.put("time", new Date());
                SerialWebSocket serialWebSocket = SpringContextUtils.getBean(SerialWebSocket.class);
                if (ObjectUtil.isNotEmpty(serialWebSocket)) serialWebSocket.sendMessageToUser(message.toJSONString());
                RedisUtil redisUtil = SpringContextUtils.getBean(RedisUtil.class);
                redisUtil.del(SerialPortUtil.SERIAL_VOICE_OPEN_PREFIX + SerialPortUtil.formatPortName(serialPort.getName()));
                break;
            case SerialPortEvent.HARDWARE_ERROR:
                log.error("硬件错误，串口 {} 断开连接", serialPort.getName());
                // 内存里面删除该串口信息
                SerialPortUtil.SERIAL_PORT_LIST.removeIf(map -> {
                    String portName = map.get("Name").toString();
                    return serialPort.getName().contains(portName);
                });
                // 关闭串口
                serialPort.close();
                // 设置离线
                serialPortService.updateBySerialStatus(SerialPortUtil.formatPortName(serialPort.getName()), 0);
                break;
            default:
                break;
        }
    }
}
