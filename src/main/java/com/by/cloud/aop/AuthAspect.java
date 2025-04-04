package com.by.cloud.aop;

import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.UserRoleEnum;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.model.entity.User;
import com.by.cloud.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author lzh
 */
@Aspect
@Component
public class AuthAspect {

    @Resource
    private UserService userService;

    @Around("@annotation(preAuthorize)")
    public Object checkAuth(ProceedingJoinPoint joinPoint, PreAuthorize preAuthorize) throws Throwable {
        // 获取请求对象
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();

        // 获取当前登录用户角色
        User loginUser = userService.getLoginUser(request);
        Integer userRole = loginUser.getUserRole();
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(userRole);

        // 如果用户角色和指定的角色不匹配，则抛异常
        UserRoleEnum mustRole = preAuthorize.role();
        if (!userRoleEnum.equals(mustRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 放行
        return joinPoint.proceed();
    }
}
