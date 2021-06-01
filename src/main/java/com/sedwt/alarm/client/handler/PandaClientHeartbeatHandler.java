package com.sedwt.alarm.client.handler;

import com.sedwt.alarm.client.PandaTcpClient;
import com.sedwt.alarm.client.config.PandaConfig;
import com.sedwt.alarm.client.service.PandaHeartbeatService;
import com.sedwt.alarm.client.utils.Log;
import com.sedwt.alarm.client.utils.BeanContext;
import com.sedwt.alarm.client.utils.ThreadPoolUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端心跳处理器
 *
 * @author : yj zhang
 * @since : 2021/5/8 13:58
 */
public class PandaClientHeartbeatHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger("clientHeartbeatHandler");
    private static final PandaConfig PROPERTIES_CONFIG;
    private static final String HOSTNAME;
    private static final int PORT;
    private static final int WRITE_IDLE_TIME_PERIOD;
    private static final PandaHeartbeatService pandaHeartbeatService;
    private static final int RETRY_CONNECT_INTERVAL;

    static {
        PROPERTIES_CONFIG = BeanContext.getBean(PandaConfig.class);
        pandaHeartbeatService = BeanContext.getBean(PandaHeartbeatService.class);
        HOSTNAME = PROPERTIES_CONFIG.getHostname();
        PORT = PROPERTIES_CONFIG.getPort();
        WRITE_IDLE_TIME_PERIOD = PROPERTIES_CONFIG.getHeartbeatPeriod();
        RETRY_CONNECT_INTERVAL = PROPERTIES_CONFIG.getRetryConnectInterval();
    }

    /**
     * 心跳超时重试次数
     */
    public static int retryCount;

    /**
     * 当客户端超过指定时间没有触发写事件给服务端时，该方法将被触发调用
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent stateEvent = (IdleStateEvent) evt;
            // 如果写空闲超时
            if (stateEvent.state() == IdleState.WRITER_IDLE) {
                retryCount++;
                logger.info("panda客户端[{}] 向服务器写数据超时，表明心跳连接失败", ctx.channel().localAddress());
                if (retryCount <= 3) {
                    // send
                    logger.info("panda客户端[{}] 重新发送心跳给panda server，重试次数:{}次",
                            ctx.channel().localAddress(), retryCount);
                    byte[] retrySendBytes = pandaHeartbeatService.assembleHeartbeatByte();
                    ctx.writeAndFlush(Unpooled.copiedBuffer(retrySendBytes));
                    Log.printHeartbeatMessage(retrySendBytes);
                } else {
                    logger.info("panda客户端[{}] 向panda server重发心跳次数已达3次，即将重新建立tcp连接", ctx.channel().localAddress());
                    ChannelFuture channelFuture = ctx.close();
                    if (channelFuture.isSuccess()) {
                        logger.info("ChannelHandlerContext关闭成功, 即将重新建立连接");
                        // 执行启动panda tcp client
                        ThreadPoolUtil.getExecutorService().execute(
                                new PandaTcpClient(HOSTNAME, PORT, WRITE_IDLE_TIME_PERIOD, RETRY_CONNECT_INTERVAL));
                    }
                    // TODO 发送告警
                }
            } else {
                logger.error("触发非写事件超时，这表明配置IdleStateEvent可能出现问题");
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
