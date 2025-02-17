package com.by.cloud.model.vo.comment;

import com.by.cloud.model.vo.user.UserVo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 图片树形评论返回类
 *
 * @author lzh
 */
@Data
public class CommentsViewVo implements Serializable {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("评论内容")
    private String content;

    @ApiModelProperty("图片ID")
    private Long picId;

    @ApiModelProperty("父级评论ID")
    private Long parentId;

    @ApiModelProperty("评论用户")
    private UserVo user;

    @ApiModelProperty("点赞数")
    private Integer likeCount;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("子级评论")
    private List<CommentsViewVo> children;

    @Serial
    private static final long serialVersionUID = 1L;
}
