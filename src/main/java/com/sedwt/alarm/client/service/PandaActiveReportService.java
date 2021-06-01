package com.sedwt.alarm.client.service;

import com.sedwt.alarm.client.entity.PandaAlarmBean;
import com.sedwt.alarm.client.entity.PandaMessageTypeEnum;
import com.sedwt.alarm.client.utils.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author : yj zhang
 * @since : 2021/5/12 14:47
 */
@Service
public class PandaActiveReportService {
    private static final Logger logger = LoggerFactory.getLogger(PandaActiveReportService.class);

    /**
     * 代理设备通信状态主动上传
     *
     * @param pandaAlarmBean
     * @return
     */
    public byte[] proxyDeviceCommunicationStatusByteArray(PandaAlarmBean pandaAlarmBean) {
        byte[] bytes = new byte[25];
        byte[] commonMessageByteArray = commonMessageByteArray(pandaAlarmBean,
                PandaMessageTypeEnum.PROXY_DEVICE_COMMUNICATE_INITIATIVE_UPLOAD.name());
        // 拷贝
        System.arraycopy(commonMessageByteArray, 0, bytes, 0, commonMessageByteArray.length);

        // (5) 通信状态， 0x00 在线 0x01 离线
        bytes[19] = pandaAlarmBean.getCommStatus().byteValue();

        // 4. 校验码（4bytes）帧体的长度、消息头和消息体字段的数据生成 CRC32 校验值
        try {
            byte[] crc32ConvertByteArray = ByteUtil.crc32Convert(bytes, 1, 19);
            System.arraycopy(crc32ConvertByteArray, 0, bytes, 20, 4);
        } catch (Exception e) {
            logger.error("CRC32校验异常, 异常原因：{}", e.getMessage());
        }

        // =================================帧尾====================================
        bytes[24] = (byte) 0xFE;

        // step2 转义后返回
        return ByteUtil.escapeByteArray(bytes);
    }

    /**
     * 告警上报（非Lan口告警和Lan口告警）、告警恢复
     *
     * @param pandaAlarmBean
     * @return
     */
    public byte[] deviceAlarmStatusByteArray(PandaAlarmBean pandaAlarmBean) {
        byte[] bytes = new byte[29];
        byte[] commonMessageByteArray = commonMessageByteArray(pandaAlarmBean,
                PandaMessageTypeEnum.DEVICE_STATUS_UPLOAD.name());
        // 拷贝
        System.arraycopy(commonMessageByteArray, 0, bytes, 0, commonMessageByteArray.length);

        // 保留字段
        bytes[19] = 0x00;

        // 设备类型
        Integer deviceType = pandaAlarmBean.getDeviceType();
        bytes[20] = deviceType == null ? 0x00 : deviceType.byteValue();

        // 设备模块, lan口告警为0xF开头 比如0xF1、0xF2、0xF3、0xF4
        Integer portNum = pandaAlarmBean.getPortNum();
        bytes[21] = (byte) ((portNum == null || portNum == 0) ? 0x00 : (240 + portNum));

        // 子状态编码，对应alarmCode
        Integer alarmCode = pandaAlarmBean.getAlarmCode();
        bytes[22] = alarmCode == null ? 0x00 : alarmCode.byteValue();

        // 状态值, 0x00 恢复  0x01 一般告警, 按照level填写
        Integer alarmLevel = pandaAlarmBean.getAlarmLevel();
        Integer alarmStatus = pandaAlarmBean.getAlarmStatus();
        if (alarmStatus != null && alarmStatus == 0){
            bytes[23] = 0x00;
        } else {
            bytes[23] = alarmLevel == null ? 0x01 : alarmLevel.byteValue();
        }

        try {
            byte[] crc32ConvertByteArray = ByteUtil.crc32Convert(bytes, 1, 23);
            System.arraycopy(crc32ConvertByteArray, 0, bytes, 24, 4);
        } catch (Exception e) {
            logger.error("CRC32校验异常, 异常原因：{}", e.getMessage());
        }

        // =================================帧尾====================================
        bytes[28] = (byte) 0xFE;

        // step2 转义后返回
        return ByteUtil.escapeByteArray(bytes);
    }

    /**
     * 主动上报通用的报文格式
     *
     * @param pandaAlarmBean
     * @return
     */
    private static byte[] commonMessageByteArray(PandaAlarmBean pandaAlarmBean, String type) {
        byte[] bytes = new byte[19];
        // =================================帧头====================================
        bytes[0] = (byte) 0xFF;

        // =================================帧体====================================

        // 1. 长度（4bytes）
        bytes[1] = 0x00;
        bytes[2] = 0x00;
        bytes[3] = 0x00;
        if (PandaMessageTypeEnum.PROXY_DEVICE_COMMUNICATE_INITIATIVE_UPLOAD.name().equals(type)) {
            bytes[4] = 0x13;
        } else if (PandaMessageTypeEnum.DEVICE_STATUS_UPLOAD.name().equals(type)){
            bytes[4] = 0x17;
        }

        // 2. 消息头 (10 bytes)
        // (1) 请求标志 (1 byte)
        bytes[5] = 0x00;
        // (2) 消息类型 (1 byte)
        bytes[6] = pandaAlarmBean.getMessageType().byteValue();
        // (3) 时间戳 (8 bytes)
        byte[] timestampToByte = ByteUtil.timestampToByte(System.currentTimeMillis(), true);
        // 将timestampToByte拷贝到bytes中
        System.arraycopy(timestampToByte, 0, bytes, 7, timestampToByte.length);

        // 3. 消息体 (4 bytes) 设备标识
        // (1) 站点类型
        Integer siteType = pandaAlarmBean.getSiteType();
        bytes[15] = siteType == null ? 0x00 : siteType.byteValue();

        // (2) 站点编号
        Integer siteNo = pandaAlarmBean.getSiteNo();
        bytes[16] = siteNo == null ? 0x00 : siteNo.byteValue();

        // (3) 设备类型
        Integer deviceType = pandaAlarmBean.getDeviceType();
        bytes[17] = deviceType == null ? 0x00 : deviceType.byteValue();

        // (4) 设备编号
        Integer deviceNo = pandaAlarmBean.getDeviceNo();
        bytes[18] = deviceType == null ? 0x00 : deviceNo.byteValue();
        return bytes;
    }
}
