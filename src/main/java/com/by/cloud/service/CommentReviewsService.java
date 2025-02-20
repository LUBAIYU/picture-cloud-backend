package com.by.cloud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.common.PageResult;
import com.by.cloud.model.dto.comment.CommentReviewsPageDto;
import com.by.cloud.model.entity.CommentReviews;

/**
 * @author lzh
 */
public interface CommentReviewsService extends IService<CommentReviews> {

    /**
     * 分页查询评论审核记录
     *
     * @param pageDto 分页参数
     * @return 审核记录
     */
    PageResult<CommentReviews> queryCommentReviewsByPage(CommentReviewsPageDto pageDto);
}
