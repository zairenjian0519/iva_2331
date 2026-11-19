/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.serial;

import com.alibaba.fastjson.JSONObject;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.serial.config.SerialPortUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * @author： DecoderX
 * @date： 2025/10/23
 */
@Slf4j
public class CommPortListener implements SerialPortEventListener {

    private final ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();

    private final SerialPort serialPort;

    public CommPortListener(SerialPort serialPort) {
        this.serialPort = serialPort;
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
                log.info("串口 {} 捕获到 DATA_AVAILABLE 事件", serialPort.getName());
                handleDataAvailableEvent();
                break;
            case SerialPortEvent.HARDWARE_ERROR:
                log.error("硬件错误，串口 {} 断开连接", serialPort.getName());
                handleHardwareErrorEvent();
                break;
            default:
                break;
        }
    }

    private void handleHardwareErrorEvent() {
        serialPort.close(); // 关闭串口

        // 发送事件
        JSONObject eventData = new JSONObject();
        eventData.put("type", "HardwareErrorEvent");
        CommEventPublisher.publishCommEvent(eventData);

    }

    private void handleDataAvailableEvent() {
        try {
            InputStream inputStream = serialPort.getInputStream();
            int available = inputStream.available();
            if (available < 0) return;

            byte[] buffer = new byte[available];
            int len = inputStream.read(buffer);
            if (len <= 0) return;

            frameBuffer.write(buffer, 0, len);
            String hex = byteTo16String(frameBuffer.toByteArray());
            log.debug("串口 {} 累积数据: {}", SerialPortUtil.formatPortName(serialPort.getName()), hex);

            // 检查是否达到完整帧
            byte[] data = frameBuffer.toByteArray();
            if (isCompleteModbusFrame(data)) {
                log.info("串口 {} 完整帧数据: {}", SerialPortUtil.formatPortName(serialPort.getName()), hex);
                if (checkCRC(data)) { // 校验 CRC16
                    double[] resArr = parseResponse(data);
                    JSONObject eventData = new JSONObject();
                    eventData.put("type", "DataAvailableEvent");
                    eventData.put("data", resArr);
                    CommEventPublisher.publishCommEvent(eventData);
                } else {
                    log.warn("串口 {} CRC 校验失败: {}", serialPort.getName(), hex);
                }

                // 清空缓冲区，为下一帧做准备
                frameBuffer.reset();
            }
        } catch (IOException e) {
            log.error("串口 {} 读取数据错误", serialPort.getName(), e);
        }
    }

    /**
     * 解析应答数据
     */
    private double[] parseResponse(byte[] resp) {
        if (resp.length < 9) {
            log.error("应答长度错误！");
            return new double[0];
        }

        byte slave = resp[0];
        byte func = resp[1];

        if (func != 0x03) {
            log.error("功能码错误：" + func);
            return new double[0];
        }

        // 解析温度、湿度（假设每个寄存器2字节，值除以100）
        int tempRaw = ((resp[3] & 0xFF) << 8) | (resp[4] & 0xFF);
        int humiRaw = ((resp[5] & 0xFF) << 8) | (resp[6] & 0xFF);
        double temp = tempRaw / 100.0;
        double humi = humiRaw / 100.0;

        System.out.printf("从机地址: %d%n", slave);
        System.out.printf("温度: %.2f ℃%n", temp);
        System.out.printf("湿度: %.2f %%RH%n", humi);

        return new double[]{slave, temp, humi};
    }

    // 判断是否是完整的 Modbus RTU 帧
    private boolean isCompleteModbusFrame(byte[] data) {
        // Modbus RTU 最小帧长 = 5（地址 + 功能码 + CRC）
        if (data.length < 5) return false;

        int functionCode = data[1] & 0xFF;

        // 读保持寄存器 (03) 响应：第3个字节为“数据字节数”
        if (functionCode == 3 || functionCode == 4) {
            int byteCount = data[2] & 0xFF;
            int expectedLength = 3 + byteCount + 2; // 地址+功能码+字节数+数据+CRC
            return data.length >= expectedLength;
        }

        // 其他功能码（如写寄存器）固定 8 字节
        return data.length >= 8;
    }

    private int[] getCRC(byte[] bytes) {
        int crc = 0xFFFF;
        for (byte b : bytes) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc >>= 1;
                    crc ^= 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        int low = crc & 0xFF;
        int high = (crc >> 8) & 0xFF;
        return new int[]{low, high}; // Modbus CRC是低位在前
    }

    private boolean checkCRC(byte[] response) {
        if (response.length < 3) return false;
        int len = response.length;
        int[] crc = getCRC(Arrays.copyOf(response, len - 2));
        return (crc[0] == (response[len - 2] & 0xFF)) && (crc[1] == (response[len - 1] & 0xFF));
    }

    private String byteTo16String(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
