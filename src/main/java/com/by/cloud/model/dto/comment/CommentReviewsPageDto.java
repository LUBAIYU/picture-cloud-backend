package com.by.cloud.model.dto.comment;

import com.by.cloud.common.PageRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author lzh
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CommentReviewsPageDto extends PageRequest implements Serializable {

    @ApiModelProperty("评论ID")
    private Long commentId;

    @ApiModelProperty("审核员ID")
    private Long reviewerId;

    @ApiModelProperty("审核状态：1-通过；2-拒绝")
    private Integer reviewStatus;

    @ApiModelProperty("审核信息")
    private String reviewMsg;
}
