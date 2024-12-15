package com.by.cloud.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.common.BaseContext;
import com.by.cloud.common.FileManager;
import com.by.cloud.common.JwtProperties;
import com.by.cloud.common.PageResult;
import com.by.cloud.constants.UserConstant;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.UserRoleEnum;
import com.by.cloud.enums.UserStatusEnum;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.mapper.UserMapper;
import com.by.cloud.model.dto.user.UserLoginDto;
import com.by.cloud.model.dto.user.UserPageDto;
import com.by.cloud.model.dto.user.UserRegisterDto;
import com.by.cloud.model.dto.user.UserUpdateDto;
import com.by.cloud.model.entity.User;
import com.by.cloud.model.vo.UserLoginVo;
import com.by.cloud.model.vo.UserVo;
import com.by.cloud.service.UserService;
import com.by.cloud.utils.JwtUtils;
import com.by.cloud.utils.ThrowUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    @Resource
    private FileManager fileManager;

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

        // 判断用户是否被冻结
        UserStatusEnum statusEnum = UserStatusEnum.getEnumByValue(dbUser.getUserStatus());
        if (UserStatusEnum.DISABLE.equals(statusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号被冻结，请联系管理员");
        }

        // 创建令牌，并将用户ID存储到令牌中
        Map<String, Object> claims = new HashMap<>(3);
        claims.put(UserConstant.USER_ID, dbUser.getUserId());
        String token = JwtUtils.createJwt(jwtProperties.getSecretKey(), jwtProperties.getTtl(), claims);
        UserLoginVo loginVo = new UserLoginVo();
        loginVo.setToken(token);
        return loginVo;
    }

    @Override
    public String uploadAvatar(MultipartFile multipartFile) {
        // 获取登录用户
        UserVo loginUser = this.getLoginUser();
        // 构造地址前缀
        String uploadPathPrefix = String.format("avatar/%s", loginUser.getUserId());
        // 上传图片
        return fileManager.uploadAvatar(multipartFile, uploadPathPrefix);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public boolean deleteBatchByIds(List<Long> ids) {
        // 获取登录用户
        UserVo loginUser = this.getLoginUser();
        // 如果ID中包含登录用户的ID，则不能删除
        if (ids.contains(loginUser.getUserId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能删除当前登录用户");
        }
        return this.removeBatchByIds(ids);
    }

    @Override
    public boolean updateUserById(UserUpdateDto updateDto) {
        // 校验参数
        Long userId = updateDto.getUserId();
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "用户ID不能为空");

        // 如果账号不为空，账号长度不能小于4位
        String userAccount = updateDto.getUserAccount();
        if (StrUtil.isNotBlank(userAccount) && userAccount.length() < UserConstant.USER_ACCOUNT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度不能小于4位");
        }

        // 获取登录用户
        UserVo loginUser = this.getLoginUser();

        // 如果当前登录用户是普通用户，则不能修改角色
        UserRoleEnum roleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (UserRoleEnum.ADMIN.getValue().equals(updateDto.getUserRole()) && UserRoleEnum.USER.equals(roleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 判断是否有更新
        boolean isUpdate = false;
        // 构建更新条件
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUserId, userId);
        if (StrUtil.isNotBlank(updateDto.getUserAccount())) {
            updateWrapper.set(User::getUserAccount, updateDto.getUserAccount());
            isUpdate = true;
        }
        if (StrUtil.isNotBlank(updateDto.getUserName())) {
            updateWrapper.set(User::getUserName, updateDto.getUserName());
            isUpdate = true;
        }
        if (StrUtil.isNotBlank(updateDto.getUserAvatar())) {
            updateWrapper.set(User::getUserAvatar, updateDto.getUserAvatar());
            isUpdate = true;
        }
        if (StrUtil.isNotBlank(updateDto.getUserProfile())) {
            updateWrapper.set(User::getUserProfile, updateDto.getUserProfile());
            isUpdate = true;
        }

        // 如果有传角色则需传有效数据
        if (updateDto.getUserRole() != null) {
            UserRoleEnum updateRoleEnum = UserRoleEnum.getEnumByValue(updateDto.getUserRole());
            if (!UserRoleEnum.ADMIN.equals(updateRoleEnum) && !UserRoleEnum.USER.equals(updateRoleEnum)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色信息无效");
            }
            updateWrapper.set(User::getUserRole, updateDto.getUserRole());
            isUpdate = true;
        }

        // 如果没有更新，直接返回
        if (!isUpdate) {
            return true;
        }
        // 更新
        return this.update(updateWrapper);
    }

    @Override
    public boolean freezeUsersByIds(List<Long> ids) {
        // 当前登录用户不能冻结
        UserVo loginUser = this.getLoginUser();
        if (ids.contains(loginUser.getUserId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能冻结当前登录用户");
        }
        // 批量更新用户状态为冻结
        return this.lambdaUpdate()
                .in(User::getUserId, ids)
                .eq(User::getUserStatus, UserStatusEnum.ENABLE.getValue())
                .set(User::getUserStatus, UserStatusEnum.DISABLE.getValue())
                .update();
    }

    @Override
    public PageResult<UserVo> pageUsers(UserPageDto pageDto) {
        // 获取参数
        String userAccount = pageDto.getUserAccount();
        String userName = pageDto.getUserName();
        Integer userRole = pageDto.getUserRole();
        Integer userStatus = pageDto.getUserStatus();

        // 校验参数
        int current = pageDto.getCurrent();
        int pageSize = pageDto.getPageSize();
        if (current <= 0 || pageSize <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数异常");
        }

        // 构建查询条件
        IPage<User> page = new Page<>(current, pageSize);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(userAccount), User::getUserAccount, userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), User::getUserName, userName);
        queryWrapper.eq(userRole != null, User::getUserRole, userRole);
        queryWrapper.eq(userStatus != null, User::getUserStatus, userStatus);

        // 查询
        this.page(page, queryWrapper);

        // 封装返回
        long total = page.getTotal();
        List<User> records = page.getRecords();
        if (total == 0) {
            return PageResult.of(total, Collections.emptyList());
        }

        // 脱敏
        List<UserVo> userVoList = records.stream().map(user -> {
            UserVo userVo = new UserVo();
            BeanUtil.copyProperties(user, userVo);
            return userVo;
        }).toList();
        return PageResult.of(total, userVoList);
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




