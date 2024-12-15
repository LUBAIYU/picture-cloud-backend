package com.by.cloud.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.by.cloud.aop.PreAuthorize;
import com.by.cloud.common.BaseResponse;
import com.by.cloud.common.PageResult;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.UserRoleEnum;
import com.by.cloud.model.dto.user.UserLoginDto;
import com.by.cloud.model.dto.user.UserPageDto;
import com.by.cloud.model.dto.user.UserRegisterDto;
import com.by.cloud.model.dto.user.UserUpdateDto;
import com.by.cloud.model.entity.User;
import com.by.cloud.model.vo.UserLoginVo;
import com.by.cloud.model.vo.UserVo;
import com.by.cloud.service.UserService;
import com.by.cloud.utils.ResultUtils;
import com.by.cloud.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author lzh
 */
@RestController
@RequestMapping("/user")
@Slf4j
@Api(tags = "用户模块")
public class UserController {

    @Resource
    private UserService userService;

    @ApiOperation("获取当前登录用户信息")
    @GetMapping("/getLoginUser")
    public BaseResponse<UserVo> getLoginUser() {
        UserVo userVo = userService.getLoginUser();
        return ResultUtils.success(userVo);
    }

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterDto dto) {
        ThrowUtils.throwIf(dto == null, ErrorCode.PARAMS_ERROR);
        long userId = userService.userRegister(dto);
        return ResultUtils.success(userId);
    }

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public BaseResponse<UserLoginVo> login(@RequestBody UserLoginDto dto) {
        ThrowUtils.throwIf(dto == null, ErrorCode.PARAMS_ERROR);
        UserLoginVo userLoginVo = userService.userLogin(dto);
        return ResultUtils.success(userLoginVo);
    }

    @ApiOperation("上传头像")
    @PostMapping("/avatar/upload")
    public BaseResponse<String> uploadAvatar(@RequestPart("file") MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR);
        String avatarUrl = userService.uploadAvatar(multipartFile);
        return ResultUtils.success(avatarUrl);
    }

    @ApiOperation("批量删除用户")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @DeleteMapping("/batch/delete")
    public BaseResponse<Boolean> deleteUserByIds(@RequestParam("ids") List<Long> ids) {
        ThrowUtils.throwIf(CollUtil.isEmpty(ids), ErrorCode.PARAMS_ERROR);
        boolean isTrue = userService.deleteBatchByIds(ids);
        return ResultUtils.success(isTrue);
    }

    @ApiOperation("根据ID获取用户信息（仅管理员）")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @GetMapping("/get/{id}")
    public BaseResponse<User> getUserById(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        return ResultUtils.success(user);
    }

    @ApiOperation("根据ID获取用户信息")
    @GetMapping("/get/vo/{id}")
    public BaseResponse<UserVo> getUserVoById(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        UserVo userVo = BeanUtil.copyProperties(user, UserVo.class);
        return ResultUtils.success(userVo);
    }

    @ApiOperation("根据ID修改用户信息")
    @PutMapping("/update")
    public BaseResponse<Boolean> updateUserById(@RequestBody UserUpdateDto updateDto) {
        ThrowUtils.throwIf(updateDto == null, ErrorCode.PARAMS_ERROR);
        boolean isSuccess = userService.updateUserById(updateDto);
        return ResultUtils.success(isSuccess);
    }

    @ApiOperation("批量冻结用户")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @GetMapping("/batch/freeze")
    public BaseResponse<Boolean> freezeBatchUsers(@RequestParam("ids") List<Long> ids) {
        ThrowUtils.throwIf(CollUtil.isEmpty(ids), ErrorCode.PARAMS_ERROR);
        boolean isSuccess = userService.freezeUsersByIds(ids);
        return ResultUtils.success(isSuccess);
    }

    @ApiOperation("分页查询用户信息")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PostMapping("/page")
    public BaseResponse<PageResult<UserVo>> pageUsers(@RequestBody UserPageDto pageDto) {
        ThrowUtils.throwIf(pageDto == null, ErrorCode.PARAMS_ERROR);
        PageResult<UserVo> pageResult = userService.pageUsers(pageDto);
        return ResultUtils.success(pageResult);
    }
}
