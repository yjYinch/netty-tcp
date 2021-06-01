package com.sedwt.alarm.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.sedwt.alarm.client.entity.PandaAlarmBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
@DS("tau")
public interface StatusSyncMapper {

    /**
     * 根据列车号和设备编号查询设备
     *
     * @param subwayNo 列车号
     * @param deviceNo 设备号
     * @return 设备unitId
     */
    Integer getUnit(String subwayNo, String deviceNo);

    /**
     * 获取tau网管系统下的所有tau设备的在线状态
     *
     * @return 网管系统下的所有tau设备的在线状态
     */
    List<PandaAlarmBean> getCommStatus();
}
