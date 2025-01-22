package com.by.cloud.utils;

/**
 * 颜色转换工具类
 *
 * @author lzh
 */
public class ColorTransferUtils {

    private ColorTransferUtils() {
    }

    /**
     * 获取16进制标准颜色
     *
     * @param hexColor 颜色字符串
     * @return 标准颜色字符串
     */
    public static String getStandardColor(String hexColor) {
        // 如果是7位，则在2位后补一个0，补齐8位标准值
        // 示例：0x080e0 => 0x0080e0
        int len = 7;
        if (hexColor.length() == len) {
            hexColor = hexColor.substring(0, 2) + "0" + hexColor.substring(2, 7);
        }
        return hexColor;
    }
}
