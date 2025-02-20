package com.by.cloud.model.dto.comment;

import com.by.cloud.common.PageRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论列表分页查询实体
 *
 * @author lzh
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CommentPageDto extends PageRequest {

    @ApiModelProperty("评论内容")
    private String content;

    @ApiModelProperty("图片ID")
    private Long picId;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("0-待审核，1-通过，2-拒绝")
    private Integer status;
}
