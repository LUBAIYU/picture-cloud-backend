package com.by.cloud.common;

/**
 * 存放线程局部变量
 *
 * @author lzh
 */
public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setLoginUserId(Long userId) {
        threadLocal.set(userId);
    }

    public static Long getLoginUserId() {
        return threadLocal.get();
    }

    public static void removeLoginUserId() {
        threadLocal.remove();
    }
}
