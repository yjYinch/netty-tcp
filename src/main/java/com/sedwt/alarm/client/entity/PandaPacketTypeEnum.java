package com.sedwt.alarm.client.entity;

public enum PandaPacketTypeEnum {
    /**
     * 请求报文
     */
    REQUEST_MESSAGE((byte) 0x00),
    /**
     * 响应报文
     */
    RESPONSE_MESSAGE((byte) 0x01);

    private final byte val;

    PandaPacketTypeEnum(byte val) {
        this.val = val;
    }

    public byte getVal() {
        return val;
    }
}
