package com.by.cloud.controller;

import com.by.cloud.aop.PreAuthorize;
import com.by.cloud.common.BaseResponse;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.UserRoleEnum;
import com.by.cloud.model.dto.picture.PictureUploadDto;
import com.by.cloud.model.vo.PictureVo;
import com.by.cloud.service.PictureService;
import com.by.cloud.utils.ResultUtils;
import com.by.cloud.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * @author lzh
 */
@RestController
@RequestMapping("/picture")
@Slf4j
@Api(tags = "图片模块")
public class PictureController {

    @Resource
    private PictureService pictureService;

    @ApiOperation("上传图片")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PostMapping("/upload")
    public BaseResponse<PictureVo> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadDto dto) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR);
        PictureVo pictureVo = pictureService.uploadPicture(multipartFile, dto);
        return ResultUtils.success(pictureVo);
    }
}
