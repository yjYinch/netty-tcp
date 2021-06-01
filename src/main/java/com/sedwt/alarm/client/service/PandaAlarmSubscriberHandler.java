package com.sedwt.alarm.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedwt.alarm.client.common.SyncAdvice;
import com.sedwt.alarm.client.entity.PandaAlarmBean;
import com.sedwt.alarm.client.entity.PandaMessageTypeEnum;
import com.sedwt.alarm.client.handler.PandaTaskHandler;
import com.sedwt.alarm.client.utils.Log;
import com.sedwt.alarm.client.utils.RedisUtil;
import com.sedwt.alarm.client.entity.AlarmSyncBean;
import com.sedwt.alarm.mapper.AlarmSyncMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.UUID;

/**
 * 主动上报的业务逻辑，TR069 --> redis channel --> {@link PandaAlarmSubscriberHandler#onMessage(String, String)}
 *
 * @author : yj zhang
 * @since : 2021/5/10 15:09
 */
@Service
public class PandaAlarmSubscriberHandler extends JedisPubSub {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PandaActiveReportService activeReportService;

    @Autowired
    private AlarmSyncMapper alarmSyncMapper;

    private static final Logger logger = LoggerFactory.getLogger("pandaAlarmSubscriberHandler");

    /**
     * 监听通道，获取channel 数据
     *
     * @param channelName tau_alarm_channel
     * @param message     PandaAlarmBean
     */
    @Override
    public void onMessage(String channelName, String message) {
        PandaAlarmBean pandaAlarmBean;
        try {
            pandaAlarmBean = objectMapper.readValue(message, PandaAlarmBean.class);
        } catch (Exception e) {
            logger.error("解析PandaAlarmBean失败，JSON转换异常，原因：{}", e.getMessage());
            return;
        }
        Integer messageType = pandaAlarmBean.getMessageType();
        if (messageType == null) {
            logger.error("主动上报的消息类型为空，请确认上报的消息类型");
            return;
        }

        try {
            // 0x08
            if (messageType == PandaMessageTypeEnum.PROXY_DEVICE_COMMUNICATE_INITIATIVE_UPLOAD.getVal()) {
                byte[] bytes = activeReportService.proxyDeviceCommunicationStatusByteArray(pandaAlarmBean);
                while (SyncAdvice.syncClientStatus) {
                    Thread.sleep(1000);
                    logger.info("当前系统正在进行同步设备状态，待同步完成之后，主动上报同步设备通信状态");
                }
                Log.printProxyDeviceCommActiveReport(bytes);
                activeReport(bytes, 0);
            } else if (messageType == PandaMessageTypeEnum.DEVICE_STATUS_UPLOAD.getVal()) { // 0x02 告警上报
                while (SyncAdvice.syncClientAlarm) {
                    Thread.sleep(1000);
                    logger.info("当前系统正在进行同步设备告警信息，待同步完成之后，主动上报设备告警信息");
                }
                if (!operationSyncTable(pandaAlarmBean)) {
                    // 恢复的告警在表中不存在的情况下不上报
                    return;
                }
                byte[] bytes = activeReportService.deviceAlarmStatusByteArray(pandaAlarmBean);
                Log.printDeviceStatusActiveReport(bytes);
                activeReport(bytes, 1);
            } else {
                logger.error("当前上报消息类型错误，messageType = {}", messageType);
            }
        } catch (Exception e) {
            logger.error("封装主动上报数据包异常，暂不发送数据给客户端, 原因：{}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        logger.info("已订阅channel:{}, 订阅频道数：{}", channel, subscribedChannels);
    }

    /**
     * 上报信息给服务端，并进行了重试策略
     *
     * @param readyReportBytes
     * @param flag
     */
    public static void activeReport(byte[] readyReportBytes, int flag) {
        ThreadLocal<Integer> retryLocal = new ThreadLocal<>();
        if (retryLocal.get() == null) {
            retryLocal.set(0);
        }
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        // 上报给服务端
        Map<String, Channel> channelMap = PandaTaskHandler.getChannelMap();
        for (Channel channel : channelMap.values()) {
            try {
                RedisUtil.set(uuid, 0, 5);
                channel.writeAndFlush(Unpooled.copiedBuffer(readyReportBytes));
                if (flag == 0) {
                    while (!SyncAdvice.syncClientStatusResponse) {
                        if (RedisUtil.get(uuid) == null) {
                            if (retryLocal.get() < 3) {
                                retryReport(channel, uuid, retryLocal, readyReportBytes, flag);
                            } else {
                                logger.info("重复上报消息已达最大次数3次，将不再上报信息");
                                break;
                            }
                        }
                    }
                } else {
                    while (!SyncAdvice.syncClientAlarmResponse) {
                        if (RedisUtil.get(uuid) == null) {
                            if (retryLocal.get() < 3) {
                                retryReport(channel, uuid, retryLocal, readyReportBytes, flag);
                            } else {
                                logger.info("重复上报消息已达最大次数3次，将不再上报信息");
                                break;
                            }
                        }
                    }
                }

            } catch (Exception e) {
                logger.error("主动上报消息失败");
            } finally {
                Integer beforeClear = retryLocal.get();
                retryLocal.remove();
                Integer afterClear = retryLocal.get();
                logger.info("清除threadLocal的值，清除前：{}, 清除后：{}", beforeClear, afterClear);
            }
        }
    }

    public static void retryReport(Channel channel, String uuid,
                                   ThreadLocal<Integer> retryLocal, byte[] bytes, int flag) {
        Integer retryCount = retryLocal.get();
        retryLocal.set(++retryCount);
        RedisUtil.set(uuid, retryCount, 5);
        logger.info("主动上报消息响应失败，即将重试，重试次数：{}", retryCount);
        channel.writeAndFlush(Unpooled.copiedBuffer(bytes));
        if (flag == 0) {
            Log.printProxyDeviceCommActiveReport(bytes);
        } else {
            Log.printDeviceStatusActiveReport(bytes);
        }
    }

    /**
     * sync同步表的操作
     *
     * @param pandaAlarmBean
     */
    public boolean operationSyncTable(PandaAlarmBean pandaAlarmBean) {
        AlarmSyncBean alarmSyncBean = new AlarmSyncBean();
        alarmSyncBean.setUnitId(pandaAlarmBean.getUnitId());
        alarmSyncBean.setAlarmNum(1);
        alarmSyncBean.setAlarmCode(pandaAlarmBean.getAlarmCode());
        alarmSyncBean.setAlarmName(pandaAlarmBean.getAlarmName());
        alarmSyncBean.setAlarmStatus(1);
        alarmSyncBean.setAlarmLevel(pandaAlarmBean.getAlarmLevel());
        alarmSyncBean.setRackNum(0);
        alarmSyncBean.setRackNum(0);
        alarmSyncBean.setRackPositionNum(0);
        alarmSyncBean.setSlotNum(0);
        alarmSyncBean.setPortNum(pandaAlarmBean.getPortNum());
        alarmSyncBean.setStationNum(0);
        alarmSyncBean.setPortNum(pandaAlarmBean.getPortNum());
        alarmSyncBean.setAlarmTime(pandaAlarmBean.getAlarmTime());
        alarmSyncBean.setEquipmentType("Panda");

        try {
            // 告警类型
            Integer alarmStatus = pandaAlarmBean.getAlarmStatus();
            AlarmSyncBean alarmSyncSingle = alarmSyncMapper.getAlarmSyncSingle(alarmSyncBean);

            // 判断告警类型：主动上报/恢复/忽略
            // 告警恢复: 0
            if (alarmStatus == 0) {
                if (alarmSyncSingle == null) {
                    // 不上报
                    logger.info("告警恢复内容{}不在alarm_sync表中，不做处理..", alarmSyncBean);
                    return false;
                } else {
                    // 清除同步表中的告警
                    alarmSyncMapper.deleteAlarmSync(alarmSyncBean);
                    logger.info("告警恢复已确认，清除该条该告警{}", alarmSyncBean);
                }
            } else { // 告警上报
                if (alarmSyncSingle == null) {
                    alarmSyncMapper.insertAlarmSync(alarmSyncBean);
                    logger.info("新增告警信息到sync_alarm表中");
                } else {
                    alarmSyncMapper.updateAlarmSync(alarmSyncBean);
                    logger.info("告警信息已存在sync_alarm表中，更新sync_alarm表的告警时间");
                }
            }
        } catch (Exception e) {
            logger.error("[Panda] 主动上报操作数据库异常，原因：{}", e.getMessage());
            return false;
        }
        return true;
    }

}
