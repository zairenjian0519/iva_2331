package org.jeecg.modules.serial.threads;

import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.mqtt.pojo.SendAlarmSystem;

import java.util.concurrent.LinkedBlockingQueue;

public class SingleThreadQueue extends Thread {

    // 使用volatile关键字确保instance的可见性
    private static volatile SingleThreadQueue instance;

    private final LinkedBlockingQueue<TaskExecutor> taskQueue;

    private final SendAlarmSystem sendAlarmSystem;

    // 私有构造函数，防止外部实例化
    private SingleThreadQueue() {
        taskQueue = new LinkedBlockingQueue<>(5000);
        sendAlarmSystem = SpringContextUtils.getBean(SendAlarmSystem.class);
        // 启动线程
        this.start();
    }

    // 双重检查锁定实现单例模式
    public static SingleThreadQueue getInstance() {
        if (instance == null) {
            synchronized (SingleThreadQueue.class) {
                if (instance == null) {
                    instance = new SingleThreadQueue();
                }
            }
        }
        return instance;
    }

    // 添加任务到任务队列
    public void addTask(TaskExecutor task) {
        taskQueue.add(task);
    }

    @Override
    public void run() {
        try {
            while (true) {
                TaskExecutor task = taskQueue.take();
                task.start();
                Thread.sleep(sendAlarmSystem.getVoiceSegment());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
