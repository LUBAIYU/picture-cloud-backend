package com.by.cloud.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.by.cloud.common.BaseResponse;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.model.dto.comment.CommentPageDto;
import com.by.cloud.model.dto.comment.CommentPublishDto;
import com.by.cloud.model.vo.comment.CommentsViewVo;
import com.by.cloud.service.CommentsService;
import com.by.cloud.utils.ResultUtils;
import com.by.cloud.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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
}
