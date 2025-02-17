package com.by.cloud.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.model.dto.comment.CommentPageDto;
import com.by.cloud.model.dto.comment.CommentPublishDto;
import com.by.cloud.model.entity.Comments;
import com.by.cloud.model.vo.comment.CommentsViewVo;

import javax.servlet.http.HttpServletRequest;

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
}
