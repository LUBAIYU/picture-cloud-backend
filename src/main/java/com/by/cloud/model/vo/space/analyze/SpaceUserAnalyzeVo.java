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
public class SpaceUserAnalyzeVo implements Serializable {

    @ApiModelProperty("时间区间")
    private String period;

    @ApiModelProperty("上传数量")
    private Long count;

    @Serial
    private static final long serialVersionUID = 1L;
}
