package com.by.cloud.model.vo.space.analyze;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author lzh
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceSizeAnalyzeVo implements Serializable {

    @ApiModelProperty("图片大小范围")
    private String sizeRange;

    @ApiModelProperty("图片数量")
    private Long count;

    @Serial
    private static final long serialVersionUID = 1L;
}
