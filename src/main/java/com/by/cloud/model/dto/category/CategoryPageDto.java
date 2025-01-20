package com.by.cloud.model.dto.category;

import com.by.cloud.common.PageRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author lzh
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CategoryPageDto extends PageRequest implements Serializable {

    @ApiModelProperty("分类名称")
    private String name;
}
