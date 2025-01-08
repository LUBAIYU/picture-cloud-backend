package com.by.cloud.model.dto.file;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class UploadPictureResult implements Serializable {

    @ApiModelProperty("图片url")
    private String picUrl;

    @ApiModelProperty("缩略图url")
    private String thumbnailUrl;

    @ApiModelProperty("图片名称")
    private String picName;

    @ApiModelProperty("图片体积")
    private Long picSize;

    @ApiModelProperty("图片宽度")
    private Integer picWidth;

    @ApiModelProperty("图片高度")
    private Integer picHeight;

    @ApiModelProperty("图片宽高比例")
    private Double picScale;

    @ApiModelProperty("图片格式")
    private String picFormat;
}
