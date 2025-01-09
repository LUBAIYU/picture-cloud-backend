package com.by.cloud.enums;

import com.by.cloud.constants.SpaceConstant;
import lombok.Getter;

/**
 * @author lzh
 */
@Getter
public enum SpaceLevelEnum {

    /**
     * 空间级别枚举
     */
    COMMON("普通版", 0, SpaceConstant.COMMON_SPACE_MAX_COUNT, SpaceConstant.COMMON_SPACE_MAX_SIZE),
    PROFESSIONAL("专业版", 1, SpaceConstant.PROFESSIONAL_SPACE_MAX_COUNT, SpaceConstant.PROFESSIONAL_SPACE_MAX_SIZE),
    FLAGSHIP("旗舰版", 2, SpaceConstant.FLAGSHIP_SPACE_MAX_COUNT, SpaceConstant.FLAGSHIP_SPACE_MAX_SIZE);

    private final String text;

    private final int value;

    private final long maxCount;

    private final long maxSize;

    SpaceLevelEnum(String text, int value, long maxCount, long maxSize) {
        this.text = text;
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    /**
     * 根据值获得枚举对象
     *
     * @param value 值
     * @return 枚举对象
     */
    public static SpaceLevelEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (SpaceLevelEnum spaceLevelEnum : values()) {
            if (spaceLevelEnum.value == value) {
                return spaceLevelEnum;
            }
        }
        return null;
    }
}
