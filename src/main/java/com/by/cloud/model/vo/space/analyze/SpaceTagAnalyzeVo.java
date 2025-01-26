package com.by.cloud.model.vo.space.analyze;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class SpaceTagAnalyzeVo implements Serializable {

    @ApiModelProperty("标签名称")
    private String tag;

    @ApiModelProperty("使用次数")
    private Long count;

    @Serial
    private static final long serialVersionUID = 1L;
}
