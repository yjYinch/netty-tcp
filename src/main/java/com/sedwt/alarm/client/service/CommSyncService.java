package com.sedwt.alarm.client.service;

import com.sedwt.alarm.client.common.SyncAdvice;
import com.sedwt.alarm.client.entity.PandaAlarmBean;
import com.sedwt.alarm.client.entity.PandaMessageBodyEnum;
import com.sedwt.alarm.client.entity.PandaMessageTypeEnum;
import com.sedwt.alarm.client.utils.ByteUtil;
import com.sedwt.alarm.client.utils.Log;
import com.sedwt.alarm.client.utils.RedisUtil;
import com.sedwt.alarm.mapper.StatusSyncMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author sedwt
 */
@Service
@Slf4j
public class CommSyncService {
    /**
     * 当收到客户端确认时，该字段设为true
     */
    public static volatile boolean isSuccessInform = false;
    @Value("${system.tau.siteNo}")
    public Integer siteNo;
    @Value("${system.tau.deviceNo}")
    public Integer deviceNo;

    @Resource
    private StatusSyncMapper statusSyncMapper;

    public void sendSync(ChannelHandlerContext ctx) {
        // 正在同步标识
        SyncAdvice.syncClientStatus = true;
        int sendCount = 0;
        Channel channel = ctx.channel();
        try {
            //1.首次发送
            byte[] bytes = commStatusBytes();
            channel.writeAndFlush(Unpooled.copiedBuffer(bytes));
            log.info("send [request] message to server, message type [{}], message content: {}",
                    PandaMessageTypeEnum.PROXY_DEVICE_COMMUNICATE_SYNC_UPLOAD.name(), ByteUtil.printBytesToConsole(bytes));
            //2.如果超时未收到响应 重发
            sendCount++;
            // (1)以当前时间戳作为唯一key，设置过期时间为5s
            String alarmNumKey = Long.toString(System.currentTimeMillis());
            RedisUtil.set(alarmNumKey, bytes, 5);
            // (2)循环判断信息确认标志
            while (!CommSyncService.isSuccessInform) {
                // (3)如果redis数据过期，未收到响应，则判断为超时
                if (RedisUtil.get(alarmNumKey) == null) {
                    //(4)如果发送次数 < 3，则重复发送
                    if (sendCount < 3) {
                        RedisUtil.set(alarmNumKey, bytes, 5);
                        channel.writeAndFlush(Unpooled.copiedBuffer(bytes));
                        sendCount++;
                        log.info("send [request] message to server timeout! message type [{}], retry times [{}], " +
                                        "message content: {}",
                                PandaMessageTypeEnum.PROXY_DEVICE_COMMUNICATE_SYNC_UPLOAD.name(),
                                sendCount, ByteUtil.printBytesToConsole(bytes));
                    } else {
                        //(5)超过3次未确认，就丢弃该消息，不再上报
                        log.info("message is not received for more than 3 times, discard the message：{}",
                                ByteUtil.printBytesToConsole(bytes));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("上报通信状态给熊猫监控平台异常：{}", e.getMessage());
        } finally {
            SyncAdvice.syncClientStatus = false;
        }
    }

    private List<PandaAlarmBean> getCommStatusMap() {
        // 1. 查询所有设备的在线状态
        return statusSyncMapper.getCommStatus().stream().filter(t -> t.getIsOnline() == 1).collect(Collectors.toList());
    }

    private byte[] commStatusBytes() throws Exception {
        List<PandaAlarmBean> commStatus = getCommStatusMap();
        int n = commStatus.size();
        byte[] bytes = new byte[26 + 5 * n];
        // 1.固定帧头
        bytes[0] = PandaMessageBodyEnum.FRAME_HEAD.getVal();
        // 2.帧体
        // 2.1 长度 4byte
        bytes[1] = (byte) 0x00;
        bytes[2] = (byte) 0x00;
        bytes[3] = (byte) 0x00;
        bytes[4] = (byte) ((byte) 20 + 5 * n);
        // 2.2 消息头 10byte
        // 2.2.1 报文类型 1byte
        bytes[5] = (byte) 0x00;
        // 2.2.2 消息类型 1byte
        bytes[6] = PandaMessageTypeEnum.PROXY_DEVICE_COMMUNICATE_SYNC_UPLOAD.getVal();
        // 2.2.3 时间戳 8byte
        byte[] timestampToByte = ByteUtil.timestampToByte(System.currentTimeMillis(), true);
        // 将timestampToByte拷贝到bytes中
        System.arraycopy(timestampToByte, 0, bytes, 7, timestampToByte.length);
        // 2.3 消息体 N byte
        // 2.3.1 设备标识码 4byte
        // 2.3.1.1 站点类型 0x01: 列车 0x02: 车站
        bytes[15] = (byte) 0x02;
        // 2.3.1.2 站点编号
        bytes[16] = siteNo == null ? (byte) 0x01 : siteNo.byteValue();
        // 2.3.1.3 设备类型 0x10: 网管代理服务器 0x11: 车载无线接入设备TAU 0x12: 乘客紧急通话设备IPH
        bytes[17] = (byte) 0x10;
        // 2.3.1.4 设备编号
        bytes[18] = deviceNo == null ? (byte) 0x01 : deviceNo.byteValue();
        // 2.3.1.5 设备数量
        bytes[19] = (byte) 0x00;
        bytes[20] = (byte) n;
        List<byte[]> list = new ArrayList<>();
        list.add(bytes);
        for (int i = 0; i < n; i++) {
            byte[] copiedByte = new byte[5];
            PandaAlarmBean pandaAlarmBean = commStatus.get(i);
            bytes[21 + 5 * i] = pandaAlarmBean.getSiteType() == null ? (byte) 0x01 : pandaAlarmBean.getSiteType().byteValue();
            copiedByte[0] = bytes[21 + 5 * i];
            String subwayNum = pandaAlarmBean.getSubwayNum();
            String subwayNo = subwayNum == null ? "0" : subwayNum.substring(subwayNum.length() - 2);
            bytes[22 + 5 * i] = (byte) Integer.parseInt(subwayNo);
            copiedByte[1] = bytes[22 + 5 * i];
            bytes[23 + 5 * i] = pandaAlarmBean.getDeviceType() == null ? (byte) 0x11 : pandaAlarmBean.getDeviceType().byteValue();
            copiedByte[2] = bytes[23 + 5 * i];
            bytes[24 + 5 * i] = pandaAlarmBean.getDeviceNo().byteValue();
            copiedByte[3] = bytes[24 + 5 * i];
            bytes[25 + 5 * i] = pandaAlarmBean.getIsOnline() == 1 ? (byte) 0x00 : (byte) 0x01;
            copiedByte[4] = bytes[25 + 5 * i];
            list.add(copiedByte);
        }
        // 2.4 校验CRC32码 4byte
        byte[] bytesFromCrc32 = ByteUtil.crc32Convert(bytes, 1, 20 + 5 * n);
        list.add(bytesFromCrc32);
        Log.logSyncCommunication(list);
        bytes[26 + 5 * (n - 1)] = bytesFromCrc32[0];
        bytes[27 + 5 * (n - 1)] = bytesFromCrc32[1];
        bytes[28 + 5 * (n - 1)] = bytesFromCrc32[2];
        bytes[29 + 5 * (n - 1)] = bytesFromCrc32[3];
        // 3.固定帧尾 1byte
        bytes[30 + 5 * (n - 1)] = PandaMessageBodyEnum.FRAME_TAIL.getVal();
        // 4.0xFF和0xFE转义
        return ByteUtil.escapeByteArray(bytes);
    }

}
