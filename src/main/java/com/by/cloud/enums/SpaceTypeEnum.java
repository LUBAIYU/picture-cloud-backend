package com.by.cloud.enums;

import lombok.Getter;

/**
 * @author lzh
 */
@Getter
public enum SpaceTypeEnum {

    /**
     * 空间类型枚举
     */
    PRIVATE("私有空间", 0),
    TEAM("团队空间", 1);

    private final String text;

    private final int value;

    SpaceTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据值获得枚举对象
     *
     * @param value 值
     * @return 枚举对象
     */
    public static SpaceTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (SpaceTypeEnum spaceTypeEnum : values()) {
            if (spaceTypeEnum.value == value) {
                return spaceTypeEnum;
            }
        }
        return null;
    }
}
