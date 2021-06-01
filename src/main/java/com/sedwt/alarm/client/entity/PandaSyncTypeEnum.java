package com.sedwt.alarm.client.entity;

public enum PandaSyncTypeEnum {
    /**
     * 全部同步
     */
    WHOLE("whole"),
    /**
     * 单个设备同步
     */
    SINGLE("single");


    private String name;

    PandaSyncTypeEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
