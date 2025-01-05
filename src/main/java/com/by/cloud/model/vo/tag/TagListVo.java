package com.by.cloud.model.vo.tag;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class TagListVo implements Serializable {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("标签名称")
    private String name;
}
