package com.sedwt.alarm.mapper;

import com.sedwt.alarm.client.entity.PandaAlarmBean;
import com.sedwt.alarm.client.entity.AlarmSyncBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author : zhang yijun
 * @date : 2020/11/27 14:47
 * @description : 告警同步表
 */
@Mapper
public interface AlarmSyncMapper {
    /**
     * 上报告警到同步告警表
     *
     * @param alarmSyncBean
     */
    void insertAlarmSync(AlarmSyncBean alarmSyncBean);

    /**
     * 删除已恢复的告警, unit_id + alarm_name(alarm_code关联) + port_num可以标识唯一告警
     *
     * @param alarmSyncBean
     * @return
     */
    Integer deleteAlarmSync(AlarmSyncBean alarmSyncBean);

    /**
     * 更新告警同步表, 主要更新告警时间
     *
     * @param alarmSyncBean
     * @return
     */
    Integer updateAlarmSync(AlarmSyncBean alarmSyncBean);

    /**
     * 告警同步查询，查询所有的告警中或者恢复或者所有的告警
     *
     * @param alarmSyncBean
     * @return
     */
    List<AlarmSyncBean> getAlarmSync(AlarmSyncBean alarmSyncBean);

    /**
     * 获取单条告警信息
     *
     * @param alarmSyncBean
     * @return
     */
    AlarmSyncBean getAlarmSyncSingle(AlarmSyncBean alarmSyncBean);

    /**
     * 获取告警同步数据
     *
     * @param unitId 根据unitId可以查指定的设备告警数据
     * @return 告警同步数据
     */
    List<PandaAlarmBean> getAlarmSyncInfo(Integer unitId);
}
