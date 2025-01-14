package com.by.cloud.controller;

import com.by.cloud.aop.PreAuthorize;
import com.by.cloud.common.BaseContext;
import com.by.cloud.common.BaseResponse;
import com.by.cloud.common.PageResult;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.PictureReviewStatusEnum;
import com.by.cloud.enums.UserRoleEnum;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.model.dto.picture.*;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.entity.Space;
import com.by.cloud.model.vo.picture.PictureTagCategoryVo;
import com.by.cloud.model.vo.picture.PictureVo;
import com.by.cloud.service.PictureService;
import com.by.cloud.service.SpaceService;
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

    @Resource
    private SpaceService spaceService;

    @ApiOperation("上传图片")
    @PostMapping("/upload")
    public BaseResponse<PictureVo> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadDto dto) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR);
        PictureVo pictureVo = pictureService.uploadPicture(multipartFile, dto);
        return ResultUtils.success(pictureVo);
    }

    @ApiOperation("通过 URL 上传图片")
    @PostMapping("/url/upload")
    public BaseResponse<PictureVo> uploadPictureByUrl(@RequestBody PictureUploadDto dto) {
        ThrowUtils.throwIf(dto == null, ErrorCode.PARAMS_ERROR);
        String fileUrl = dto.getFileUrl();
        PictureVo pictureVo = pictureService.uploadPicture(fileUrl, dto);
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
        pictureService.deletePictureById(picId);
        return ResultUtils.success(true);
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
        // 空间权限校验
        Long spaceId = pageDto.getSpaceId();
        if (spaceId == null) {
            // 公共图库,用户只能看到审核通过的图片
            pageDto.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pageDto.setNullSpaceId(true);
        } else {
            // 私有空间，只有空间创建者才能查看
            Long loginUserId = BaseContext.getLoginUserId();
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if (!loginUserId.equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有查看该空间的权限");
            }
        }
        PageResult<PictureVo> pageResult = pictureService.queryPictureVoByPage(pageDto);
        return ResultUtils.success(pageResult);
    }

    @Deprecated
    @ApiOperation("分页查询图片（封装类），多级缓存")
    @PostMapping("/cache/vo/page")
    public BaseResponse<PageResult<PictureVo>> queryPictureVoByPageWithCache(@RequestBody PicturePageDto pageDto) {
        ThrowUtils.throwIf(pageDto == null, ErrorCode.PARAMS_ERROR);
        // 限制爬虫
        int pageSize = pageDto.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        // 用户只能看到审核通过的图片
        pageDto.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        PageResult<PictureVo> pageResult = pictureService.queryPictureVoByPageWithCache(pageDto);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation("更新图片信息")
    @PutMapping("/update")
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateDto updateDto) {
        ThrowUtils.throwIf(updateDto == null, ErrorCode.PARAMS_ERROR);
        boolean isSuccess = pictureService.updatePicture(updateDto);
        return ResultUtils.success(isSuccess);
    }

    @ApiOperation("获取图片标签分类列表")
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategoryVo> listPictureTagCategory() {
        PictureTagCategoryVo pictureTagCategoryVo = pictureService.listPictureTagCategory();
        return ResultUtils.success(pictureTagCategoryVo);
    }

    @ApiOperation("图片审核")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PostMapping("/review")
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewDto reviewDto) {
        ThrowUtils.throwIf(reviewDto == null, ErrorCode.PARAMS_ERROR);
        pictureService.pictureReview(reviewDto);
        return ResultUtils.success(true);
    }

    @ApiOperation("批量抓取并上传图片")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PostMapping("/batch/upload")
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureBatchDto batchDto) {
        ThrowUtils.throwIf(batchDto == null, ErrorCode.PARAMS_ERROR);
        int uploadCount = pictureService.uploadPictureByBatch(batchDto);
        return ResultUtils.success(uploadCount);
    }
}
