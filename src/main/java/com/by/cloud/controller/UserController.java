package com.by.cloud.controller;

import com.by.cloud.common.BaseResponse;
import com.by.cloud.model.vo.UserVo;
import com.by.cloud.service.UserService;
import com.by.cloud.utils.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
