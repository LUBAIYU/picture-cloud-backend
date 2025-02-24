package com.by.cloud.model.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class VipExchangeDto implements Serializable {

    @ApiModelProperty("兑换码")
    private String vipCode;
}
