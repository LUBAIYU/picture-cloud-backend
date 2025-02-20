
package com.by.cloud.controller;

import com.by.cloud.common.BaseResponse;
import com.by.cloud.common.PageResult;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.model.dto.comment.CommentReviewsPageDto;
import com.by.cloud.model.entity.CommentReviews;
import com.by.cloud.service.CommentReviewsService;
import com.by.cloud.utils.ResultUtils;
import com.by.cloud.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author lzh
 */
@RestController
@RequestMapping("/comment/reviews")
@Api(tags = "评论审核模块")
public class CommentReviewsController {

    @Resource
    private CommentReviewsService commentReviewsService;

    @ApiOperation("分页查询评论审核记录")
    @PostMapping("/page")
    public BaseResponse<PageResult<CommentReviews>> queryCommentReviewsByPage(@RequestBody CommentReviewsPageDto pageDto) {
        ThrowUtils.throwIf(pageDto == null, ErrorCode.PARAMS_ERROR);
        PageResult<CommentReviews> pageResult = commentReviewsService.queryCommentReviewsByPage(pageDto);
        return ResultUtils.success(pageResult);
    }
}
