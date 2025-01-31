package com.by.cloud.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.by.cloud.common.BaseContext;
import com.by.cloud.common.BaseResponse;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.model.dto.spaceuser.SpaceUserAddDto;
import com.by.cloud.model.dto.spaceuser.SpaceUserEditDto;
import com.by.cloud.model.dto.spaceuser.SpaceUserQueryDto;
import com.by.cloud.model.entity.SpaceUser;
import com.by.cloud.model.vo.spaceuser.SpaceUserVo;
import com.by.cloud.service.SpaceUserService;
import com.by.cloud.utils.ResultUtils;
import com.by.cloud.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author lzh
 */
@RestController
@RequestMapping("/spaceUser")
@Api(tags = "空间用户关联模块")
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    @ApiOperation("添加成员到空间")
    @PostMapping("/add")
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddDto spaceUserAddDto) {
        ThrowUtils.throwIf(spaceUserAddDto == null, ErrorCode.PARAMS_ERROR);
        long id = spaceUserService.addSpaceUser(spaceUserAddDto);
        return ResultUtils.success(id);
    }

    @ApiOperation("从空间移除成员")
    @DeleteMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteSpaceUserById(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        spaceUserService.deleteById(id);
        return ResultUtils.success(true);
    }

    @ApiOperation("查询某个成员在某个空间的信息")
    @PostMapping("/get")
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryDto queryDto) {
        ThrowUtils.throwIf(queryDto == null, ErrorCode.PARAMS_ERROR);
        // 参数校验
        Long userId = queryDto.getUserId();
        Long spaceId = queryDto.getSpaceId();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(userId, spaceId), ErrorCode.PARAMS_ERROR);
        // 查询
        LambdaQueryWrapper<SpaceUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SpaceUser::getUserId, userId);
        queryWrapper.eq(SpaceUser::getSpaceId, spaceId);
        SpaceUser spaceUser = spaceUserService.getOne(queryWrapper);
        return ResultUtils.success(spaceUser);
    }

    @ApiOperation("查询空间成员列表")
    @PostMapping("/list")
    public BaseResponse<List<SpaceUserVo>> listSpaceUser(@RequestBody SpaceUserQueryDto queryDto) {
        ThrowUtils.throwIf(queryDto == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUserVo> spaceUserVoList = spaceUserService.querySpaceVoList(queryDto);
        return ResultUtils.success(spaceUserVoList);
    }

    @ApiOperation("编辑成员信息")
    @PutMapping("/edit")
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditDto editDto) {
        ThrowUtils.throwIf(editDto == null, ErrorCode.PARAMS_ERROR);
        spaceUserService.editSpaceUserById(editDto);
        return ResultUtils.success(true);
    }

    @ApiOperation("查询我加入的团队空间列表")
    @GetMapping("/list/my")
    public BaseResponse<List<SpaceUserVo>> listMyTeamSpace() {
        Long loginUserId = BaseContext.getLoginUserId();
        SpaceUserQueryDto queryDto = new SpaceUserQueryDto();
        queryDto.setUserId(loginUserId);
        List<SpaceUserVo> spaceUserVoList = spaceUserService.querySpaceVoList(queryDto);
        return ResultUtils.success(spaceUserVoList);
    }
}
