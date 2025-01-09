package com.by.cloud.model.dto.space;

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
public class SpacePageDto extends PageRequest implements Serializable {

    @ApiModelProperty("创建用户 id")
    private Long userId;

    @ApiModelProperty("空间名称")
    private String spaceName;

    @ApiModelProperty("空间级别：0-普通版 1-专业版 2-旗舰版")
    private Integer spaceLevel;
}
