package com.by.cloud.model.dto.picture;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片批量编辑请求
 *
 * @author lzh
 */
@Data
public class PictureEditByBatchDto implements Serializable {

    @ApiModelProperty("图片ID列表")
    private List<Long> pictureIdList;

    @ApiModelProperty("空间ID")
    private Long spaceId;

    @ApiModelProperty("分类ID")
    private Long categoryId;

    @ApiModelProperty("标签ID列表")
    private List<Long> tagIdList;
}
