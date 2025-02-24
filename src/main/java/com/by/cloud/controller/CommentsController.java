package com.by.cloud.controller;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.by.cloud.common.BaseResponse;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.model.dto.comment.CommentPageDto;
import com.by.cloud.model.dto.comment.CommentPublishDto;
import com.by.cloud.model.dto.comment.CommentReviewDto;
import com.by.cloud.model.vo.comment.CommentsViewVo;
import com.by.cloud.service.CommentsService;
import com.by.cloud.utils.ResultUtils;
import com.by.cloud.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author lzh
 */
@RestController
@RequestMapping("/comment")
@Api(tags = "评论模块")
public class CommentsController {

    @Resource
    private CommentsService commentsService;

    @ApiOperation("发布评论")
    @PostMapping("/publish")
    public BaseResponse<Long> publishComments(@RequestBody CommentPublishDto commentPublishDto, HttpServletRequest request) {
        ThrowUtils.throwIf(commentPublishDto == null, ErrorCode.PARAMS_ERROR);
        long id = commentsService.publishComments(commentPublishDto, request);
        return ResultUtils.success(id);
    }

    @ApiOperation("删除评论")
    @DeleteMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        commentsService.deleteCommentsById(id, request);
        return ResultUtils.success(true);
    }

    @ApiOperation("查询评论列表")
    @PostMapping("/page/vo")
    public BaseResponse<IPage<CommentsViewVo>> queryComments(@RequestBody CommentPageDto commentPageDto) {
        ThrowUtils.throwIf(commentPageDto == null, ErrorCode.PARAMS_ERROR);
        IPage<CommentsViewVo> pageResult = commentsService.queryCommentsByPage(commentPageDto);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation("查询某张图片的评论总数")
    @GetMapping("/count/{id}")
    public BaseResponse<Integer> getCommentCount(@PathVariable("id") Long picId) {
        ThrowUtils.throwIf(picId == null || picId <= 0, ErrorCode.PARAMS_ERROR);
        int count = commentsService.getCommentCountByPicId(picId);
        return ResultUtils.success(count);
    }

    @ApiOperation("批量查询图片的评论数")
    @GetMapping("/count/batch")
    public BaseResponse<Map<Long, Long>> queryBatchCommentCount(@RequestParam("ids") List<Long> picIdList) {
        ThrowUtils.throwIf(CollUtil.isEmpty(picIdList), ErrorCode.PARAMS_ERROR);
        Map<Long, Long> resultMap = commentsService.queryBatchCommentCount(picIdList);
        return ResultUtils.success(resultMap);
    }

    @ApiOperation("评论点赞")
    @PostMapping("/thumb")
    public BaseResponse<Boolean> thumbComment(Long commentId, HttpServletRequest request) {
        ThrowUtils.throwIf(commentId == null || commentId <= 0, ErrorCode.PARAMS_ERROR);
        commentsService.thumbComment(commentId, request);
        return ResultUtils.success(true);
    }

    @ApiOperation("取消评论点赞")
    @PostMapping("/thumb/cancel")
    public BaseResponse<Boolean> cancelThumbComment(Long commentId, HttpServletRequest request) {
        ThrowUtils.throwIf(commentId == null || commentId <= 0, ErrorCode.PARAMS_ERROR);
        commentsService.cancelThumbComment(commentId, request);
        return ResultUtils.success(true);
    }

    @ApiOperation("人工评论审核")
    @PostMapping("/review")
    public BaseResponse<Boolean> commentReview(@RequestBody CommentReviewDto commentReviewDto, HttpServletRequest request) {
        ThrowUtils.throwIf(commentReviewDto == null, ErrorCode.PARAMS_ERROR);
        commentsService.commentReview(commentReviewDto, request);
        return ResultUtils.success(true);
    }
}
