package org.jeecg.config.queue;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 延迟队列
 */
public class MyDelayed<T> implements Delayed {
    // 延迟时间 毫秒
    private long delayTime;
    // 过期时间
    private long expireTime;

    // 数据payload
    private T data;

    public MyDelayed(long delayTime, T t) {
        this.delayTime = delayTime;
        // 过期时间 = 当前时间 + 延迟时间
        this.expireTime = System.currentTimeMillis() + delayTime;
        data = t;
    }

    /**
     * 剩余时间 = 到期时间 - 当前时间
     */
    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return unit.convert(this.expireTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 优先级规则：两个任务比较，时间短的优先执行
     */
    @Override
    public int compareTo(@NotNull Delayed o) {
        long f = this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
        return (int) f;
    }

    @Override
    public String toString() {
        return "delayTime=" + delayTime +
                ", expire=" + expireTime +
                ", data=" + data;
    }

    public T getData(){
        return this.data;
    }
}
