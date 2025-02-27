package com.by.cloud.controller;

import com.by.cloud.common.BaseResponse;
import com.by.cloud.common.PageResult;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.model.dto.comment.CommentLikesPageDto;
import com.by.cloud.model.entity.CommentLikes;
import com.by.cloud.service.CommentLikesService;
import com.by.cloud.utils.ResultUtils;
import com.by.cloud.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author lzh
 */
@RestController
@RequestMapping("/comment/likes")
@Api(tags = "评论点赞模块")
public class CommentLikesController {

    @Resource
    private CommentLikesService commentLikesService;

    @ApiOperation("分页查询评论点赞记录")
    @PostMapping("/page")
    public BaseResponse<PageResult<CommentLikes>> queryCommentLikesByPage(CommentLikesPageDto pageDto) {
        ThrowUtils.throwIf(pageDto == null, ErrorCode.PARAMS_ERROR);
        PageResult<CommentLikes> pageResult = commentLikesService.queryCommentLikesByPage(pageDto);
        return ResultUtils.success(pageResult);
    }
}
