package com.sedwt.alarm.client.entity;

import lombok.Data;

import java.util.Date;

/**
 * @author : zhang yijun
 * @date : 2020/11/23 17:44
 * @description : 告警的基本信息表，里面的字段暂时未确定
 */
@Data
public class AlarmSyncBean {

    /**
     * 告警同步表id
     */
    private Integer id;

    /**
     * 子系统编码
     */
    private Integer systemCode;

    /**
     * 车站编号 0-102 占用一个字节，取值范围为0x00 -- 0x66
     */
    private Integer stationNum;

    /**
     * 设备编号，占用2个字节, 关联userName
     */
    private Integer unitId;

    /**
     * 告警编号，2个字节, 流水id
     */
    private Integer alarmNum;

    /**
     * 告警码
     */
    private Integer alarmCode;

    /**
     * 告警内容名称
     */
    private String alarmName;

    /**
     * 告警状态，1:告警中 2: 恢复 0xFF：产生 0x00：恢复  告警同步上报时，只能为0xFF
     */
    private Integer alarmStatus;

    /**
     * 告警产生时间
     */
    private Date alarmTime;

    /**
     * 告警等级
     */
    private Integer alarmLevel;

    /**
     * 告警类型，无此项，发送0x00
     */
    private Integer alarmType;

    /**
     * 机架号，无此项，发送0x00
     */
    private Integer rackNum;

    /**
     * 机框号，无此项，发送0x00
     */
    private Integer rackPositionNum;

    /**
     * 槽位号，无此项，发送0x00
     */
    private Integer slotNum;

    /**
     * 端口号，无此项，发送0x00
     */
    private Integer portNum;

    /**
     * 告警范围
     */
    private String scope;

    /**
     * 告警原因
     */
    private String reason;

    /**
     * 告警建议
     */
    private String advise;

    /**
     * 此字段目的是为了告警上报时的响应，收到集中告警系统未确认的次数
     * 当count >= 3 时，丢弃该告警
     */
    private int unconfirmedCount;

    /**
     * 设备类别：tau或iph
     */
    private String equipmentType;

    /**
     * 列车号
     */
    private Integer subwayNum;

    /**
     * 编号
     */
    private Integer number;
}
