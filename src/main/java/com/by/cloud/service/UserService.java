package com.by.cloud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.model.dto.user.UserLoginDto;
import com.by.cloud.model.dto.user.UserRegisterDto;
import com.by.cloud.model.entity.User;
import com.by.cloud.model.vo.UserLoginVo;
import com.by.cloud.model.vo.UserVo;

/**
 * @author lzh
 */
public interface UserService extends IService<User> {

    /**
     * 获取当前登录用户
     *
     * @return 脱敏用户信息
     */
    UserVo getLoginUser();

    /**
     * 用户注册
     *
     * @param dto 请求数据
     * @return 用户ID
     */
    long userRegister(UserRegisterDto dto);

    /**
     * 用户登录
     *
     * @param dto 请求数据
     * @return UserLoginVo
     */
    UserLoginVo userLogin(UserLoginDto dto);
}
