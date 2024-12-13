package com.by.cloud.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 *
 * @author lzh
 */
public class JwtUtils {

    /**
     * 生成JWT令牌
     *
     * @param secretKey 密钥
     * @param ttlMillis 过期时间
     * @param claims    令牌存储的内容
     * @return JWT令牌
     */
    public static String createJwt(String secretKey, long ttlMillis, Map<String, Object> claims) {
        // 设置Header签名算法，使用HS256算法
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        //设置JWT过期时间，已毫秒为单位
        long expMills = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMills);

        //设置JWT的body
        JwtBuilder builder = Jwts.builder()
                // 设置自定义内容
                .setClaims(claims)
                // 设置签名使用的签名算法和签名使用的秘钥
                .signWith(signatureAlgorithm, secretKey.getBytes(StandardCharsets.UTF_8))
                // 设置过期时间
                .setExpiration(exp);
        return builder.compact();
    }

    /**
     * Token解密
     *
     * @param secretKey JWT秘钥
     * @param token     加密后的token
     * @return 自定义内容
     */
    public static Claims parseJwt(String secretKey, String token) {
        return Jwts.parser()
                //设置签名秘钥
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                //设置需要解析的token
                .parseClaimsJws(token)
                .getBody();
    }
}
