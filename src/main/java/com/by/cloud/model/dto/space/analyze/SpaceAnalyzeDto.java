package com.by.cloud.model.dto.space.analyze;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 空间分析数据传输对象
 *
 * @author lzh
 */
@Data
public class SpaceAnalyzeDto implements Serializable {

    @ApiModelProperty("空间ID")
    private Long spaceId;

    @ApiModelProperty("是否查询公共图库")
    private boolean queryPublic;

    @ApiModelProperty("全空间分析")
    private boolean queryAll;
}
