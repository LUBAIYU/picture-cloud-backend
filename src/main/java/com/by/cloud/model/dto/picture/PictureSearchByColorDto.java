package com.by.cloud.model.dto.picture;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class PictureSearchByColorDto implements Serializable {

    @ApiModelProperty("空间ID")
    private Long spaceId;

    @ApiModelProperty("图片主色调")
    private String picColor;
}
