package com.sedwt.alarm.client.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author : yj zhang
 * @since : 2021/5/11 9:12
 */

public class Log {
    private static final Logger logger = LoggerFactory.getLogger("Message Log");

    private Log() {
    }

    /**
     * 心跳请求报文日志打印
     *
     * @param bytes 消息数组
     */
    public static void printHeartbeatMessage(byte[] bytes) {
        String messageType = "[Heartbeat Request] 心跳请求";
        if (bytes == null || bytes.length == 0) {
            logger.error("bytes为空，请确认bytes是否准确, 消息类型：{}", messageType);
            return;
        }
        int timeCount = 0;
        int crc32Count = 0;
        // 表示有转义字符
        if (bytes.length > 24) {
            for (int i = 0; i < bytes.length; i++) {
                if ((bytes[i] & 0xFF) == 0xFD) {
                    if (i <= 19) {
                        // 时间戳有转义字符
                        timeCount++;
                    } else {
                        // CRC32有转义字符
                        crc32Count++;
                    }
                }
            }
        }

        // 遍历数组，记录0xFD的位置
        StringBuilder sb = new StringBuilder(64);

        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            if (i == 0) {
                sb.append("|").append("0x").append(hex.toUpperCase()).append("|  |");
            } else if (i == 4 || i == 5 || i == 6 || i == 18 + timeCount || i == 14 + timeCount || i == 22 + timeCount + crc32Count) {
                sb.append("0x").append(hex.toUpperCase()).append("|  |");
                if (i == 6) {
                    for (int j = 0; j < 8 - timeCount; j++) {
                        sb.append("    ");
                        sb.append(" ");
                    }
                } else if (i == 18 + timeCount) {
                    for (int j = 0; j < 4 - crc32Count; j++) {
                        sb.append("    ");
                        sb.append(" ");
                    }
                }
            } else if (i == bytes.length - 1) {
                sb.append("0x").append(hex.toUpperCase()).append("|");
            } else {
                sb.append("0x").append(hex.toUpperCase()).append(" ");
            }
        }

        int length = sb.length();
        StringBuilder mess = new StringBuilder();
        mess.append("|HEAD|  ").append("|       Length      |  ").append("|Type|  ").append("|Flag|  ");
        mess.append("|                                   Timestamp                                   |  ");
        mess.append("|    MessageType    |  ");
        mess.append("|                 CRC32                 |  ");
        mess.append("|TAIL|");

        StringBuilder fb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            fb.append("-");
        }
        logger.info("{}: \n\n{}\n{}\n{}\n{}\n{}\n{}\n{}\n", messageType, fb.toString(), strForPrintMessage(0),
                fb.toString(), mess.toString(), fb.toString(), sb.toString(), fb.toString());
    }

    /**
     * 打印服务端发过来的报文消息
     *
     * @param bytes       报文消息
     * @param messageType 消息类型
     */
    public static void printResponseMessage(byte[] bytes, String messageType) {
        if (bytes == null || bytes.length != 21) {
            logger.error("bytes为空，请确认bytes是否准确, 消息类型: {}", messageType);
            return;
        }
        StringBuilder sb = new StringBuilder(64);

        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            if (i == 0) {
                sb.append("|").append("0x").append(hex.toUpperCase()).append("|  |");
            } else if (i == 4 || i == 5 || i == 6 || i == 14 || i == 19) {
                sb.append("0x").append(hex.toUpperCase()).append("|  |");
            } else if (i == 15) {
                sb.append("        ");
                sb.append("0x").append(hex.toUpperCase());
                sb.append("       ");
                sb.append("|  |");
            } else if (i == bytes.length - 1) {
                sb.append("0x").append(hex.toUpperCase()).append("|");
            } else {
                sb.append("0x").append(hex.toUpperCase()).append(" ");
            }
        }

        int length = sb.length();
        StringBuilder fb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            fb.append("-");
        }
        StringBuilder mess = new StringBuilder();
        mess.append("|HEAD|  ").append("|       Length      |  ").append("|Type|  ").append("|Flag|  ");
        mess.append("|               Timestamp               |  ");

        mess.append("|    MessageType    |  ");
        mess.append("|       CRC32       |  ").append("|TAIL|");

        StringBuilder headBodyTail = new StringBuilder();
        headBodyTail.append("|HEAD|  |                                                         ");
        headBodyTail.append("BODY                                                               ");
        headBodyTail.append("|  |TAIL|");
        logger.info("{}: \n\n{}\n{}\n{}\n{}\n{}\n{}\n{}\n", messageType, fb.toString(), headBodyTail.toString(),
                fb.toString(), mess.toString(), fb.toString(), sb.toString(), fb.toString());

    }

    /**
     * 消息响应日志打印字符串构建
     * shiftRightCount : 增加的字节数
     *
     * @return
     */
    private static String strForPrintMessage(int shiftRightCount) {
        StringBuilder mess2 = new StringBuilder();
        mess2.append("|HEAD|  ");
        mess2.append("|                                                                                  ");
        while (shiftRightCount > 0) {
            mess2.append("     ");
            shiftRightCount--;
        }
        mess2.append("BODY");
        mess2.append("                                                                                               ");
        mess2.append("   |  |TAIL|");
        return mess2.toString();
    }

    /**
     * 打印主动上报代理设备通信状态
     *
     * @param bytes
     */
    public static void printProxyDeviceCommActiveReport(byte[] bytes) {
        String messageType = "[Proxy Device Communication Status Active Report] 代理设备通信状态主动上报报文";
        if (bytes == null) {
            logger.error("bytes为空，请确认bytes是否准确, 消息类型：{}", messageType);
            return;
        }
        StringBuilder sb = new StringBuilder(64);

        int timeCount = 0;
        int crc32Count = 0;
        // 表示有转义字符
        if (bytes.length > 25) {
            for (int i = 0; i < bytes.length; i++) {
                if ((bytes[i] & 0xFF) == 0xFD) {
                    if (i <= 19) {
                        // 时间戳有转义字符
                        timeCount++;
                    } else {
                        // CRC32有转义字符
                        crc32Count++;
                    }
                }
            }
        }

        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            if (i == 0) {
                sb.append("|").append("0x").append(hex.toUpperCase()).append("|  |");
            } else if (i == 4 || i == 5 || i == 6 || i == 14 + timeCount
                    || i == 19 + timeCount || i == 23 + timeCount + crc32Count) {
                sb.append("0x").append(hex.toUpperCase()).append("|  |");
                if (i == 6) {
                    for (int j = 0; j < 8 - timeCount; j++) {
                        sb.append("    ");
                        sb.append(" ");
                    }
                } else if (i == 19 + timeCount) {
                    for (int j = 0; j < 4 - crc32Count; j++) {
                        sb.append("    ");
                        sb.append(" ");
                    }
                }
            } else if (i == bytes.length - 1) {
                sb.append("0x").append(hex.toUpperCase()).append("|");
            } else {
                sb.append("0x").append(hex.toUpperCase()).append(" ");
            }
        }

        int length = sb.length();
        StringBuilder fb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            fb.append("-");
        }

        StringBuilder mess = new StringBuilder();
        mess.append("|HEAD|  ").append("|       Length      |  ").append("|Type|  ").append("|Flag|  ");
        mess.append("|                                   Timestamp                                   |  ");
        mess.append("|       MessageType      |  ");
        mess.append("|                 CRC32                 |  ");
        mess.append("|TAIL|");

        StringBuilder head = new StringBuilder();
        head.append("|HEAD|  |                                                                             ");
        head.append("                              BODY                                                    ");
        head.append("                          |  |TAIL|");

        logger.info("{}: \n\n{}\n{}\n{}\n{}\n{}\n{}\n{}\n", messageType, fb.toString(), head.toString(),
                fb.toString(), mess.toString(), fb.toString(), sb.toString(), fb.toString());

    }

    /**
     * 设备状态主动上传
     *
     * @param bytes
     */
    public static void printDeviceStatusActiveReport(byte[] bytes) {
        String messageType = "[Proxy Device Status Active Report] 设备状态主动上报报文";
        StringBuilder sb = new StringBuilder(64);

        int timeCount = 0;
        int crc32Count = 0;
        // 表示有转义字符
        if (bytes.length > 29) {
            for (int i = 0; i < bytes.length; i++) {
                if ((bytes[i] & 0xFF) == 0xFD) {
                    if (i <= 23) {
                        // 时间戳有转义字符
                        timeCount++;
                    } else {
                        // CRC32有转义字符
                        crc32Count++;
                    }
                }
            }
        }

        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            if (i == 0) {
                sb.append("|").append("0x").append(hex.toUpperCase()).append("|  |");
            } else if (i == 4 || i == 5 || i == 6 || i == 14 + timeCount ||
                    i == 23 + timeCount || i == 27 + timeCount + crc32Count) {
                sb.append("0x").append(hex.toUpperCase()).append("|  |");
                if (i == 6) {
                    for (int j = 0; j < 8 - timeCount; j++) {
                        sb.append("    ");
                        sb.append(" ");
                    }
                } else if (i == 23 + timeCount) {
                    for (int j = 0; j < 4 - crc32Count; j++) {
                        sb.append("    ");
                        sb.append(" ");
                    }
                }
            } else if (i == bytes.length - 1) {
                sb.append("0x").append(hex.toUpperCase()).append("|");
            } else {
                sb.append("0x").append(hex.toUpperCase()).append(" ");
            }
        }

        int length = sb.length();
        StringBuilder fb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            fb.append("-");
        }


        StringBuilder mess = new StringBuilder();
        mess.append("|HEAD|  ").append("|       Length      |  ").append("|Type|  ").append("|Flag|  ");
        mess.append("|                                   Timestamp                                   |  ");
        mess.append("|                 MessageType                |  ");
        mess.append("|                 CRC32                 |  ");
        mess.append("|TAIL|");

        StringBuilder head = new StringBuilder();
        head.append("|HEAD|  ").append("|                                                         ").append("BODY");
        head.append("                                                                    |  |TAIL|");

        logger.info("{}: \n\n{}\n{}\n{}\n{}\n{}\n{}\n{}\n", messageType, fb.toString(), strForPrintMessage(5),
                fb.toString(), mess.toString(), fb.toString(), sb.toString(), fb.toString());
    }

    public static void logSyncCommunication(List<byte[]> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        byte[] bytes = list.get(0);
        byte[] crc32Bytes = list.get(list.size() - 1);
        if (bytes.length != 21) {
            return;
        }
        byte[] newBytes = new byte[26];
        System.arraycopy(bytes, 0, newBytes, 0, 21);
        System.arraycopy(crc32Bytes, 0, newBytes, 21, 4);
        newBytes[25] = (byte) 0xFE;
        // 转义后的数组
        byte[] escapeByteArray = ByteUtil.escapeByteArray(newBytes);

        StringBuilder sb = new StringBuilder(64);

        int timeCount = 0;
        int crc32Count = 0;
        // 表示有转义字符
        for (int i = 0; i < escapeByteArray.length; i++) {
            if ((escapeByteArray[i] & 0xFF) == 0xFD) {
                if (i <= 23) {
                    // 时间戳有转义字符
                    timeCount++;
                } else {
                    // CRC32有转义字符
                    crc32Count++;
                }
            }
        }

        for (int i = 0; i < escapeByteArray.length; i++) {
            String hex = Integer.toHexString(escapeByteArray[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            if (i == 0) {
                sb.append("|").append("0x").append(hex.toUpperCase()).append("|  |");
            } else if (i == 4 || i == 5 || i == 6 || i == 14 + timeCount ||
                    i == 20 + timeCount || i == 27 + timeCount + crc32Count) {
                sb.append("0x").append(hex.toUpperCase()).append("|  |");
                if (i == 6) {
                    for (int j = 0; j < 8 - timeCount; j++) {
                        sb.append("    ");
                        sb.append(" ");
                    }
                } else if (i == 23 + timeCount) {
                    for (int j = 0; j < 4 - crc32Count; j++) {
                        sb.append("    ");
                        sb.append(" ");
                    }
                }
            } else if (i == bytes.length - 1) {
                sb.append("0x").append(hex.toUpperCase()).append("|");
            } else {
                sb.append("0x").append(hex.toUpperCase()).append(" ");
            }
        }

        int length = sb.length();
        StringBuilder fb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            fb.append("-");
        }


        StringBuilder mess = new StringBuilder();
        mess.append("|HEAD|  ").append("|       Length      |  ").append("|Type|  ").append("|Flag|  ");
        mess.append("|                                   Timestamp                                   |  ");
        mess.append("|                 MessageType                |  ");
        mess.append("|                 CRC32                 |  ");
        mess.append("|TAIL|");

        StringBuilder head = new StringBuilder();
        head.append("|HEAD|  ").append("|                                                         ").append("BODY");
        head.append("                                                                    |  |TAIL|");

        logger.info("{}: \n\n{}\n{}\n{}\n{}\n{}\n{}\n{}\n", "同步设备状态", fb.toString(), strForPrintMessage(5),
                fb.toString(), mess.toString(), fb.toString(), sb.toString(), fb.toString());

    }
}
