package com.by.cloud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.model.entity.User;
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
}
