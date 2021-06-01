package com.sedwt.alarm.client.service;

import com.sedwt.alarm.client.common.SyncAdvice;
import com.sedwt.alarm.client.entity.PandaAlarmBean;
import com.sedwt.alarm.client.entity.PandaMessageBodyEnum;
import com.sedwt.alarm.client.entity.PandaMessageTypeEnum;
import com.sedwt.alarm.client.utils.ByteUtil;
import com.sedwt.alarm.client.utils.RedisUtil;
import com.sedwt.alarm.mapper.AlarmSyncMapper;
import com.sedwt.alarm.mapper.StatusSyncMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author sedwt
 */
@Service
@Slf4j
public class AlarmSyncService {
    /**
     * 当收到客户端确认时，该字段设为true
     */
    public static volatile boolean isSuccessInform = false;
    @Value("${system.tau.siteNo}")
    public Integer siteNo;
    @Value("${system.tau.deviceNo}")
    public Integer deviceNo;

    @Resource
    private StatusSyncMapper statusSyncMapper;
    @Resource
    private AlarmSyncMapper alarmSyncMapper;

    public void sendSync(ChannelHandlerContext ctx, Integer unitId) {
        // 设备告警同步标识
        SyncAdvice.syncClientAlarm = true;
        Channel channel = ctx.channel();
        int sendCount = 0;
        try {
            byte[] bytes = alarmStatusBytes(unitId);
            channel.writeAndFlush(Unpooled.copiedBuffer(bytes));
            log.info("send [request] message to server, message type [{}], message content: {}",
                    PandaMessageTypeEnum.PROXY_DEVICE_STATUS_SYNC_UPLOAD.name(), ByteUtil.printBytesToConsole(bytes));
            sendCount++;
            // 以流水id作为唯一key，设置过期时间为5s
            String alarmNumKey = Long.toString(System.currentTimeMillis());
            RedisUtil.set(alarmNumKey, bytes, 5);
            // 未收到告警确认消息时
            while (!AlarmSyncService.isSuccessInform) {
                // 如果超时
                if (RedisUtil.get(alarmNumKey) == null) {
                    //如果发送次数<3，则重复发送
                    if (sendCount < 3) {
                        RedisUtil.set(alarmNumKey, bytes, 5);
                        channel.writeAndFlush(Unpooled.copiedBuffer(bytes));
                        sendCount++;
                        log.info("send [request] message to server timeout! message type [{}], retry times [{}], " +
                                        "message content: {}",
                                PandaMessageTypeEnum.PROXY_DEVICE_STATUS_SYNC_UPLOAD.name(),
                                sendCount, ByteUtil.printBytesToConsole(bytes));
                    } else {
                        // 超过3次未确认，就丢弃该告警，不再上报
                        log.info("message is not received for more than 3 times, discard the message：{}", ByteUtil.printBytesToConsole(bytes));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("同步告警信息给熊猫监控平台异常：{}", e.getMessage());
        } finally {
            SyncAdvice.syncClientAlarm = false;
        }
    }

    private Map<Integer, List<PandaAlarmBean>> getAlarmStatusMap(Integer id) {
        // 1. 查询告警同步表alarm_sync中正在告警的数据
        List<PandaAlarmBean> syncAlarmList;
        syncAlarmList = alarmSyncMapper.getAlarmSyncInfo(id);
        // 2. 查询所有的unit和对应的列车号和设备编号、在线状态
        List<PandaAlarmBean> commStatus = statusSyncMapper.getCommStatus();
        // 3. 以unit当key PandaAlarmBean当value 把list转成map
        Map<Integer, PandaAlarmBean> unitMap = commStatus.stream().collect(Collectors.toMap(PandaAlarmBean::getUnitId,
                Function.identity()));

        List<PandaAlarmBean> noUnitList = new ArrayList<>();
        // 4. 遍历syncAlarmList，从unitMap中取出对应设备的列车号和设备编号、在线状态
        for (PandaAlarmBean bean : syncAlarmList) {
            Integer unitId = bean.getUnitId();
            if (unitMap.containsKey(unitId)) {
                PandaAlarmBean pandaAlarmBean = unitMap.get(unitId);
                bean.setDeviceNo(pandaAlarmBean.getDeviceNo());
                String subwayNum = pandaAlarmBean.getSubwayNum();
                String subwayNo = subwayNum == null ? "0" : subwayNum.substring(subwayNum.length() - 2);
                bean.setSiteNo(Integer.parseInt(subwayNo));
                bean.setIsOnline(pandaAlarmBean.getIsOnline());
            } else {
                // 5. 如果unitMap中找不到对应设备，则记录下来此设备在list中的index
                noUnitList.add(bean);
            }
        }

        // 6. 如果noUnitList不是空，则删除syncAlarmList中对应告警信息，不同步给集中告警平台
        if (!noUnitList.isEmpty()) {
            syncAlarmList.removeAll(noUnitList);
        }

        // 7. 把syncAlarmList中的告警数据根据unit进行分组
        Map<Integer, List<PandaAlarmBean>> mapo = new HashMap<>();
        syncAlarmList.forEach(bean -> {
            Integer unitId = bean.getUnitId();
            if (mapo.containsKey(unitId)) {
                List<PandaAlarmBean> alarmSyncBeans = mapo.get(unitId);
                alarmSyncBeans.add(bean);
                mapo.put(unitId, alarmSyncBeans);
            } else {
                List<PandaAlarmBean> list = new ArrayList<>();
                list.add(bean);
                mapo.put(bean.getUnitId(), list);
            }
        });
        return mapo;
    }

    public byte[] alarmStatusBytes(Integer id) throws Exception {
        Map<Integer, List<PandaAlarmBean>> map = getAlarmStatusMap(id);
        // 报文可边长部分使用ArrayList进行拼装
        ArrayList<Byte> list = new ArrayList<>();
        map.forEach((unitId, alarmList) -> {
            //(1)站点类型 0x01 列车 0x02 车站
            list.add((byte) 0x01);
            //(2)站点编号 对应列车号
            list.add(Optional.ofNullable(alarmList.get(0).getSiteNo()).map(Integer::byteValue).orElse((byte) 0x00));
            //(3)设备类型 tau固定0x11
            list.add((byte) 0x11);
            //(4)设备编号 对应设备location.number, 1.2.3.4
            list.add(Optional.ofNullable(alarmList.get(0).getDeviceNo()).map(Integer::byteValue).orElse((byte) 0x00));
            //(5)子状态数量
            int k = alarmList.size();
            list.add((byte) k);
            alarmList.forEach(alarmBean -> {
                //(6.1)保留字段 默认是0x00
                list.add((byte) 0x00);
                //(6.2)设备类型 tau固定0x11
                list.add((byte) 0x11);
                //(6.3)模块编号 网口告警是0xF1 普通告警是0x00
                if (alarmBean.getAlarmCode() == 11) {
                    list.add((byte) (alarmBean.getModuleNo() == null ? 0x00 : +alarmBean.getModuleNo() + 15 * 16));
                } else {
                    list.add((byte) 0x00);
                }
                //(6.4)子状态编号 对应告警编号
                list.add(Optional.ofNullable(alarmBean.getAlarmCode()).map(Integer::byteValue).orElse((byte) 0x00));
                //(6.5)告警状态
                if (alarmBean.getAlarmStatus() == 0) {
                    list.add((byte) 0x00);
                } else {
                    list.add(Optional.ofNullable(alarmBean.getAlarmLevel()).map(Integer::byteValue).orElse((byte) 0x00));
                }
            });
        });

        int m = list.size();
        byte[] bytes = new byte[26 + m];
        // 1.固定帧头
        bytes[0] = PandaMessageBodyEnum.FRAME_HEAD.getVal();
        // 2.帧体
        // 2.1 长度 4byte
        bytes[1] = (byte) 0x00;
        bytes[2] = (byte) 0x00;
        bytes[3] = (byte) 0x00;
        bytes[4] = (byte) ((byte) 20 + m);

        // 2.2 消息头 10byte
        // 2.2.1 报文类型 1byte
        bytes[5] = (byte) 0x00;
        // 2.2.2 消息类型 1byte
        bytes[6] = PandaMessageTypeEnum.PROXY_DEVICE_STATUS_SYNC_UPLOAD.getVal();
        // 2.2.3 时间戳 8byte
        byte[] timestampToByte = ByteUtil.timestampToByte(System.currentTimeMillis(), true);
        // 将timestampToByte拷贝到bytes中
        System.arraycopy(timestampToByte, 0, bytes, 7, timestampToByte.length);

        // 2.3 消息体 N byte
        // 2.3.1 设备标识码 4byte
        // 2.3.1.1 站点类型 0x01: 列车 0x02: 车站
        bytes[15] = (byte) 0x02;
        // 2.3.1.2 站点编号
        bytes[16] = siteNo == null ? (byte) 0x01 : siteNo.byteValue();
        // 2.3.1.3 设备类型 0x10: 网管代理服务器 0x11: 车载无线接入设备TAU 0x12: 乘客紧急通话设备IPH
        bytes[17] = (byte) 0x10;
        // 2.3.1.4 设备编号
        bytes[18] = deviceNo == null ? (byte) 0x01 : deviceNo.byteValue();

        // 2.3.1.5 设备数量
        bytes[19] = (byte) 0x00;
        bytes[20] = (byte) map.size();

        int i = 0;
        for (Byte aByte : list) {
            i++;
            bytes[20 + i] = aByte;
        }

        // 2.4 校验CRC32码 4byte
        byte[] bytesFromCrc32 = ByteUtil.crc32Convert(bytes, 1, 20 + i);
        bytes[20 + i + 1] = bytesFromCrc32[0];
        bytes[20 + i + 2] = bytesFromCrc32[1];
        bytes[20 + i + 3] = bytesFromCrc32[2];
        bytes[20 + i + 4] = bytesFromCrc32[3];

        // 3.固定帧尾 1byte
        bytes[20 + i + 5] = PandaMessageBodyEnum.FRAME_TAIL.getVal();

        // 4.0xFF和0xFE转义
        return ByteUtil.escapeByteArray(bytes);
    }

    public Integer isExistDevice(Integer subwayNo, Integer deviceType, Integer deviceNo) {
        if (deviceType == 17) {
            return statusSyncMapper.getUnit(subwayNo.toString(), deviceNo.toString());
        }
        return null;
    }

}
