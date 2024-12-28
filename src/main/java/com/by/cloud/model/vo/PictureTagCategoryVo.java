package com.by.cloud.model.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author lzh
 */
@Data
public class PictureTagCategoryVo implements Serializable {

    @ApiModelProperty("标签列表")
    private List<String> tagList;

    @ApiModelProperty("分类列表")
    private List<String> categoryList;
}
