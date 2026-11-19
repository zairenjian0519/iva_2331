/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.serial;

import gnu.io.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.serial.utils.SerialUtil;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TooManyListenersException;

/**
 * @author： DecoderX
 * @date： 2025/10/22
 */
@Slf4j
@Component
public class Serial485Service {

    @Getter
    @Setter
    private SerialPort serialPort;

    /**
     * 获取设备上连接的485传感器
     */
    public List<String> listAvailableSerialPorts() {
        List<String> ports = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {
            CommPortIdentifier portId = portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                ports.add(portId.getName());
            }
        }

        if (ports.isEmpty()) {
            log.warn("未找到串行端口！");
        }

        return ports;
    }

    /**
     * 打开串口
     */
    public void openCommPort(Serial485Setting serial485Setting)
            throws NoSuchPortException, UnsupportedCommOperationException, PortInUseException, TooManyListenersException {

        String commPortName = serial485Setting.getCommPortName();

        // 通过端口名识别端口
        CommPortIdentifier identifier = CommPortIdentifier.getPortIdentifier(commPortName);
        if (identifier.isCurrentlyOwned()) {
            log.error("{} 串口正在使用, 当前owner：{}", commPortName, identifier.getCurrentOwner());
            return;
        }

        // 打开端口，并给端口名字和一个timeout（打开操作的超时时间）
        SerialPort commPort = identifier.open(commPortName, 2000);

        // 判断是不是串口
        if (commPort != null) {
            // 设置监听器生效 当有数据时通知
            commPort.setDTR(true);
            commPort.setRTS(true);

            // 设置一下串口的波特率等参数
            commPort.setSerialPortParams(
                    serial485Setting.getBaudRate(),
                    serial485Setting.getDataBits(),
                    serial485Setting.getStopBits(),
                    serial485Setting.getParity());

            log.info("打开串口{}成功", commPortName);
            setSerialPort(commPort);
            serialPort.addEventListener(new CommPortListener(getSerialPort()));
            serialPort.notifyOnDataAvailable(true);
        } else {
            log.info("{}不是串口", commPortName);
        }
    }

    /**
     * 添加监听器
     */
    public void addCommPortListener(SerialPortEventListener serialPortEventListener) throws TooManyListenersException {
        serialPort.addEventListener(serialPortEventListener);
    }

    /**
     * 关闭串口
     */
    public void closeCommPort() {
        if (serialPort != null) {
            serialPort.removeEventListener();
            serialPort.close();
            log.warn("串口{}关闭", serialPort.getName());
            setSerialPort(null);
        }
    }

    /**
     * 发送指令，参数格式为：01 03 00 00 00 02 C4 0B
     * 需要转换
     */
    public void sendCommand(String hexCommand) throws IOException {
        if (serialPort != null) {
            byte[] command = SerialUtil.hexToByte(hexCommand);
            OutputStream outputStream = serialPort.getOutputStream();
            outputStream.write(command);
            outputStream.flush(); // 仅刷新，不关闭
            log.info("串口 {} 发送指令 {} 成功", serialPort.getName(), hexCommand);
        }
    }
}
