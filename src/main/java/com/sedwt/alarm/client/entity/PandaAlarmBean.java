package com.sedwt.alarm.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * @author Administrator
 */
@Getter
@Setter
@ToString
public class PandaAlarmBean {
    private Integer unitId;
    /**
     * 站点类型 0x01 列车 0x02 车站
     */
    private Integer siteType;

    /**
     * 站点编号 对应列车号
     */
    private Integer siteNo;

    /**
     * 设备类型  tau 0x11 iph 0x12
     */
    private Integer deviceType;

    /**
     * 设备编号 对应设备location.number, 1.2.3.4
     */
    private Integer deviceNo;

    /**
     * 通讯状态 在线0x00 掉线0x01
     */
    private Integer commStatus;

    /**
     * 保留字段 默认为0
     */
    private Integer retain = 0x00;

    /**
     * 设备模块编号 普通告警 0x00 网口告警 0xF1
     */
    private Integer moduleNo;

    /**
     * 子状态编号 对应告警码
     */
    private Integer alarmCode;

    /**
     * 状态值 0x00 恢复  0x01 一般告警  0x02 重要告警  0x03 紧急告警
     */
    private Integer alarmStatus;

    private Integer alarmLevel;

    private Integer isOnline;

    /**
     * 消息类型
     * 0x02 : 告警主动上报
     * 0x08 : 设备通信状态主动上报
     */
    private Integer messageType;

    /**
     * Lan口告警
     */
    private Integer portNum;

    /**
     * 告警时间
     */
    private Date alarmTime;

    /**
     * 告警名称
     */
    private String alarmName;

    private String subwayNum;
}
