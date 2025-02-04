package com.by.cloud.controller;

import com.by.cloud.aop.PreAuthorize;
import com.by.cloud.common.BaseResponse;
import com.by.cloud.common.PageResult;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.SpaceLevelEnum;
import com.by.cloud.enums.UserRoleEnum;
import com.by.cloud.model.dto.space.SpaceCreateDto;
import com.by.cloud.model.dto.space.SpaceEditDto;
import com.by.cloud.model.dto.space.SpacePageDto;
import com.by.cloud.model.dto.space.SpaceUpdateDto;
import com.by.cloud.model.entity.Space;
import com.by.cloud.model.vo.space.SpaceLevelVo;
import com.by.cloud.model.vo.space.SpaceVo;
import com.by.cloud.service.SpaceService;
import com.by.cloud.utils.ResultUtils;
import com.by.cloud.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * @author lzh
 */
@RestController
@RequestMapping("/space")
@Api(tags = "空间模块")
public class SpaceController {

    @Resource
    private SpaceService spaceService;

    @ApiOperation("删除空间信息")
    @DeleteMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteSpaceById(@PathVariable Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        spaceService.deleteById(id, request);
        return ResultUtils.success(true);
    }

    @ApiOperation("编辑空间信息")
    @PutMapping("/edit")
    public BaseResponse<Boolean> editSpaceById(@RequestBody SpaceEditDto editDto, HttpServletRequest request) {
        ThrowUtils.throwIf(editDto == null, ErrorCode.PARAMS_ERROR);
        spaceService.editSpaceById(editDto, request);
        return ResultUtils.success(true);
    }

    @ApiOperation("更新空间信息（仅管理员）")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PutMapping("/update")
    public BaseResponse<Boolean> updateSpaceById(@RequestBody SpaceUpdateDto updateDto) {
        ThrowUtils.throwIf(updateDto == null, ErrorCode.PARAMS_ERROR);
        spaceService.updateSpaceById(updateDto);
        return ResultUtils.success(true);
    }

    @ApiOperation("根据ID获取空间（仅管理员）")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @GetMapping("/get")
    public BaseResponse<Space> getSpaceById(@RequestParam("id") Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        return ResultUtils.success(space);
    }

    @ApiOperation("根据ID获取空间（封装类）")
    @GetMapping("/vo/get")
    public BaseResponse<SpaceVo> getSpaceVoById(@RequestParam("id") Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        SpaceVo spaceVo = spaceService.getSpaceVoById(id, request);
        return ResultUtils.success(spaceVo);
    }

    @ApiOperation("分页查询空间（仅管理员）")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PostMapping("/page")
    public BaseResponse<PageResult<Space>> querySpaceByPage(@RequestBody SpacePageDto pageDto) {
        ThrowUtils.throwIf(pageDto == null, ErrorCode.PARAMS_ERROR);
        PageResult<Space> pageResult = spaceService.querySpaceByPage(pageDto);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation("分页查询空间（封装类）")
    @PostMapping("/vo/page")
    public BaseResponse<PageResult<SpaceVo>> querySpaceVoByPage(@RequestBody SpacePageDto pageDto) {
        ThrowUtils.throwIf(pageDto == null, ErrorCode.PARAMS_ERROR);
        // 限制爬虫
        int pageSize = pageDto.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        PageResult<SpaceVo> pageResult = spaceService.querySpaceVoByPage(pageDto);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation("创建空间")
    @PostMapping("/create")
    public BaseResponse<Long> createSpace(@RequestBody SpaceCreateDto createDto, HttpServletRequest request) {
        ThrowUtils.throwIf(createDto == null, ErrorCode.PARAMS_ERROR);
        long id = spaceService.createSpace(createDto, request);
        return ResultUtils.success(id);
    }

    @ApiOperation("获取空间级别信息列表")
    @GetMapping("/level/list")
    public BaseResponse<List<SpaceLevelVo>> listSpaceLevel() {
        SpaceLevelEnum[] spaceLevelEnums = SpaceLevelEnum.values();
        List<SpaceLevelVo> spaceLevelVoList = Arrays.stream(spaceLevelEnums).map(spaceLevelEnum -> new SpaceLevelVo(
                spaceLevelEnum.getText(),
                spaceLevelEnum.getValue(),
                spaceLevelEnum.getMaxCount(),
                spaceLevelEnum.getMaxSize()
        )).toList();
        return ResultUtils.success(spaceLevelVoList);
    }
}
