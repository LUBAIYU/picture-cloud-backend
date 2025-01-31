package com.by.cloud.model.dto.space;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class SpaceCreateDto implements Serializable {

    @ApiModelProperty("空间名称")
    private String spaceName;

    @ApiModelProperty("空间级别：0-普通版 1-专业版 2-旗舰版")
    private Integer spaceLevel;

    @ApiModelProperty("空间类型：0-私有 1-团队")
    private Integer spaceType;

}
