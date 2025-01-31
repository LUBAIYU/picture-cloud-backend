package com.by.cloud.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * @author lzh
 */
@Getter
public enum SpaceRoleEnum {

    /**
     * 空间角色枚举
     */
    VIEWER("浏览者", "viewer"),
    EDITOR("编辑者", "editor"),
    ADMIN("管理员", "admin");

    private final String text;

    private final String value;

    SpaceRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据值获得枚举对象
     *
     * @param value 值
     * @return 枚举对象
     */
    public static SpaceRoleEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (SpaceRoleEnum spaceRoleEnum : values()) {
            if (spaceRoleEnum.value.equals(value)) {
                return spaceRoleEnum;
            }
        }
        return null;
    }

    /**
     * 获取所有枚举的文本列表
     *
     * @return 文本列表
     */
    public static List<String> getAllTexts() {
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getText)
                .toList();
    }

    /**
     * 获取所有枚举的值列表
     *
     * @return 值列表
     */
    public static List<String> getAllValues() {
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getValue)
                .toList();
    }
}
