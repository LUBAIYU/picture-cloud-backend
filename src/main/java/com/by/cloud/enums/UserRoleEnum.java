package com.by.cloud.enums;

import lombok.Getter;

/**
 * @author lzh
 */
@Getter
public enum UserRoleEnum {

    /**
     * 角色枚举
     */
    ADMIN(0, "管理员"),
    USER(1, "普通用户");

    private final Integer value;

    private final String text;

    UserRoleEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据值获得枚举对象
     *
     * @param value 值
     * @return 枚举对象
     */
    public static UserRoleEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (UserRoleEnum roleEnum : values()) {
            if (roleEnum.value.equals(value)) {
                return roleEnum;
            }
        }
        return null;
    }
}
