package com.sedwt.alarm.client.entity;

public enum PandaResultEnum {
    /**
     * 0x00 成功   0xFF 其它未定义的错误
     */
    SUCCESS((byte) 0x00),
    /**
     * 0x01 无效的节点标识码
     */
    INVALID_NODE_CODE((byte) 0x01),

    /**
     * 0x02 无效的消息类型码
     */
    INVALID_MESSAGE_TYPE_CODE((byte) 0x02),

    /**
     * 0xFF 其它未定义的错误
     */
    UNKNOWN_ERROR((byte) 0xFF);


    private final byte val;

    PandaResultEnum(byte val) {
        this.val = val;
    }

    public byte getVal() {
        return val;
    }
}
