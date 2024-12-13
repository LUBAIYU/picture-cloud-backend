package com.by.cloud.common;

import com.by.cloud.enums.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * @param <T>
 * @author lzh
 */
@Data
public class BaseResponse<T> implements Serializable {

    /**
     * 响应码
     */
    private int code;

    /**
     * 数据
     */
    private T data;

    /**
     * 错误信息
     */
    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}

