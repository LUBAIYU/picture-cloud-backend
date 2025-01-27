package com.by.cloud.model.dto.space.analyze;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author lzh
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceRankAnalyzeDto extends SpaceAnalyzeDto {

    @ApiModelProperty("排名前 N 的空间")
    private Integer topN = 10;

    @Serial
    private static final long serialVersionUID = 1L;
}
