package com.by.cloud.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.common.BaseContext;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.mapper.UserMapper;
import com.by.cloud.model.entity.User;
import com.by.cloud.model.vo.UserVo;
import com.by.cloud.service.UserService;
import org.springframework.stereotype.Service;

/**
 * @author lzh
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public UserVo getLoginUser() {
        // 从本地线程取出ID
        Long loginUserId = BaseContext.getLoginUserId();
        if (loginUserId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        User user = this.getById(loginUserId);
        return BeanUtil.copyProperties(user, UserVo.class);
    }
}




