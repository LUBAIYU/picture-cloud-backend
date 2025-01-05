package com.by.cloud.model.dto.picture;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author lzh
 */
@Data
public class PictureUpdateDto implements Serializable {

    @ApiModelProperty("图片ID")
    private Long picId;

    @ApiModelProperty("图片名称")
    private String picName;

    @ApiModelProperty("简介")
    private String introduction;

    @ApiModelProperty("分类ID")
    private Long categoryId;

    @ApiModelProperty("标签ID列表")
    private List<Long> tagIdList;
}
