package com.sedwt.alarm.client.entity;

/**
 * @author : yj zhang
 * @since : 2021/5/10 9:43
 */

public enum PandaMessageTypeEnum {
    /**
     * 心跳消息类型标识符
     */
    HEART_BEAT((byte) 0x01),
    /**
     * 设备状态主动上传
     */
    DEVICE_STATUS_UPLOAD((byte) 0x02),

    /**
     * 设备状态同步命令
     */
    CLIENT_STATUS_SYNC_COMMOND((byte) 0x03),

    /**
     * 代理设备通信状态主动上传
     */
    PROXY_DEVICE_COMMUNICATE_INITIATIVE_UPLOAD((byte) 0x08),

    /**
     * 代理设备通信状态同步上传
     */
    PROXY_DEVICE_COMMUNICATE_SYNC_UPLOAD((byte) 0x09),

    /**
     * 代理设备状态同步上传
     */
    PROXY_DEVICE_STATUS_SYNC_UPLOAD((byte) 0x10);

    private final byte val;

    PandaMessageTypeEnum(byte val) {
        this.val = val;
    }

    public byte getVal() {
        return val;
    }
}
