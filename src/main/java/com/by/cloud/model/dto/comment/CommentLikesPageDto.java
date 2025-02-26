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
public class CommentLikesPageDto extends PageRequest implements Serializable {

    @ApiModelProperty("评论ID")
    private Long commentId;

    @ApiModelProperty("用户ID")
    private Long userId;
}
