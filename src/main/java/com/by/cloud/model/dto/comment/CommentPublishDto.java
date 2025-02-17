package com.by.cloud.model.dto.comment;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class CommentPublishDto implements Serializable {

    @ApiModelProperty("图片ID")
    private Long picId;

    @ApiModelProperty("图片内容")
    private String content;

    @ApiModelProperty("父级评论ID")
    private Long parentId;
}
