package com.by.cloud.enums;

import lombok.Getter;

/**
 * 审核记录状态枚举
 *
 * @author lzh
 */
@Getter
public enum ReviewResultStatusEnum {

    /**
     * 审核结果状态枚举
     */
    APPROVE(0, "通过"),
    REJECT(1, "拒绝"),
    FAILED(2, "失败");

    private final Integer value;

    private final String text;

    ReviewResultStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 值
     * @return 枚举值
     */
    public static ReviewResultStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (ReviewResultStatusEnum statusEnum : values()) {
            if (statusEnum.value.equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
