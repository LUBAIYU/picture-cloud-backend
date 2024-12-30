package com.by.cloud.enums;

import lombok.Getter;

/**
 * @author lzh
 */
@Getter
public enum PictureReviewStatusEnum {

    /**
     * 审核状态枚举
     */
    UNREVIEWED(0, "待审核"),
    PASS(1, "通过"),
    REJECT(2, "拒绝");

    private final Integer value;

    private final String text;

    PictureReviewStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 值
     * @return 枚举值
     */
    public static PictureReviewStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (PictureReviewStatusEnum statusEnum : values()) {
            if (statusEnum.value.equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
