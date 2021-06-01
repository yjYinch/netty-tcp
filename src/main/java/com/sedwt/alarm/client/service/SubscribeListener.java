package com.sedwt.alarm.client.service;


import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

/**
 * @author : zhang yijun
 * @date : 2021/1/28 10:05
 * @description : TODO
 */

@Slf4j
public class SubscribeListener<T extends JedisPubSub> implements Runnable{

    /**
     * jedisPool
     */
    private final JedisPool jedisPool;

    /**
     * 订阅对象
     */
    private final T subscribe;
    /**
     * 订阅频道名
     */
    private final String channelName;

    /**
     * 构造SubscribeListener对象
     * @param jedisPool
     */
    public SubscribeListener(JedisPool jedisPool, T subscribe, String channelName){
        this.jedisPool = jedisPool;
        this.subscribe = subscribe;
        this.channelName = channelName;
    }

    /**
     * 启动订阅者
     */
    @Override
    public void run() {
        Jedis jedis = null;
        try {
            // 从redis连接池获取一个redis连接
            jedis = jedisPool.getResource();
            log.info("从jedis池中获取jedis客户端:[{}], 并准备订阅通道channel:[{}]", jedis, channelName);
            // 通过subscribe 的api去订阅，入参是订阅对象和频道名
            jedis.subscribe(subscribe, channelName);
            log.info("订阅关闭");
        } catch (Exception e) {
            log.error("订阅线程redis的alarm_channel频道failed");
        } finally {
            if (jedis != null){
                jedis.close();
            }
        }
    }
}
