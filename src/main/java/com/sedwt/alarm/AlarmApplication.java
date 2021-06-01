package com.sedwt.alarm;

import com.sedwt.alarm.client.PandaTcpClient;
import com.sedwt.alarm.client.config.PandaConfig;
import com.sedwt.alarm.client.service.PandaAlarmSubscriberHandler;
import com.sedwt.alarm.client.utils.ThreadPoolUtil;
import com.sedwt.alarm.client.service.SubscribeListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import redis.clients.jedis.JedisPool;


/**
 * @author yj zhang
 * @since 2020.12
 */
@SpringBootApplication
public class AlarmApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ac = SpringApplication.run(AlarmApplication.class, args);
        // 配置类
        PandaConfig pc = ac.getBean(PandaConfig.class);
        PandaAlarmSubscriberHandler pandaAlarmSubscriberHandler = ac.getBean(PandaAlarmSubscriberHandler.class);

        JedisPool jedisPool0 = ac.getBean("jedisPool0", JedisPool.class);

        // =============================================redis 通道监听启动===============================================
        // panda_alarm_channel 通道监听
        ThreadPoolUtil.getExecutorService()
                .execute(new SubscribeListener<>(jedisPool0, pandaAlarmSubscriberHandler, "panda_alarm_channel"));

        // =============================================Panda tcp 客户端启动=============================================
        // panda tcp 客户端启动
        ThreadPoolUtil.getExecutorService().execute(new PandaTcpClient(pc.getHostname(),
                pc.getPort(), pc.getHeartbeatPeriod(), pc.getRetryConnectInterval()));
    }
}
