package com.sedwt.alarm.client.entity;

public enum PandaResponseResultEnum {
    /**
     * 成功
     */
    SUCCESS("success"),
    /**
     * 需要重试
     */
    RETRY("retry"),

    /**
     * 失败
     */
    FAIL("fail");


    private String name;

    PandaResponseResultEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
