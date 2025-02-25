package com.by.cloud.enums;

import lombok.Getter;

/**
 * 评论审核状态枚举
 *
 * @author lzh
 */
@Getter
public enum CommentReviewStatusEnum {

    /**
     * 审核状态枚举
     */
    SUBMITTED(0, "已提交"),
    REVIEWING(1, "审核中"),
    PASS(2, "通过"),
    REJECT(3, "拒绝"),
    FAILED(4, "失败");

    private final Integer value;

    private final String text;

    CommentReviewStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 值
     * @return 枚举值
     */
    public static CommentReviewStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (CommentReviewStatusEnum statusEnum : values()) {
            if (statusEnum.value.equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
