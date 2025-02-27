package com.by.cloud.model.dto.comment;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class CommentLikesDto implements Serializable {

    @ApiModelProperty("评论ID")
    private Long commentId;

    @ApiModelProperty("是否点赞")
    private Boolean isLiked;
}
