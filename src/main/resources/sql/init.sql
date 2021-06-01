-- ----------------------------
-- 告警同步表
-- ----------------------------
DROP TABLE IF EXISTS "public"."alarm_sync";
CREATE TABLE "public"."alarm_sync"
(
    "id"                serial NOT NULL,
    "unit_id"           int4   NOT NULL,
    "alarm_num"         int4   NOT NULL,
    "alarm_code"       int4            DEFAULT 0,
    "alarm_name"        varchar(255) COLLATE "pg_catalog"."default",
    "alarm_status"      int4   NOT NULL DEFAULT 1,
    "alarm_level"       varchar(255) COLLATE "pg_catalog"."default",
    "alarm_type"        int4   NOT NULL DEFAULT 1,
    "subway_num"        int4            DEFAULT 0,
    "number"            int4            DEFAULT 0,
    "rack_num"          int4            DEFAULT 0,
    "rack_position_num" int4            DEFAULT 0,
    "slot_num"          int4            DEFAULT 0,
    "port_num"          int4            DEFAULT 0,
    "station_num"       int4            DEFAULT 0,
    "alarm_time"        timestamptz(6),
    "scope"             varchar(255) COLLATE "pg_catalog"."default",
    "reason"            varchar(255) COLLATE "pg_catalog"."default",
    "advise"            varchar(255) COLLATE "pg_catalog"."default",
    "equipment_type"    varchar(255) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."alarm_sync"."id" IS '告警流水号id';
COMMENT ON COLUMN "public"."alarm_sync"."unit_id" IS '设备编码唯一id';
COMMENT ON COLUMN "public"."alarm_sync"."alarm_num" IS '告警流水号';
COMMENT ON COLUMN "public"."alarm_sync"."alarm_code" IS '告警码';
COMMENT ON COLUMN "public"."alarm_sync"."alarm_name" IS '告警内容名称';
COMMENT ON COLUMN "public"."alarm_sync"."alarm_status" IS '告警状态 1：告警中';
COMMENT ON COLUMN "public"."alarm_sync"."alarm_level" IS '告警等级，一级告警（紧急告警）、二级告警（严重告警）、三级告警（一般告警）、四级告警（提示告警）';
COMMENT ON COLUMN "public"."alarm_sync"."alarm_type" IS '告警类型，默认为1';
COMMENT ON COLUMN "public"."alarm_sync"."subway_num" IS '列车号（仅记录列车号后两位）';
COMMENT ON COLUMN "public"."alarm_sync"."number" IS '编号（tau为编号，iph为车厢+车门）';
COMMENT ON COLUMN "public"."alarm_sync"."rack_num" IS '机架号';
COMMENT ON COLUMN "public"."alarm_sync"."rack_position_num" IS '机架内的位置号';
COMMENT ON COLUMN "public"."alarm_sync"."slot_num" IS '槽位号';
COMMENT ON COLUMN "public"."alarm_sync"."port_num" IS '端口号';
COMMENT ON COLUMN "public"."alarm_sync"."station_num" IS '站点号';
COMMENT ON COLUMN "public"."alarm_sync"."alarm_time" IS '告警时间';
COMMENT ON COLUMN "public"."alarm_sync"."scope" IS '影响范围';
COMMENT ON COLUMN "public"."alarm_sync"."reason" IS '告警原因';
COMMENT ON COLUMN "public"."alarm_sync"."advise" IS '处理建议';
COMMENT ON COLUMN "public"."alarm_sync"."equipment_type" IS '设备类别：TAU或IPH';

ALTER TABLE "public"."alarm_sync"
    ADD CONSTRAINT "alarm_sync_pkey" PRIMARY KEY ("id");
CREATE
    UNIQUE INDEX unit_id_alarm_code_port_num_equipmenttype_idx
    ON alarm_sync("unit_id", "alarm_code", "port_num", "equipment_type");
