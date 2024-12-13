package com.by.cloud.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.mapper.UserMapper;
import com.by.cloud.model.entity.User;
import com.by.cloud.service.UserService;
import org.springframework.stereotype.Service;

/**
* @author lzh
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{

}




