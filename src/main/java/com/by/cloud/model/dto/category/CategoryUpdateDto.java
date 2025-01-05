package com.by.cloud.model.dto.category;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class CategoryUpdateDto implements Serializable {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("分类名称")
    private String name;
}
