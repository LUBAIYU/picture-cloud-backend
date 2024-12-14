package com.by.cloud.controller;

import com.by.cloud.common.BaseResponse;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.model.dto.user.UserLoginDto;
import com.by.cloud.model.dto.user.UserRegisterDto;
import com.by.cloud.model.vo.UserLoginVo;
import com.by.cloud.model.vo.UserVo;
import com.by.cloud.service.UserService;
import com.by.cloud.utils.ResultUtils;
import com.by.cloud.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

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
}
