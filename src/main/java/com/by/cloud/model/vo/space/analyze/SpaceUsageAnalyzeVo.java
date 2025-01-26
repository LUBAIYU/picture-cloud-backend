package com.by.cloud.model.vo.space.analyze;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 空间使用情况分析返回类
 *
 * @author lzh
 */
@Data
public class SpaceUsageAnalyzeVo implements Serializable {

    @ApiModelProperty("已使用大小")
    private Long usedSize;

    @ApiModelProperty("总大小")
    private Long maxSize;

    @ApiModelProperty("空间使用比例")
    private Double sizeUsageRatio;

    @ApiModelProperty("当前图片数量")
    private Long usedCount;

    @ApiModelProperty("最大图片数量")
    private Long maxCount;

    @ApiModelProperty("图片数量占比")
    private Double countUsageRatio;

    @Serial
    private static final long serialVersionUID = 1L;
}
