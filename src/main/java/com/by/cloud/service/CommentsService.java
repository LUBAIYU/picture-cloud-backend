package com.by.cloud.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.model.dto.comment.CommentPageDto;
import com.by.cloud.model.dto.comment.CommentPublishDto;
import com.by.cloud.model.dto.comment.CommentReviewDto;
import com.by.cloud.model.entity.Comments;
import com.by.cloud.model.vo.comment.CommentsViewVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author lzh
 */
public interface CommentsService extends IService<Comments> {

    /**
     * 发布评论
     *
     * @param commentPublishDto 评论内容
     * @param request           请求对象
     * @return 评论ID
     */
    long publishComments(CommentPublishDto commentPublishDto, HttpServletRequest request);

    /**
     * 根据ID删除评论
     *
     * @param id      评论ID
     * @param request 请求对象
     */
    void deleteCommentsById(Long id, HttpServletRequest request);

    /**
     * 分页查询评论
     *
     * @param commentPageDto 分页参数
     * @return 树形评论
     */
    IPage<CommentsViewVo> queryCommentsByPage(CommentPageDto commentPageDto);

    /**
     * 根据图片ID查询评论总数
     *
     * @param picId 图片ID
     * @return 评论总数
     */
    int getCommentCountByPicId(Long picId);

    /**
     * 评论点赞
     *
     * @param commentId 评论ID
     * @param request   请求对象
     */
    void thumbComment(Long commentId, HttpServletRequest request);

    /**
     * 取消评论点赞
     *
     * @param commentId 评论ID
     * @param request   请求对象
     */
    void cancelThumbComment(Long commentId, HttpServletRequest request);

    /**
     * 批量查询图片的评论数
     *
     * @param picIdList 图片ID列表
     * @return 图片ID与评论数的映射
     */
    Map<Long, Long> queryBatchCommentCount(List<Long> picIdList);

    /**
     * 人工评论审核
     *
     * @param commentReviewDto 评论审核参数
     * @param request          请求对象
     */
    void commentReview(CommentReviewDto commentReviewDto, HttpServletRequest request);
}
