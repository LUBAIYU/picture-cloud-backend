package com.by.cloud.controller;

import com.by.cloud.common.BaseResponse;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.model.dto.picture.PictureLikesDto;
import com.by.cloud.service.PictureLikesService;
import com.by.cloud.utils.ResultUtils;
import com.by.cloud.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author lzh
 */
@RestController
@RequestMapping("/picture/likes")
@Api(tags = "图片点赞模块")
public class PictureLikesController {

    @Resource
    private PictureLikesService pictureLikesService;

    @ApiOperation("图片点赞/取消点赞")
    @PostMapping("/thumb")
    public BaseResponse<Boolean> thumbOrCancelThumbPicture(@RequestBody PictureLikesDto pictureLikesDto, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureLikesDto == null, ErrorCode.PARAMS_ERROR);
        pictureLikesService.thumbOrCancelThumbPicture(pictureLikesDto, request);
        return ResultUtils.success(true);
    }
}
