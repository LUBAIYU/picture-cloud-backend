package com.by.cloud.model.dto.comment;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 评论审核请求
 *
 * @author lzh
 */
@Data
public class CommentReviewDto implements Serializable {

    @ApiModelProperty("评论ID")
    private Long commentId;

    @ApiModelProperty("审核状态：1-通过；2-拒绝")
    private Integer reviewStatus;

    @ApiModelProperty("审核信息")
    private String reviewMsg;
}
