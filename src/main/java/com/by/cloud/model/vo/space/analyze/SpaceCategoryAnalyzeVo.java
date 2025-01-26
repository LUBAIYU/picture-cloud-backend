package com.by.cloud.model.vo.space.analyze;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class SpaceCategoryAnalyzeVo implements Serializable {

    @ApiModelProperty("图片分类")
    private String category;

    @ApiModelProperty("图片数量")
    private Long count;

    @ApiModelProperty("分类图片总大小")
    private Long totalSize;

    @Serial
    private static final long serialVersionUID = 1L;
}
