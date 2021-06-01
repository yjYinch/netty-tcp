package com.sedwt.alarm.client.handler;

import com.sedwt.alarm.client.PandaTcpClient;
import com.sedwt.alarm.client.common.SyncAdvice;
import com.sedwt.alarm.client.config.PandaConfig;
import com.sedwt.alarm.client.entity.PandaMessageTypeEnum;
import com.sedwt.alarm.client.entity.PandaResponse;
import com.sedwt.alarm.client.entity.PandaResponseResultEnum;
import com.sedwt.alarm.client.entity.PandaResultEnum;
import com.sedwt.alarm.client.entity.PandaSyncTypeEnum;
import com.sedwt.alarm.client.service.AlarmSyncService;
import com.sedwt.alarm.client.service.CommSyncService;
import com.sedwt.alarm.client.service.PandaHeartbeatService;
import com.sedwt.alarm.client.utils.ByteUtil;
import com.sedwt.alarm.client.utils.BeanContext;
import com.sedwt.alarm.client.utils.ThreadPoolUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.sedwt.alarm.client.common.SyncAdvice.syncClientAlarmResponse;
import static com.sedwt.alarm.client.common.SyncAdvice.syncClientStatusResponse;

/**
 * @author : yj zhang
 * @since : 2021/5/8 13:56
 */

public class PandaTaskHandler extends ChannelInboundHandlerAdapter {
    private static final PandaConfig PROPERTIES_CONFIG;
    private static final String HOSTNAME;
    private static final int PORT;
    private static final int WRITE_IDLE_TIME_PERIOD;
    private static final int RETRY_CONNECT_INTERVAL;

    static {
        PROPERTIES_CONFIG = BeanContext.getBean(PandaConfig.class);
        HOSTNAME = PROPERTIES_CONFIG.getHostname();
        PORT = PROPERTIES_CONFIG.getPort();
        WRITE_IDLE_TIME_PERIOD = PROPERTIES_CONFIG.getHeartbeatPeriod();
        RETRY_CONNECT_INTERVAL = PROPERTIES_CONFIG.getRetryConnectInterval();
    }

    private static final Map<String, Channel> CHANNEL_MAP = new HashMap<>(1);

    public static Map<String, Channel> getChannelMap() {
        return CHANNEL_MAP;
    }

    private static final Logger logger = LoggerFactory.getLogger("pandaTaskHandler");
    private static final PandaHeartbeatService PANDA_HEARTBEAT_SERVICE;

    private static final AlarmSyncService alarmSyncService;
    private static final CommSyncService commSyncService;

    static {
        PANDA_HEARTBEAT_SERVICE = BeanContext.getBean(PandaHeartbeatService.class);
        alarmSyncService = BeanContext.getBean(AlarmSyncService.class);
        commSyncService = BeanContext.getBean(CommSyncService.class);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        logger.info("channel:[{}] registered", channel);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        logger.info("channel:[{}] unregistered", channel);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        CHANNEL_MAP.put("Panda", channel);
        logger.info("channel通道:[{}] 已激活，channel_map : {}", channel, CHANNEL_MAP);

        SyncAdvice.isActive = true;
        //(1)同步设备通信状态
        ThreadPoolUtil.getExecutorService().execute(() -> commSyncService.sendSync(ctx));
        //(2)同步设备告警状态
        ThreadPoolUtil.getExecutorService().execute(() -> alarmSyncService.sendSync(ctx, null));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SyncAdvice.isActive = false;
        logger.info("channel inactive, 清除CHANNEL_MAP：{}", CHANNEL_MAP);
        CHANNEL_MAP.clear();
        logger.info("清除CHANNEL_MAP后：{}", CHANNEL_MAP);
        ThreadPoolUtil.getExecutorService().execute(
                new PandaTcpClient(HOSTNAME, PORT, WRITE_IDLE_TIME_PERIOD, RETRY_CONNECT_INTERVAL));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //解码过后，处理业务
        PandaResponse messageType = (PandaResponse) msg;
        if (messageType == null) {
            logger.error("解析消息响应类型为null,请确认解码逻辑");
            return;
        }
        String type = messageType.getType();

        // 1. 同步设备通信状态确认 2. 同步设备状态信息确认 3. 心跳确认，确认之后继续发送心跳
        if (PandaMessageTypeEnum.HEART_BEAT.name().equals(type)) {
            // 定时一段时间后发送心跳
            PANDA_HEARTBEAT_SERVICE.sendHeartbeat(ctx);
        } else if (PandaMessageTypeEnum.DEVICE_STATUS_UPLOAD.name().equals(type)) {
            // 设备状态主动上传响应确认
            String result = messageType.getResult();
            if (PandaResponseResultEnum.SUCCESS.name().equals(result)) {
                logger.info("设备状态主动上传响应成功");
                syncClientAlarmResponse = true;
            } else {
                logger.error("设备状态主动上传响应失败");
                syncClientAlarmResponse = false;
            }
        } else if (PandaMessageTypeEnum.CLIENT_STATUS_SYNC_COMMOND.name().equals(type)) {
            // 设备状态同步上传命令响应
            if (PandaResponseResultEnum.SUCCESS.name().equals(messageType.getResult())) {
                // (1)发送响应内容
                sendResponse(ctx, PandaMessageTypeEnum.CLIENT_STATUS_SYNC_COMMOND.getVal(), PandaResultEnum.SUCCESS.getVal());
                // (2)设备告警状态同步上传
                String syncType = messageType.getSyncType();
                if (PandaSyncTypeEnum.SINGLE.name().equals(syncType)) {
                    Integer unitId = messageType.getUnitId();
                    alarmSyncService.sendSync(ctx, unitId);
                } else {
                    alarmSyncService.sendSync(ctx, null);
                }
            } else {
                sendResponse(ctx, PandaMessageTypeEnum.CLIENT_STATUS_SYNC_COMMOND.getVal(),
                        PandaResultEnum.INVALID_NODE_CODE.getVal());
            }
        } else if (PandaMessageTypeEnum.PROXY_DEVICE_COMMUNICATE_INITIATIVE_UPLOAD.name().equals(type)) {
            // 代理设备通信状态主动上传响应确认
            String result = messageType.getResult();
            if (PandaResponseResultEnum.SUCCESS.name().equals(result)) {
                logger.info("设备通信状态主动上传响应成功");
                syncClientStatusResponse = true;
            } else {
                logger.error("设备通信状态主动上传响应失败");
                syncClientStatusResponse = false;
            }
        } else if (PandaMessageTypeEnum.PROXY_DEVICE_COMMUNICATE_SYNC_UPLOAD.name().equals(type)) {
            // 代理设备通信状态同步上传响应确认
            String result = messageType.getResult();
            if (PandaResponseResultEnum.SUCCESS.name().equals(result)) {
                logger.info("同步设备通信状态成功");
            } else {
                logger.error("同步设备通信状态失败");
            }
            CommSyncService.isSuccessInform = true;
        } else if (PandaMessageTypeEnum.PROXY_DEVICE_STATUS_SYNC_UPLOAD.name().equals(type)) {
            // 代理设备状态同步上传响应确认
            String result = messageType.getResult();
            if (PandaResponseResultEnum.SUCCESS.name().equals(result)) {
                logger.info("同步设备告警状态成功");
            } else {
                logger.error("同步设备告警状态失败");
            }
            AlarmSyncService.isSuccessInform = true;
        } else if ("unknown".equals(type)) {
            logger.error("未知的响应消息类型，请确认");
        } else {
            logger.error("未知的响应消息类型，请确认");
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        logger.info("channel read complete");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("出现异常...异常原因：{}", cause.getMessage());

    }

    private void sendResponse(ChannelHandlerContext ctx, byte messageType, byte result) {
        Channel channel = ctx.channel();
        try {
            byte[] bytes = ByteUtil.getResponseBytes(messageType, result);
            channel.writeAndFlush(Unpooled.copiedBuffer(bytes));
            logger.info("send [response] message to server, message type [{}], message content: {}",
                    PandaMessageTypeEnum.CLIENT_STATUS_SYNC_COMMOND.name(), ByteUtil.printBytesToConsole(bytes));
        } catch (Exception e) {
            logger.error("send [response] message to server error! {}", e.getMessage());
        }
    }

}
