package com.sedwt.alarm.client.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author : zhang yijun
 * @date : 2021/1/20 9:36
 * @description : TODO
 */

@Data
@Component
public class Properties {

    /**
     * 线程池配置参数
     */
    @Value("${thread.pool.core-size}")
    private Integer coreSize;

    @Value("${thread.pool.max-size}")
    private Integer maxSize;

    @Value("${thread.pool.max-queue-capacity}")
    private Integer maxQueueCapacity;

    @Value("${thread.pool.max-alive-time}")
    private Integer maxAliveTime;



}
