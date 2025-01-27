package com.by.cloud.model.dto.space.analyze;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author lzh
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUserAnalyzeDto extends SpaceAnalyzeDto {

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("时间纬度：day/week/month")
    private String timeDimension;
}
