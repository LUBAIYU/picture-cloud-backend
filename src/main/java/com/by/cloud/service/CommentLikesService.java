package com.by.cloud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.common.PageResult;
import com.by.cloud.model.dto.comment.CommentLikesPageDto;
import com.by.cloud.model.entity.CommentLikes;

/**
 * @author lzh
 */
public interface CommentLikesService extends IService<CommentLikes> {

    /**
     * 分页查询评论点赞记录
     *
     * @param pageDto 查询参数
     * @return 分页数据
     */
    PageResult<CommentLikes> queryCommentLikesByPage(CommentLikesPageDto pageDto);
}
