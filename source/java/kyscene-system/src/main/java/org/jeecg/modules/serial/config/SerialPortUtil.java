package org.jeecg.modules.serial.config;


import cn.hutool.core.util.ObjectUtil;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEventListener;
import org.jeecg.modules.serial.entity.SoundPillarParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 串口通信
 */
public class SerialPortUtil {

    private static Logger log = LoggerFactory.getLogger(SerialPortUtil.class);

    private static final int DELAY_TIME = 2000;

    // 波特率
    private static final int BAUD_RATE = 9600;

    // 数据位
    private static final int DATA_BITS = SerialPort.DATABITS_8;

    // 停止位
    private static final int STOP_BITS = SerialPort.STOPBITS_1;

    // 奇偶校验位
    private static final int PARITY = SerialPort.PARITY_NONE;

    public static final List<Map<String, Object>> SERIAL_PORT_LIST = new ArrayList<>();

    // 音柱命令缓存key
    public static final String SERIAL_VOICE_CODE_LIST = "serial_voice_code_list";

    // 音柱openKey，过期监听的key
    public static final String SERIAL_VOICE_OPEN_PREFIX = "voice_open_key_";

    public static final String SERIAL_VOICE_ALGORITHM = "serial_voice_algorithm";

    public static final ConcurrentLinkedQueue<SoundPillarParam> VOICE_PLAY_QUEUE = new ConcurrentLinkedQueue<>();

    /**
     * 打开串口
     */
    public static SerialPort openPort(String portName) {
        SerialPort serialPort = null;
        try {
            // 通过端口名识别端口
            CommPortIdentifier identifier = CommPortIdentifier.getPortIdentifier(portName);
            // 打开端口，并给端口名字和一个timeout（打开操作的超时时间）
            CommPort commPort = identifier.open(portName, DELAY_TIME);
            // 判断是不是串口
            if (commPort instanceof SerialPort) {
                serialPort = (SerialPort) commPort;
                // 设置监听器生效 当有数据时通知
                serialPort.setDTR(true);
                serialPort.setRTS(true);
                // 设置一下串口的波特率等参数
                serialPort.setSerialPortParams(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY);
                log.info("打开串口{}成功", portName);
            } else {
                log.info("{}不是串口", portName);
            }
        } catch (Exception e) {
            log.error("打开串口{}错误", portName, e);
        }
        return serialPort;
    }

    /**
     * 打开串口
     * 带其他参数
     */
    public static SerialPort openPort(String portName, Integer baudRate, Integer dataBits, Integer stopBits, Integer parity) {
        SerialPort serialPort = null;
        try {
            // 通过端口名识别端口
            CommPortIdentifier identifier = CommPortIdentifier.getPortIdentifier(portName);
            if (identifier.isCurrentlyOwned()) {
                log.error("{} 串口正在使用, 当前owner：{}", portName, identifier.getCurrentOwner());
                return null;
            }
            // 打开端口，并给端口名字和一个timeout（打开操作的超时时间）
            CommPort commPort = identifier.open(portName, DELAY_TIME);
            // 判断是不是串口
            if (commPort instanceof SerialPort) {
                serialPort = (SerialPort) commPort;
                // 设置监听器生效 当有数据时通知
                serialPort.setDTR(true);
                serialPort.setRTS(true);
                // 设置一下串口的波特率等参数
                serialPort.setSerialPortParams(baudRate, dataBits, stopBits, parity);
                log.info("打开串口{}成功", portName);
            } else {
                log.info("{}不是串口", portName);
            }
        } catch (Exception e) {
            log.error("打开串口{}错误", portName, e);
        }
        return serialPort;
    }

    /**
     * 串口增加数据监听器
     */
    public static void addListener(SerialPort serialPort, SerialPortEventListener listener) throws TooManyListenersException {
        if (serialPort == null) return;
        // 给串口添加监听器
        serialPort.addEventListener(listener);
        // 设置当有数据到达时唤醒监听接收线程
        serialPort.notifyOnDataAvailable(Boolean.TRUE);
        // 设置当通信中断时唤醒中断线程
        serialPort.notifyOnBreakInterrupt(Boolean.TRUE);
    }

    /**
     * 关闭串口
     */
    public static void closePort(SerialPort serialPort) {
        if (serialPort != null) {
            serialPort.close();
            log.warn("串口{}关闭", serialPort.getName());
        }
    }

    /**
     * 发送数据
     */
    public static void sendData(SerialPort serialPort, byte[] data) {
        if (serialPort == null) return;
        OutputStream os = null;
        try {
            //获得串口的输出流
            os = serialPort.getOutputStream();
            os.write(data);
            os.flush();
        } catch (IOException e) {
            log.error("串口{}发送数据错误", serialPort.getName(), e);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                log.error("串口 {} OutputStream 关闭失败", serialPort.getName(), e);
            }
        }
    }

    /**
     * 从串口读取数据
     */
    public static byte[] readData(SerialPort serialPort) {
        InputStream is = null;
        byte[] bytes = null;
        try {
            //获得串口的输入流
            is = serialPort.getInputStream();
            //获得数据长度
            int bufflenth = is.available();
            while (bufflenth != 0) {
                //初始化byte数组
                bytes = new byte[bufflenth];
                is.read(bytes);
                bufflenth = is.available();
            }
        } catch (IOException e) {
            log.error("串口{}读取数据失败", serialPort.getName(), e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                log.error("串口 {} InputStream 关闭失败", serialPort.getName(), e);
            }
        }
        return bytes;
    }

    /**
     * 查询可用端口
     */
    public static List<String> getSerialPortList() {
        // 删除全部缓存的串口
        if (ObjectUtil.isNotEmpty(SERIAL_PORT_LIST)) {
            // 使用迭代器逐个删除元素
            Iterator<Map<String, Object>> iterator = SERIAL_PORT_LIST.iterator();
            while (iterator.hasNext()) {
                // 先获取当前元素
                Map<String, Object> currentElement = iterator.next();
                SerialPort serialPort = (SerialPort) currentElement.get("SerialPort");
                serialPort.close();
                // 删除当前元素
                iterator.remove();
            }
        }
        List<String> systemPorts = new ArrayList<>();
        // 获得当前所有可用端口
        Enumeration<CommPortIdentifier> serialPorts = CommPortIdentifier.getPortIdentifiers();
        // 将可用端口名添加到List并返回该List
        while (serialPorts.hasMoreElements()) {
            systemPorts.add(serialPorts.nextElement().getName());
        }
        return systemPorts;
    }

    /**
     * 格式化串口名称
     */
    public static String formatPortName(String portName) {
        //log.info("需要格式化的串口名称：{}", portName);
        // 正则表达式匹配主要部分
        String regex = "(COM\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(portName);
        if (matcher.find()) {
            // 提取匹配的部分
            return matcher.group(1);
        }
        return portName;
    }

}


