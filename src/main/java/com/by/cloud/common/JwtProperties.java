package com.by.cloud.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT属性配置
 *
 * @author lzh
 */
@Component
@ConfigurationProperties(prefix = "user.jwt")
@Data
public class JwtProperties {

    /**
     * 密钥
     */
    private String secretKey;

    /**
     * 过期时间
     */
    private Long ttl;

    /**
     * 令牌名称
     */
    private String tokenName;
}
