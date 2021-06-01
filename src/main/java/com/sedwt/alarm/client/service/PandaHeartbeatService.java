package com.sedwt.alarm.client.service;

import com.sedwt.alarm.client.config.PandaConfig;
import com.sedwt.alarm.client.utils.ByteUtil;
import com.sedwt.alarm.client.utils.Log;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author : yj zhang
 * @since : 2021/5/8 16:06
 */
@Service
public class PandaHeartbeatService {
    private static final Logger logger = LoggerFactory.getLogger("heartbeatService");

    @Autowired
    private PandaConfig pandaConfig;

    /**
     * 封装心跳包
     *
     * @return 已封装好的心跳包
     */
    public byte[] assembleHeartbeatByte() throws Exception {
        // step1 封装数据包
        byte[] bytes = new byte[24];

        // =================================帧头====================================
        bytes[0] = (byte) 0xFF;

        // =================================帧体====================================

        // 1. 长度（4bytes）
        bytes[1] = 0x00;
        bytes[2] = 0x00;
        bytes[3] = 0x00;
        bytes[4] = 0x12;

        // 2. 消息头 (10 bytes)
        // (1) 请求标志 (1 byte)
        bytes[5] = 0x00;
        // (2) 消息体 (1 byte)
        bytes[6] = 0x01;
        // (3) 时间戳 (8 bytes)
        byte[] timestampToByte = ByteUtil.timestampToByte(System.currentTimeMillis(), true);
        // 将timestampToByte拷贝到bytes中
        System.arraycopy(timestampToByte, 0, bytes, 7, timestampToByte.length);

        // 3. 消息体 (4 bytes) 设备标识
        bytes[15] = 0x02;
        bytes[16] = 0x01;
        bytes[17] = 0x10;
        bytes[18] = 0x01;

        // 4. 校验码（4bytes）帧体的长度、消息头和消息体字段的数据生成 CRC32 校验值
        byte[] crc32ConvertByteArray = ByteUtil.crc32Convert(bytes, 1, 18);
        System.arraycopy(crc32ConvertByteArray, 0, bytes, 19, 4);

        // =================================帧尾====================================
        bytes[23] = (byte) 0xFE;


        // step2  对需要转义的字符进行转义
        return ByteUtil.escapeByteArray(bytes);
    }

    /**
     * 发送心跳包
     *
     * @param ctx
     */
    public void sendHeartbeat(ChannelHandlerContext ctx) {
        if (ctx == null || ctx.channel() == null) {
            return;
        }
        final Channel channel = ctx.channel();
        channel.eventLoop().schedule(() -> {
            try {
                byte[] bytes = assembleHeartbeatByte();
                ChannelFuture channelFuture = ctx.writeAndFlush(Unpooled.copiedBuffer(bytes));
                if (channelFuture.isSuccess()) {
                    logger.info("客户端[{}]发送心跳给服务端", channel.localAddress());
                    Log.printHeartbeatMessage(bytes);
                }
            } catch (Exception e) {
                logger.error("发送心跳异常");
            }
        }, pandaConfig.getHeartbeatPeriod(), TimeUnit.SECONDS);
    }
}
