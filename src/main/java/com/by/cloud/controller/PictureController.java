package com.by.cloud.controller;

import com.by.cloud.aop.PreAuthorize;
import com.by.cloud.common.BaseResponse;
import com.by.cloud.common.PageResult;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.UserRoleEnum;
import com.by.cloud.model.dto.picture.PictureEditDto;
import com.by.cloud.model.dto.picture.PicturePageDto;
import com.by.cloud.model.dto.picture.PictureUpdateDto;
import com.by.cloud.model.dto.picture.PictureUploadDto;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.vo.PictureVo;
import com.by.cloud.service.PictureService;
import com.by.cloud.utils.ResultUtils;
import com.by.cloud.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
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

    @ApiOperation("根据ID获取图片（仅管理员）")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @GetMapping("/get")
    public BaseResponse<Picture> getPictureById(@RequestParam("picId") Long picId) {
        ThrowUtils.throwIf(picId == null || picId <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(picId);
        return ResultUtils.success(picture);
    }

    @ApiOperation("根据ID获取图片（封装类）")
    @GetMapping("/vo/get")
    public BaseResponse<PictureVo> getPictureVoById(@RequestParam("picId") Long picId) {
        ThrowUtils.throwIf(picId == null || picId <= 0, ErrorCode.PARAMS_ERROR);
        PictureVo pictureVo = pictureService.getPictureVo(picId);
        return ResultUtils.success(pictureVo);
    }

    @ApiOperation("根据ID删除图片")
    @DeleteMapping("/delete/{picId}")
    public BaseResponse<Boolean> deletePictureById(@PathVariable Long picId) {
        ThrowUtils.throwIf(picId == null || picId <= 0, ErrorCode.PARAMS_ERROR);
        boolean isSuccess = pictureService.deleteById(picId);
        return ResultUtils.success(isSuccess);
    }

    @ApiOperation("分页查询图片（仅管理员）")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PostMapping("/page")
    public BaseResponse<PageResult<Picture>> queryPictureByPage(@RequestBody PicturePageDto pageDto) {
        ThrowUtils.throwIf(pageDto == null, ErrorCode.PARAMS_ERROR);
        PageResult<Picture> pageResult = pictureService.queryPictureByPage(pageDto);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation("分页查询图片（封装类）")
    @PostMapping("/vo/page")
    public BaseResponse<PageResult<PictureVo>> queryPictureVoByPage(@RequestBody PicturePageDto pageDto) {
        ThrowUtils.throwIf(pageDto == null, ErrorCode.PARAMS_ERROR);
        // 限制爬虫
        int pageSize = pageDto.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        PageResult<PictureVo> pageResult = pictureService.queryPictureVoByPage(pageDto);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation("更新图片信息（仅管理员）")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PutMapping("/update")
    public BaseResponse<Boolean> updatePictureByAdmin(@RequestBody PictureUpdateDto updateDto) {
        ThrowUtils.throwIf(updateDto == null, ErrorCode.PARAMS_ERROR);
        boolean isSuccess = pictureService.updatePictureByAdmin(updateDto);
        return ResultUtils.success(isSuccess);
    }

    @ApiOperation("编辑图片信息")
    @PutMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditDto editDto) {
        ThrowUtils.throwIf(editDto == null, ErrorCode.PARAMS_ERROR);
        pictureService.editPicture(editDto);
        return ResultUtils.success(true);
    }
}
