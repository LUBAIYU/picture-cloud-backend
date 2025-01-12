package com.by.cloud.model.vo.space;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
@AllArgsConstructor
public class SpaceLevelVo implements Serializable {

    @ApiModelProperty("空间级别")
    private String text;

    @ApiModelProperty("索引值")
    private int value;

    @ApiModelProperty("最大条数")
    private long maxCount;

    @ApiModelProperty("最大容量")
    private long maxSize;
}
