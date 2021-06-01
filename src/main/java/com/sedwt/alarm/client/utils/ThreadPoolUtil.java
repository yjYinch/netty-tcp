package com.sedwt.alarm.client.utils;

import com.sedwt.alarm.client.config.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : yj zhang
 * @since : 2021/5/8 9:16
 */

public class ThreadPoolUtil {
    private static final Logger logger = LoggerFactory.getLogger("threadPoolUtil");

    private volatile static ExecutorService executorService = null;

    /**
     * 核心线程数
     */
    private static final Integer CORE_SIZE;

    /**
     * 最大线程数
     */
    private static final Integer MAX_SIZE;

    /**
     * 线程存活时间
     */
    private static final Integer MAX_ALIVE_TIME;

    /**
     * 队列容量
     */
    private static final Integer QUEUE_CAPACITY;

    static {
        Properties properties = BeanContext.getBean(Properties.class);
        // 没有配置参数则按照CPU的核数*2
        CORE_SIZE = properties.getCoreSize() == null ?
                Runtime.getRuntime().availableProcessors() * 2 : properties.getCoreSize();
        MAX_SIZE = properties.getMaxSize() == null ?
                Runtime.getRuntime().availableProcessors() * 2 : properties.getMaxSize();
        MAX_ALIVE_TIME = properties.getMaxAliveTime() == null ? 30 : properties.getMaxAliveTime();
        QUEUE_CAPACITY = properties.getMaxQueueCapacity() == null ? MAX_SIZE << 1 : properties.getMaxQueueCapacity();
        logger.info("线程池参数初始化完成，CORE_SIZE={}, MAX_SIZE={}, MAX_ALIVE_TIME={}, QUEUE_CAPACITY={} ",
                CORE_SIZE, MAX_SIZE, MAX_ALIVE_TIME, QUEUE_CAPACITY);
    }

    private ThreadPoolUtil() {
    }

    public static ExecutorService getExecutorService(){
        if (executorService == null){
            synchronized (ThreadPoolUtil.class){
                if (executorService == null){
                    executorService = new ThreadPoolExecutor(
                            CORE_SIZE,
                            MAX_SIZE,
                            MAX_ALIVE_TIME,
                            TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                            new CustomThreadNameFactory("alarm-pool"),
                            new ThreadPoolExecutor.AbortPolicy());
                    logger.info("executorService已创建");
                }
            }
        }
        return executorService;
    }

    /**
     * 自定义线程工厂
     */
    private static class CustomThreadNameFactory implements ThreadFactory{
        /**
         * executorService的数量编号
         */
        private final AtomicInteger poolNumber = new AtomicInteger(1);

        /**
         * 线程的数量编号
         */
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private final ThreadGroup threadGroup;

        public final String namePrefix;
        public CustomThreadNameFactory(String threadName){
            SecurityManager s = System.getSecurityManager();
            threadGroup = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
            if (threadName == null || "".equals(threadName.trim())){
                threadName = "alarm-pool";
            }
            namePrefix = threadName +"-" + poolNumber.getAndIncrement() +"-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(threadGroup, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()){
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY){
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
