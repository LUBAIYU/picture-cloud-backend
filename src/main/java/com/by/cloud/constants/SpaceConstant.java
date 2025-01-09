package com.by.cloud.constants;

/**
 * 空间常量
 *
 * @author lzh
 */
public interface SpaceConstant {

    /**
     * 普通版空间最大数量
     */
    long COMMON_SPACE_MAX_COUNT = 100;

    /**
     * 专业版空间最大数量
     */
    long PROFESSIONAL_SPACE_MAX_COUNT = 1000;

    /**
     * 旗舰版空间最大数量
     */
    long FLAGSHIP_SPACE_MAX_COUNT = 10000;

    /**
     * 普通版空间最大容量
     */
    long COMMON_SPACE_MAX_SIZE = 100L * 1024 * 1024;

    /**
     * 专业版空间最大容量
     */
    long PROFESSIONAL_SPACE_MAX_SIZE = 1000L * 1024 * 1024;

    /**
     * 旗舰版空间最大容量
     */
    long FLAGSHIP_SPACE_MAX_SIZE = 10000L * 1024 * 1024;

    /**
     * 空间名称最大长度
     */
    int SPACE_NAME_MAX_LENGTH = 30;
}
