package com.by.cloud.model.dto.picture;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class PictureReviewDto implements Serializable {

    @ApiModelProperty("ID")
    private Long id;

    @ApiModelProperty("审核状态：0-待审核；1-通过；2-拒绝")
    private Integer reviewStatus;

    @ApiModelProperty("审核信息")
    private String reviewMessage;
}
