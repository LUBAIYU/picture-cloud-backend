package com.by.cloud.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装类
 *
 * @author lzh
 */
@Data
@AllArgsConstructor(staticName = "of")
public class PageResult<T> implements Serializable {

    @ApiModelProperty("总记录数")
    private Long total;

    @ApiModelProperty("记录数据")
    private List<T> records;
}
