package com.sedwt.alarm.client.handler;

import com.sedwt.alarm.client.entity.PandaMessageBodyEnum;
import com.sedwt.alarm.client.entity.PandaMessageTypeEnum;
import com.sedwt.alarm.client.entity.PandaPacketTypeEnum;
import com.sedwt.alarm.client.entity.PandaResponse;
import com.sedwt.alarm.client.entity.PandaResponseResultEnum;
import com.sedwt.alarm.client.entity.PandaSyncTypeEnum;
import com.sedwt.alarm.client.service.AlarmSyncService;
import com.sedwt.alarm.client.utils.ByteUtil;
import com.sedwt.alarm.client.utils.Log;
import com.sedwt.alarm.client.utils.BeanContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.sedwt.alarm.client.utils.ByteUtil.isSameMessageType;
import static com.sedwt.alarm.client.utils.ByteUtil.isSamePacketType;
import static com.sedwt.alarm.client.utils.ByteUtil.isSuccessResponse;

/**
 * @author : yj zhang
 * @since : 2021/5/8 13:52
 */
@Slf4j
public class PandaDecodeHandler extends ByteToMessageDecoder {
    private static final AlarmSyncService alarmSyncService;

    static {
        alarmSyncService = BeanContext.getBean(AlarmSyncService.class);
    }

    /**
     * 解析服务端的响应信息，根据响应信息来确定是何种消息响应类型 并封装到实体类PandaMessageType中，
     * 传给PandaTaskHandler的channelRead方法来处理。
     * <p>
     * 0x01 : 心跳响应
     * 0x02 : 设备状态主动上传响应
     * 0x08 : 代理设备通信状态主动上传响应
     * 0x09 : 代理设备通信状态同步上传响应
     * 0x10 : 代理设备状态同步上传
     *
     * @param ctx
     * @param in
     * @param out
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int length = 1 + 4 + 1 + 1 + 8 + 1 + 4 + 1;
        boolean headFlag = false;
        List<Byte> list = new ArrayList<>();
        while (in.isReadable()) {
            int b = in.readByte();
            // 0xFF
            if ((b & PandaMessageBodyEnum.HEX_SIGNAL.getVal()) == PandaMessageBodyEnum.FRAME_HEAD.getVal()) {
                headFlag = true;
            } else if ((b & PandaMessageBodyEnum.HEX_SIGNAL.getVal()) == PandaMessageBodyEnum.FRAME_TAIL.getVal()) {
                list.add((byte) b);
                break;
            }
            if (headFlag) {
                list.add((byte) b);
            }
        }
        int size = list.size();
        List<Byte> newList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Byte aByte = list.get(i);
            // 如果是0xFD, 将后一位字节还原（+128）
            if (aByte == PandaMessageBodyEnum.ESCAPE_CHARACTER.getVal() && (++i < size)) {
                Byte aByte1 = list.get(i);
                if (aByte1 == 0x7F) {
                    newList.add((byte) 0xFF);
                } else if (aByte1 == 0x7E) {
                    newList.add((byte) 0xFE);
                } else if (aByte1 == 0x7D) {
                    newList.add((byte) 0xFD);
                } else {
                    log.error("转义字符0xFD后一位不是0x7D、0x7E、0x7F中的一种");
                }
                continue;
            }
            newList.add(aByte);
        }
        list.clear();
        byte[] responseMessage = new byte[newList.size()];
        for (int i = 0; i < newList.size(); i++) {
            responseMessage[i] = newList.get(i);
        }

        if (newList.size() < length) {
            log.error("字节长度不满足最低要求, 目标：{}, 实际：{}", length, newList.size());
            ByteUtil.printInConsole(responseMessage);
            return;
        }
        // - 先判断报文类型 再判断消息类型
        PandaResponse messageType = new PandaResponse();
        if (isSamePacketType(responseMessage, PandaPacketTypeEnum.REQUEST_MESSAGE.getVal())) {
            // (1) 接收请求报文
            // - 判断消息类型
            // (1.1) 同步告警状态请求
            if (isSameMessageType(responseMessage, PandaMessageTypeEnum.CLIENT_STATUS_SYNC_COMMOND.getVal())) {
                log.info("receive [request] message from server, message type[{}], message content: {}",
                        PandaMessageTypeEnum.CLIENT_STATUS_SYNC_COMMOND.name(), ByteUtil.printBytesToConsole(responseMessage));
                messageType.setType(PandaMessageTypeEnum.CLIENT_STATUS_SYNC_COMMOND.name());
                // 判断是单个设备状态同步还是全部设备状态同步
                if (responseMessage[15] == (byte) 0x02 && responseMessage[17] == (byte) 0x10) {
                    messageType.setSyncType(PandaSyncTypeEnum.WHOLE.name());
                    messageType.setResult(PandaResponseResultEnum.SUCCESS.name());
                } else if (responseMessage[15] == (byte) 0x01
                        && (responseMessage[17] == (byte) 0x11 || responseMessage[17] == (byte) 0x12)) {
                    messageType.setSyncType(PandaSyncTypeEnum.SINGLE.name());
                    Integer subwayNo = ((Byte) responseMessage[16]).intValue();
                    Integer deviceType = ((Byte) responseMessage[17]).intValue();
                    Integer deviceNo = ((Byte) responseMessage[18]).intValue();
                    Integer unitId = alarmSyncService.isExistDevice(subwayNo, deviceType, deviceNo);
                    if (unitId != null) {
                        messageType.setUnitId(unitId);
                        messageType.setResult(PandaResponseResultEnum.SUCCESS.name());
                    } else {
                        messageType.setResult(PandaResponseResultEnum.FAIL.name());
                    }
                }
            }
        } else if (isSamePacketType(responseMessage, PandaPacketTypeEnum.RESPONSE_MESSAGE.getVal())) {
            // (2) 接收响应报文
            // - 判断响应结果
            if (isSuccessResponse(responseMessage)) {
                messageType.setResult(PandaResponseResultEnum.SUCCESS.name());
            } else {
                messageType.setResult(PandaResponseResultEnum.FAIL.name());
            }

            // - 判断消息类型
            if (isSameMessageType(responseMessage, PandaMessageTypeEnum.HEART_BEAT.getVal())) {
                // 1.心跳响应类型
                messageType.setType(PandaMessageTypeEnum.HEART_BEAT.name());
                // 重置心跳重试次数
                PandaClientHeartbeatHandler.retryCount = 0;
                Log.printResponseMessage(responseMessage, "[HEART_BEAT] Response");
            } else if (isSameMessageType(responseMessage, PandaMessageTypeEnum.DEVICE_STATUS_UPLOAD.getVal())) {
                // 2.设备状态主动上传响应类型
                messageType.setType(PandaMessageTypeEnum.DEVICE_STATUS_UPLOAD.name());
                log.info("receive [response] message from server, message type: {}, result: {}, message ",
                        PandaMessageTypeEnum.DEVICE_STATUS_UPLOAD.name(),
                        messageType.getResult());
                Log.printResponseMessage(responseMessage, "[设备状态主动上传] Response");
            } else if (isSameMessageType(responseMessage, PandaMessageTypeEnum.PROXY_DEVICE_COMMUNICATE_INITIATIVE_UPLOAD.getVal())) {
                // 3.代理设备通信状态主动上传响应类型
                messageType.setType(PandaMessageTypeEnum.PROXY_DEVICE_COMMUNICATE_INITIATIVE_UPLOAD.name());
                log.info("receive [response] message from server, message type: [{}], result: [{}], message",
                        PandaMessageTypeEnum.PROXY_DEVICE_COMMUNICATE_INITIATIVE_UPLOAD.name(),
                        messageType.getResult());
                Log.printResponseMessage(responseMessage, "[设备通信状态主动上传] Response");
            } else if (isSameMessageType(responseMessage, PandaMessageTypeEnum.PROXY_DEVICE_COMMUNICATE_SYNC_UPLOAD.getVal())) {
                // 4.代理设备通信状态同步上传响应类型
                messageType.setType(PandaMessageTypeEnum.PROXY_DEVICE_COMMUNICATE_SYNC_UPLOAD.name());
                log.info("receive [response] message from server, message type: [{}], result: [{}], message " +
                                "content: {}",
                        PandaMessageTypeEnum.PROXY_DEVICE_COMMUNICATE_SYNC_UPLOAD.name(),
                        messageType.getResult(), ByteUtil.printBytesToConsole(responseMessage));
            } else if (isSameMessageType(responseMessage, PandaMessageTypeEnum.PROXY_DEVICE_STATUS_SYNC_UPLOAD.getVal())) {
                // 5.代理设备状态同步上传响应类型
                messageType.setType(PandaMessageTypeEnum.PROXY_DEVICE_STATUS_SYNC_UPLOAD.name());
                log.info("receive [response] message from server, message type: [{}], result: [{}], message " +
                                "content: {}",
                        PandaMessageTypeEnum.PROXY_DEVICE_STATUS_SYNC_UPLOAD.name(),
                        messageType.getResult(), ByteUtil.printBytesToConsole(responseMessage));
            } else {
                messageType.setType("unknown");
            }
        } else {
            // (3) 非法报文
            log.error("非法的报文类型");
        }
        out.add(messageType);
    }

}
