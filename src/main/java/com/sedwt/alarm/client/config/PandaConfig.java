package com.sedwt.alarm.client.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author : yj zhang
 * @since : 2021/5/10 10:52
 */
@Component
@ConfigurationProperties(prefix = "panda")
@Setter
@Getter
public class PandaConfig {

    /**
     * 心跳间隔
     */
    private Integer heartbeatPeriod;

    /**
     * 服务端主机名
     */
    private String hostname;

    /**
     * 服务端端口
     */
    private int port;

    /**
     * 重连服务端间隔时间
     */
    private int retryConnectInterval;
}
