package com.sedwt.alarm.client.common;

/**
 * 同步通知，当告警正在同步时，syncClientStatus == true， syncClientAlarm == true
 *
 * @author : yj zhang
 * @since : 2021/5/14 11:33
 */

public class SyncAdvice {
    /**
     * 同步设备状态， true表示同步中
     */
    public static volatile boolean syncClientStatus = false;

    /**
     * 同步设备告警，true表示同步中
     */
    public static volatile boolean syncClientAlarm = false;

    public static volatile boolean syncClientStatusResponse = false;

    public static volatile boolean syncClientAlarmResponse = false;

    public static volatile boolean isActive = false;

    private SyncAdvice() {
    }

}
