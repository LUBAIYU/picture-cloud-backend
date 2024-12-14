package com.by.cloud.model.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class UserLoginVo implements Serializable {

    @ApiModelProperty("登录令牌")
    private String token;
}
