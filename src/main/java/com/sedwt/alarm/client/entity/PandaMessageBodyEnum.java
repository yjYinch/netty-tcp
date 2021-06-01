package com.sedwt.alarm.client.entity;

/**
 * @author : yj zhang
 * @since : 2021/5/10 10:09
 */

public enum PandaMessageBodyEnum {
    /**
     * 进制位标识
     */
    HEX_SIGNAL((byte) 0xFF),
    /**
     * 帧头
     */
    FRAME_HEAD((byte) 0xFF),
    /**
     * 帧尾
     */
    FRAME_TAIL((byte) 0xFE),
    /**
     * 转义标识符
     */
    ESCAPE_CHARACTER((byte) 0xFD);

    private final byte val;

    PandaMessageBodyEnum(byte val) {
        this.val = val;
    }

    public byte getVal() {
        return val;
    }
}
