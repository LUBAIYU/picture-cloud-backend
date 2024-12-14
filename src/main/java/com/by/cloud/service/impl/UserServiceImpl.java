package com.by.cloud.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.common.BaseContext;
import com.by.cloud.common.JwtProperties;
import com.by.cloud.constants.UserConstant;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.UserRoleEnum;
import com.by.cloud.enums.UserStatusEnum;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.mapper.UserMapper;
import com.by.cloud.model.dto.user.UserLoginDto;
import com.by.cloud.model.dto.user.UserRegisterDto;
import com.by.cloud.model.entity.User;
import com.by.cloud.model.vo.UserLoginVo;
import com.by.cloud.model.vo.UserVo;
import com.by.cloud.service.UserService;
import com.by.cloud.utils.JwtUtils;
import com.by.cloud.utils.ThrowUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lzh
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Value("${user.encrypt.salt}")
    private String salt;

    @Resource
    private JwtProperties jwtProperties;

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

    @Override
    public long userRegister(UserRegisterDto dto) {
        // 获取参数
        String userAccount = dto.getUserAccount();
        String userPassword = dto.getUserPassword();
        String checkPassword = dto.getCheckPassword();

        // 校验参数
        ThrowUtils.throwIf(StringUtils.isAnyBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号长度不能小于4位");
        checkPassword(userPassword, checkPassword);

        // 判断账号是否重复
        User dbUser = this.lambdaQuery().eq(User::getUserAccount, userAccount).one();
        ThrowUtils.throwIf(dbUser != null, ErrorCode.PARAMS_ERROR, "账号已存在");

        // 获取加密后的密码
        String encryptPassword = getEncryptPassword(userPassword);

        // 保存数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setUserStatus(UserStatusEnum.ENABLE.getValue());
        boolean isTrue = this.save(user);
        ThrowUtils.throwIf(!isTrue, ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误！");

        return user.getUserId();
    }

    @Override
    public UserLoginVo userLogin(UserLoginDto dto) {
        // 请求参数
        String userAccount = dto.getUserAccount();
        String userPassword = dto.getUserPassword();
        String encryptPassword = getEncryptPassword(userPassword);

        // 校验参数
        ThrowUtils.throwIf(StringUtils.isAnyBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号长度不能小于4位");
        checkPassword(userPassword, null);

        // 校验账号和密码
        User dbUser = this.lambdaQuery().eq(User::getUserAccount, userAccount).one();
        ThrowUtils.throwIf(dbUser == null, ErrorCode.PARAMS_ERROR, "账号或密码错误");
        if (!dbUser.getUserPassword().equals(encryptPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }

        // 创建令牌，并将用户ID存储到令牌中
        Map<String, Object> claims = new HashMap<>(3);
        claims.put(UserConstant.USER_ID, dbUser.getUserId());
        String token = JwtUtils.createJwt(jwtProperties.getSecretKey(), jwtProperties.getTtl(), claims);
        UserLoginVo loginVo = new UserLoginVo();
        loginVo.setToken(token);
        return loginVo;
    }

    /**
     * 校验密码和确认密码
     *
     * @param userPassword  密码
     * @param checkPassword 确认密码
     */
    private void checkPassword(String userPassword, String checkPassword) {
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "密码长度不能小于8位");
        ThrowUtils.throwIf(userPassword.matches(".*\\s.*"), ErrorCode.PARAMS_ERROR, "用户密码不能包含空格、制表符、换行符等非法输入！");
        if (StrUtil.isNotBlank(checkPassword)) {
            ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "密码不一致");
        }
    }

    /**
     * 获取加密密码
     *
     * @param userPassword 密码
     * @return 密钥
     */
    private String getEncryptPassword(String userPassword) {
        return DigestUtil.md5Hex(userPassword + salt);
    }
}




