package com.by.cloud.model.dto.space;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class SpaceEditDto implements Serializable {

    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("空间名称")
    private String spaceName;
}
