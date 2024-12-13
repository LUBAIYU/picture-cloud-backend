package com.by.cloud.enums;

import lombok.Getter;

/**
 * @author lzh
 */
@Getter
public enum UserStatusEnum {

    /**
     * 状态枚举
     */
    DISABLE(0, "不可用"),
    ENABLE(1, "可用");

    private final Integer value;

    private final String text;

    UserStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 值
     * @return 枚举值
     */
    public static UserStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (UserStatusEnum statusEnum : values()) {
            if (statusEnum.value.equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
