package com.by.cloud.model.dto.picture;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class PictureSearchByPictureDto implements Serializable {

    @ApiModelProperty("图片ID")
    private Long pictureId;
}
