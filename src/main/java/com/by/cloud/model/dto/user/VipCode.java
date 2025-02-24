package com.by.cloud.model.dto.user;

import lombok.Data;

/**
 * @author lzh
 */
@Data
public class VipCode {

    /**
     * 兑换码
     */
    private String code;

    /**
     * 是否已使用
     */
    private boolean hasUsed;
}
