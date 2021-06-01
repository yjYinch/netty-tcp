package com.sedwt.alarm.client.utils;

import com.sedwt.alarm.client.entity.PandaMessageBodyEnum;
import com.sedwt.alarm.client.entity.PandaResultEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * @author : yj zhang
 * @since : 2021/5/8 14:20
 */

public class ByteUtil {
    private static final Logger logger = LoggerFactory.getLogger("byteUtil");

    private ByteUtil() {
    }

    /**
     * 时间戳转换为字节数组
     * <p>
     * 示例：1620455072374
     * 2进制：10111100101001010101001100010001001110110
     * 16进制byte: 1794AA62276
     * <p>
     * 注意该方法返回的字节数组 最低位到最高位是从数组的0-7顺序排列的
     * 也就是：低位在前，高位在后
     * byte:   [0x76, 0x22, 0xA6, 0x4A, 0x79, 0x01, 0x00, 0x00]
     * <p>
     * 如果需要高位在前，低位在后，调用方法 {@link ByteUtil#convertByte(byte[])}
     *
     * @param timestamp 时间戳
     * @param flag      true: 高位在前，低位在后
     * @return byte数组
     */
    public static byte[] timestampToByte(long timestamp, boolean flag) {
        byte[] timestampBytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            timestampBytes[i] = (byte) ((timestamp >> i * 8) & 0xFF);
        }
        if (flag) {
            return convertByte(timestampBytes);
        }
        return timestampBytes;
    }

    /**
     * 判断字节数组里的数据是否需要转义，如果需要转义就将其转义
     *
     * @param bytes
     * @return
     */
    public static byte[] escapeByteArray(byte[] bytes) {
        int len = bytes.length;
        List<Byte> list = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            // 首尾不转义
            if (i == 0 || i == len - 1) {
                list.add(bytes[i]);
                continue;
            }
            if (isNeededToConvert(bytes[i])) {
                list.add((byte) 0xFD);
                list.add((byte) (bytes[i] - 128));
            } else {
                list.add(bytes[i]);
            }
        }
        int size = list.size();
        byte[] escapedByteArray = new byte[size];
        for (int i = 0; i < size; i++) {
            escapedByteArray[i] = list.get(i);
        }
        return escapedByteArray;
    }

    /**
     * 调换数组的顺序 [1,2,5,3] ==> [3,5,2,1]
     *
     * @param bytes
     * @return
     */
    public static byte[] convertByte(byte[] bytes) {
        int length = bytes.length;
        if (length == 0) {
            return new byte[length];
        }
        byte[] newByte = new byte[length];
        for (int i = 0; i < length; i++) {
            newByte[length - 1 - i] = bytes[i];
        }
        return newByte;
    }

    /**
     * 特殊字符转义，因帧头帧尾占用0xFF、0xFE，编解码时需要对帧体的数据进行转义处理，使用0xFD进行转义
     * --------------------------
     * |  转义前    |  转义后     |
     * -------------------------
     * |  0xFF    |  0xFD 0x7F |
     * -------------------------
     * |  0xFE    |  0xFD 0x7E |
     * -------------------------
     * |  0xFD    |  0xFD 0x7D |
     * -------------------------
     *
     * @param b 待转义字符
     * @return
     */
    public static byte[] specialByteConvert(byte b) {
        byte[] convertedByte = new byte[2];
        if (b == PandaMessageBodyEnum.FRAME_HEAD.getVal()) {
            convertedByte[0] = PandaMessageBodyEnum.FRAME_HEAD.getVal();
            convertedByte[1] = 0x7F;
        } else if (b == PandaMessageBodyEnum.FRAME_TAIL.getVal()) {
            convertedByte[0] = PandaMessageBodyEnum.FRAME_TAIL.getVal();
            convertedByte[1] = 0x7E;
        } else if (b == PandaMessageBodyEnum.ESCAPE_CHARACTER.getVal()) {
            convertedByte[0] = PandaMessageBodyEnum.ESCAPE_CHARACTER.getVal();
            convertedByte[1] = 0x7D;
        } else {
            convertedByte[0] = b;
        }

        return convertedByte;
    }

    /**
     * 判断字节数是否是需要转义的字符
     *
     * @param b 字节
     * @return
     */
    public static boolean isNeededToConvert(byte b) {
        return b == PandaMessageBodyEnum.FRAME_HEAD.getVal()
                || b == PandaMessageBodyEnum.FRAME_TAIL.getVal() || b == PandaMessageBodyEnum.ESCAPE_CHARACTER.getVal();
    }

    /**
     * 将字节数组中的值进行CRC32转义。
     *
     * @param bytes
     * @param offset
     * @param length
     * @return
     */
    public static byte[] crc32Convert(byte[] bytes, int offset, int length) throws Exception {
        try {
            CRC32 crc32 = new CRC32();
            crc32.reset();
            crc32.update(bytes, offset, length);
            long value = crc32.getValue();
            return crc32ToByteArray(value);
        } catch (Exception e) {
            logger.error("crc32转义异常");
            throw new Exception("校验码进行crc32转义错误");
        }
    }

    /**
     * crc32校验码生成字节数组
     *
     * @param value
     * @return
     */
    public static byte[] crc32ToByteArray(long value) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) ((value >> (3 - i) * 8) & 0xFF);
        }
        return bytes;
    }

    /**
     * 控制台打印字节数组，以16进制格式输出
     *
     * @param bytes
     */
    public static void printInConsole(byte[] bytes) {
        logger.info("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append("0x").append(hex.toUpperCase()).append(" ");
        }
        logger.info(sb.toString());
    }

    public static StringBuilder bytesToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            if (i == bytes.length - 1) {
                sb.append("0x").append(hex.toUpperCase());
            } else {
                sb.append("0x").append(hex.toUpperCase()).append(" ");
            }
        }
        return sb;
    }

    /**
     * 获取转义前的报文数据
     *
     * @param bytes 经过转义的报文数据
     * @return 转义前的报文数据
     */
    public static byte[] getBeforeEscapedBytes(byte[] bytes) {
        int l = bytes.length;
        List<Byte> list = new ArrayList<>();
        for (int i = 0; i < l; i++) {
            if (bytes[i] == (byte) 0xFD && bytes[i + 1] == (byte) 0x7D) {
                list.add((byte) 0xFD);
            } else if (bytes[i] == (byte) 0xFD && bytes[i + 1] == (byte) 0x7E) {
                list.add((byte) 0xFE);
            } else if (bytes[i] == (byte) 0xFD && bytes[i + 1] == (byte) 0x7F) {
                list.add((byte) 0xFF);
            } else if (((bytes[i] == (byte) 0x7D || bytes[i] == (byte) 0x7E || bytes[i] == (byte) 0x7F)
                    && (bytes[i - 1] == (byte) 0xFD))) {
                logger.debug("this is a useless byte");
            } else {
                list.add(bytes[i]);
            }
        }
        byte[] byteArray = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            byteArray[i] = list.get(i);
        }
        return byteArray;
    }

    /**
     * 格式化报文数据
     *
     * @param bytes 源报文字节数组
     * @return 格式化后的报文字符串
     */
    public static StringBuilder printBytesToConsole(byte[] bytes) {
        byte[] array = getBeforeEscapedBytes(bytes);
        StringBuilder sb = new StringBuilder();
        Map<String, byte[]> map = new HashMap<>();
        //(1)帧头
        map.put("head", new byte[]{array[0]});
        //(2)消息长度
        byte[] messageLength = new byte[4];
        System.arraycopy(array, 1, messageLength, 0, 4);
        map.put("length", messageLength);
        //(3)报文类型
        map.put("type", new byte[]{array[5]});
        //(4)消息类型
        map.put("flag", new byte[]{array[6]});
        //(5)时间戳
        byte[] timestamp = new byte[8];
        System.arraycopy(array, 7, timestamp, 0, 8);
        map.put("timestamp", timestamp);
        //(6)消息体
        int msgLength = array.length - 20;
        byte[] message = new byte[msgLength];
        System.arraycopy(array, 15, message, 0, msgLength);
        map.put("message", message);
        //(7)CRC32
        byte[] crc32 = new byte[4];
        System.arraycopy(array, 15 + msgLength, crc32, 0, 4);
        map.put("crc32", crc32);
        //(8)帧尾
        map.put("tail", new byte[]{array[19 + msgLength]});

        int length = 142;
        StringBuilder fb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            fb.append("-");
        }

        StringBuilder mess = new StringBuilder();
        mess.append("|HEAD|  ").append("|       Length      |  ").append("|Type|  ").append("|Flag|  ")
                .append("|               Timestamp               |  ").append("|    MessageType    |  ")
                .append("|       CRC32       |  ").append("|TAIL|");

        StringBuilder headBodyTail = new StringBuilder();
        headBodyTail.append("|HEAD|  |                                                         ");
        headBodyTail.append("BODY                                                               ");
        headBodyTail.append("|  |TAIL|");

        sb.append("|").append(bytesToString(map.get("head")))
                .append("|  |").append(bytesToString(map.get("length")))
                .append("|  |").append(bytesToString(map.get("type")))
                .append("|  |").append(bytesToString(map.get("flag")))
                .append("|  |").append(bytesToString(map.get("timestamp")));
        int mLength = map.get("message").length;
        if (mLength == 1) {
            sb.append("|  |        ").append(bytesToString(map.get("message"))).append("       ");
        } else if (mLength == 4) {
            sb.append("|  |").append(bytesToString(map.get("message")));
        } else {
            sb.append("|  |").append("      message      ");
        }
        sb.append("|  |").append(bytesToString(map.get("crc32")))
                .append("|  |").append(bytesToString(map.get("tail"))).append("|");

        StringBuilder log = new StringBuilder();
        log.append("\n").append(fb).append("\n").append(headBodyTail).append("\n").append(fb).append("\n")
                .append(mess).append("\n").append(fb).append("\n").append(sb).append("\n").append(fb).append("\n");
        if (mLength > 4) {
            log.append("message = [").append(bytesToString(map.get("message"))).append("]\n");
        }
        return log;
    }

    public static byte[] getResponseBytes(byte messageType, byte result) throws Exception {
        byte[] bytes = new byte[21];
        // 1.固定帧头
        bytes[0] = PandaMessageBodyEnum.FRAME_HEAD.getVal();
        // 2.帧体
        // 2.1 长度 4byte
        bytes[1] = (byte) 0x00;
        bytes[2] = (byte) 0x00;
        bytes[3] = (byte) 0x00;
        bytes[4] = (byte) 0x0F;

        // 2.2 消息头 10byte
        // 2.2.1 报文类型 1byte
        bytes[5] = (byte) 0x01;
        // 2.2.2 消息类型 1byte
        bytes[6] = messageType;
        // 2.2.3 时间戳 8byte
        byte[] timestampToByte = ByteUtil.timestampToByte(System.currentTimeMillis(), true);
        // 将timestampToByte拷贝到bytes中
        System.arraycopy(timestampToByte, 0, bytes, 7, timestampToByte.length);

        bytes[15] = result;

        byte[] bytesFromCrc32 = ByteUtil.crc32Convert(bytes, 1, 15);
        bytes[16] = bytesFromCrc32[0];
        bytes[17] = bytesFromCrc32[1];
        bytes[18] = bytesFromCrc32[2];
        bytes[19] = bytesFromCrc32[3];

        // 3.固定帧尾 1byte
        bytes[20] = PandaMessageBodyEnum.FRAME_TAIL.getVal();
        return ByteUtil.escapeByteArray(bytes);
    }

    /**
     * 判断报文消息类型
     *
     * @param bytes       解码后的报文字节数组
     * @param messageType 目标消息类型
     * @return 报文消息类型和目标消息类型是否相同
     */
    public static boolean isSameMessageType(byte[] bytes, byte messageType) {
        return bytes[6] == messageType;
    }

    /**
     * 判断报文类型
     *
     * @param bytes      解码后的报文字节数组
     * @param packetType 目标报文类型
     * @return 报文类型和目标类型是否相同
     */
    public static boolean isSamePacketType(byte[] bytes, byte packetType) {
        if (((bytes[0] & PandaMessageBodyEnum.HEX_SIGNAL.getVal()) == PandaMessageBodyEnum.FRAME_HEAD.getVal())
                && (bytes[bytes.length - 1] == PandaMessageBodyEnum.FRAME_TAIL.getVal())) {
            return bytes[5] == packetType;
        }
        return false;
    }

    /**
     * 响应结果是否成功
     *
     * @param bytes 解码后的报文字节数组
     * @return 响应码是否等于成功
     */
    public static boolean isSuccessResponse(byte[] bytes) {
        return bytes[15] == PandaResultEnum.SUCCESS.getVal();
    }
}
