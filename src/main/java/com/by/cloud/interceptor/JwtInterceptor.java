package com.by.cloud.interceptor;

import cn.hutool.core.util.StrUtil;
import com.by.cloud.common.BaseContext;
import com.by.cloud.common.JwtProperties;
import com.by.cloud.constants.UserConstant;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT令牌拦截器
 *
 * @author lzh
 */
@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    @Resource
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果请求是静态资源请求，则不拦截
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 从请求头获取令牌
        String token = request.getHeader(jwtProperties.getTokenName());
        if (StrUtil.isBlank(token)) {
            // 响应失败
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        log.info("令牌 Token = {}", token);

        // 校验令牌
        try {
            Claims claims = JwtUtils.parseJwt(jwtProperties.getSecretKey(), token);
            long userId = Long.parseLong(claims.get(UserConstant.USER_ID).toString());
            log.info("登录用户ID = {}", userId);
            // 保存用户ID到线程本地变量
            BaseContext.setLoginUserId(userId);
            // 放行
            return true;
        } catch (Exception e) {
            // 响应失败
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 释放线程本地变量资源
        if (BaseContext.getLoginUserId() != null) {
            BaseContext.removeLoginUserId();
        }
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
