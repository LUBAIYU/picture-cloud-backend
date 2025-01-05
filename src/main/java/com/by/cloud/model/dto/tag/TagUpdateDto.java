package com.by.cloud.model.dto.tag;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class TagUpdateDto implements Serializable {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("标签名称")
    private String name;
}
