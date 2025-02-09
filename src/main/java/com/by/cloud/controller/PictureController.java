package com.by.cloud.controller;

import cn.hutool.core.util.StrUtil;
import com.by.cloud.aop.PreAuthorize;
import com.by.cloud.api.aliyunai.AliYunAiApi;
import com.by.cloud.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.by.cloud.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.by.cloud.api.imagesearch.model.ImageSearchResult;
import com.by.cloud.common.BaseResponse;
import com.by.cloud.common.PageResult;
import com.by.cloud.common.auth.StpKit;
import com.by.cloud.common.auth.annotation.SaSpaceCheckPermission;
import com.by.cloud.constants.SpaceUserPermissionConstant;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.PictureReviewStatusEnum;
import com.by.cloud.enums.UserRoleEnum;
import com.by.cloud.model.dto.picture.*;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.vo.picture.PictureTagCategoryVo;
import com.by.cloud.model.vo.picture.PictureVo;
import com.by.cloud.service.PictureService;
import com.by.cloud.utils.ResultUtils;
import com.by.cloud.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
    private AliYunAiApi aliYunAiApi;

    @ApiOperation("上传图片")
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVo> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadDto dto, HttpServletRequest request) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR);
        PictureVo pictureVo = pictureService.uploadPicture(multipartFile, dto, request);
        return ResultUtils.success(pictureVo);
    }

    @ApiOperation("通过 URL 上传图片")
    @PostMapping("/url/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVo> uploadPictureByUrl(@RequestBody PictureUploadDto dto, HttpServletRequest request) {
        ThrowUtils.throwIf(dto == null, ErrorCode.PARAMS_ERROR);
        String fileUrl = dto.getFileUrl();
        PictureVo pictureVo = pictureService.uploadPicture(fileUrl, dto, request);
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
    public BaseResponse<PictureVo> getPictureVoById(@RequestParam("picId") Long picId, HttpServletRequest request) {
        ThrowUtils.throwIf(picId == null || picId <= 0, ErrorCode.PARAMS_ERROR);
        PictureVo pictureVo = pictureService.getPictureVo(picId, request);
        return ResultUtils.success(pictureVo);
    }

    @ApiOperation("根据ID删除图片")
    @DeleteMapping("/delete/{picId}")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePictureById(@PathVariable Long picId) {
        ThrowUtils.throwIf(picId == null || picId <= 0, ErrorCode.PARAMS_ERROR);
        pictureService.deletePictureById(picId);
        return ResultUtils.success(true);
    }

    @ApiOperation("分页查询图片（仅管理员）")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PostMapping("/page")
    public BaseResponse<PageResult<PictureVo>> queryPictureByPage(@RequestBody PicturePageDto pageDto) {
        ThrowUtils.throwIf(pageDto == null, ErrorCode.PARAMS_ERROR);
        PageResult<PictureVo> pageResult = pictureService.queryPictureVoByPage(pageDto);
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
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }
        PageResult<PictureVo> pageResult = pictureService.queryPictureVoByPage(pageDto);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation("分页查询图片（封装类），多级缓存")
    @PostMapping("/cache/vo/page")
    public BaseResponse<PageResult<PictureVo>> queryPictureVoByPageWithCache(@RequestBody PicturePageDto pageDto) {
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
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }
        PageResult<PictureVo> pageResult = pictureService.queryPictureVoByPageWithCache(pageDto);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation("更新图片信息")
    @PutMapping("/update")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateDto updateDto, HttpServletRequest request) {
        ThrowUtils.throwIf(updateDto == null, ErrorCode.PARAMS_ERROR);
        boolean isSuccess = pictureService.updatePicture(updateDto, request);
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
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewDto reviewDto, HttpServletRequest request) {
        ThrowUtils.throwIf(reviewDto == null, ErrorCode.PARAMS_ERROR);
        pictureService.pictureReview(reviewDto, request);
        return ResultUtils.success(true);
    }

    @ApiOperation("批量抓取并上传图片")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PostMapping("/batch/upload")
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureBatchDto batchDto, HttpServletRequest request) {
        ThrowUtils.throwIf(batchDto == null, ErrorCode.PARAMS_ERROR);
        int uploadCount = pictureService.uploadPictureByBatch(batchDto, request);
        return ResultUtils.success(uploadCount);
    }

    @ApiOperation("以图搜图")
    @PostMapping("/search/byPicture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody PictureSearchByPictureDto searchByPictureDto) {
        ThrowUtils.throwIf(searchByPictureDto == null, ErrorCode.PARAMS_ERROR);
        List<ImageSearchResult> resultList = pictureService.searchPictureByPicture(searchByPictureDto);
        return ResultUtils.success(resultList);
    }

    @ApiOperation("根据颜色搜索图片")
    @PostMapping("/search/byColor")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVo>> searchPictureByColor(@RequestBody PictureSearchByColorDto searchByColorDto, HttpServletRequest request) {
        ThrowUtils.throwIf(searchByColorDto == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = searchByColorDto.getSpaceId();
        String picColor = searchByColorDto.getPicColor();
        List<PictureVo> resultList = pictureService.searchPictureByColor(spaceId, picColor, request);
        return ResultUtils.success(resultList);
    }

    @ApiOperation("批量更新图片信息")
    @PutMapping("/edit/byBatch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchDto pictureEditByBatchDto, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchDto == null, ErrorCode.PARAMS_ERROR);
        pictureService.editPictureByBatch(pictureEditByBatchDto, request);
        return ResultUtils.success(true);
    }

    @ApiOperation("创建AI扩图任务")
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createOutPaintingTask(@RequestBody PictureCreateOutPaintingTaskDto createOutPaintingTaskDto) {
        ThrowUtils.throwIf(createOutPaintingTaskDto == null, ErrorCode.PARAMS_ERROR);
        CreateOutPaintingTaskResponse response = pictureService.createOutPaintingTask(createOutPaintingTaskDto);
        return ResultUtils.success(response);
    }

    @ApiOperation("查询AI扩图任务")
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }

    @ApiOperation("刷新指定缓存")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PostMapping("/cache/refresh")
    public BaseResponse<Boolean> refreshCache(@RequestBody PicturePageDto pageDto) {
        ThrowUtils.throwIf(pageDto == null, ErrorCode.PARAMS_ERROR);
        pictureService.refreshCache(pageDto);
        return ResultUtils.success(true);
    }

    @ApiOperation("获取所有缓存Key")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @GetMapping("/cache/keyList")
    public BaseResponse<List<String>> listAllCacheKeys(String prefix) {
        List<String> keys = pictureService.getAllCacheKeys(prefix);
        return ResultUtils.success(keys);
    }

    @ApiOperation("删除指定缓存")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @GetMapping("/cache/remove")
    public BaseResponse<Boolean> deleteCacheByKey(@RequestParam String hashKey) {
        ThrowUtils.throwIf(StrUtil.isBlank(hashKey), ErrorCode.PARAMS_ERROR);
        boolean result = pictureService.removeCacheByKey(hashKey);
        return ResultUtils.success(result);
    }
}
