package org.jeecg.modules.pin.utils;

import lombok.Getter;
import lombok.Setter;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.util.SysParameterUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class GpioControl {

    private static final String DEFAULT_GPIO_PATH = "/sys/class/gpio/";
    private static final String EXPORT_FILE = "export";
    private static final String DIRECTION_FILE = "direction";
    private static final String VALUE_FILE = "value";

    private final String gpioNumber;
    private final String direction;

    @Getter
    @Setter
    private String gpioPath;

    // 缓存初始化状态，避免重复初始化
    private boolean isInitialized = false;

    // 构造器，确保路径不会为空
    private GpioControl(String gpioNumber, String direction) {
        this.gpioNumber = gpioNumber;
        this.direction = direction;
        this.gpioPath = getGpioPathFromConfigOrDefault();
    }

    public static GpioControl getInstance(String gpioNumber, String direction) {
        return new GpioControl(gpioNumber, direction);
    }

    // 链式调用：初始化GPIO
    public GpioControl init() throws IOException {
        // 如果已经初始化过，则直接返回当前对象
        if (isInitialized) {
            return this;
        }
        // 判断文件是否存在
        String valuePath = getGpioPath() + "gpio" + gpioNumber + "/" + VALUE_FILE;
        if (Files.exists(Paths.get(valuePath))) {
            isInitialized = true; // 标记为已初始化
            return this; // 文件已存在，跳过初始化
        }
        // 导出GPIO pin
        writeToFile(getGpioPath() + EXPORT_FILE, gpioNumber);

        // 设置GPIO direction
        writeToFile(getGpioPath() + "gpio" + gpioNumber + "/" + DIRECTION_FILE, direction);

        isInitialized = true; // 标记为已初始化
        return this; // 返回当前对象，支持链式调用
    }

    // 链式调用：设置GPIO pin 值
    public GpioControl setValue(String value) throws IOException {
        // 如果没有初始化过，调用 init 方法
        if (!isInitialized) {
            init(); // 自动调用 init 进行初始化
        }
        writeToFile(getGpioPath() + "gpio" + gpioNumber + "/" + VALUE_FILE, value);
        return this; // 返回当前对象，支持链式调用
    }

    // 从配置中获取GPIO路径，若没有则使用默认路径
    private String getGpioPathFromConfigOrDefault() {
        SysParameter sysParameter = SysParameterUtil.getSysParameter("GPIO_PATH");
        if (sysParameter != null && sysParameter.getParamVal() != null) {
            return sysParameter.getParamVal();
        }
        return DEFAULT_GPIO_PATH;
    }

    // 写入文件的方法，使用 Files.write 来简化代码
    private void writeToFile(String filePath, String content) throws IOException {
        Files.write(Paths.get(filePath), content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // 静态方法，读取GPIO pin 值，路径示例：/sys/class/gpio/gpio412/value
    public static String readValue(String gpioNumber) throws IOException {
        String readStr = Files.readString(Paths.get(DEFAULT_GPIO_PATH + "gpio" + gpioNumber + "/" + VALUE_FILE));
        return readStr.trim();
    }
}
