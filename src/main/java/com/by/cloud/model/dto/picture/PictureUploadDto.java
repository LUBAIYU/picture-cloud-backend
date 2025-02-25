package com.by.cloud.model.dto.picture;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class PictureUploadDto implements Serializable {

    @ApiModelProperty("图片ID")
    private Long picId;

    @ApiModelProperty("图片URL")
    private String fileUrl;

    @ApiModelProperty("图片名称")
    private String picName;

    @ApiModelProperty("空间ID")
    private Long spaceId;
}
