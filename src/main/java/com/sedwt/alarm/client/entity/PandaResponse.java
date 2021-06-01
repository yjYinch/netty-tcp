package com.sedwt.alarm.client.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * @author : yj zhang
 * @since : 2021/5/10 10:23
 */

@Getter
@Setter
public class PandaResponse {
    /**
     * 消息类型
     */
    private String type;

    private String syncType;

    private Integer unitId;

    private String result;
}
