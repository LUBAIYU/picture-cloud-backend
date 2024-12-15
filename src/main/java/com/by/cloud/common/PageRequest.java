package com.by.cloud.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author lzh
 */
@Data
public class PageRequest {

    @ApiModelProperty("当前页码")
    private int current = 1;

    @ApiModelProperty("页面大小")
    private int pageSize = 10;
}
