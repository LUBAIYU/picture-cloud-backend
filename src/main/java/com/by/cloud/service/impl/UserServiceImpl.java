package com.by.cloud.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.common.FileManager;
import com.by.cloud.common.PageResult;
import com.by.cloud.common.auth.StpKit;
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
import com.by.cloud.model.vo.user.UserVo;
import com.by.cloud.service.UserService;
import com.by.cloud.utils.ThrowUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

/**
 * @author lzh
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Value("${user.encrypt.salt}")
    private String salt;

    @Resource
    private FileManager fileManager;

    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object object = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) object;
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        return user;
    }

    @Override
    public Long getLoginUserId(HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        return loginUser.getUserId();
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
    public UserVo userLogin(UserLoginDto dto, HttpServletRequest request) {
        // 请求参数
        String userAccount = dto.getUserAccount();
        String userPassword = dto.getUserPassword();
        String encryptPassword = getEncryptPassword(userPassword);

        // 校验参数
        ThrowUtils.throwIf(StringUtils.isAnyBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号长度不能小于4位");
        checkPassword(userPassword, null);

        // 校验账号和密码
        User user = this.lambdaQuery().eq(User::getUserAccount, userAccount).one();
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "账号或密码错误");
        if (!user.getUserPassword().equals(encryptPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }

        // 判断用户是否被冻结
        UserStatusEnum statusEnum = UserStatusEnum.getEnumByValue(user.getUserStatus());
        if (UserStatusEnum.DISABLE.equals(statusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号被冻结，请联系管理员");
        }

        // 保存用户的登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);

        // 保存用户登录态到 Sa-Token
        StpKit.SPACE.login(user.getUserId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);

        return BeanUtil.copyProperties(user, UserVo.class);
    }

    @Override
    public String uploadAvatar(MultipartFile multipartFile, HttpServletRequest request) {
        // 获取登录用户
        User loginUser = this.getLoginUser(request);
        // 构造地址前缀
        String uploadPathPrefix = String.format("avatar/%s", loginUser.getUserId());
        // 上传图片
        return fileManager.uploadAvatar(multipartFile, uploadPathPrefix);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public boolean deleteBatchByIds(List<Long> ids, HttpServletRequest request) {
        // 获取登录用户
        User loginUser = this.getLoginUser(request);
        // 如果ID中包含登录用户的ID，则不能删除
        if (ids.contains(loginUser.getUserId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能删除当前登录用户");
        }
        return this.removeBatchByIds(ids);
    }

    @Override
    public boolean updateUserById(UserUpdateDto updateDto, HttpServletRequest request) {
        // 校验参数
        Long userId = updateDto.getUserId();
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "用户ID不能为空");

        // 如果账号不为空，账号长度不能小于4位
        String userAccount = updateDto.getUserAccount();
        if (StrUtil.isNotBlank(userAccount) && userAccount.length() < UserConstant.USER_ACCOUNT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度不能小于4位");
        }

        // 获取登录用户
        User loginUser = this.getLoginUser(request);

        // 如果有传用户角色，则进行校验
        Integer userRole = updateDto.getUserRole();
        if (userRole != null) {
            // 获取当前登录用户的角色
            UserRoleEnum roleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
            // 校验角色是否有效
            UserRoleEnum newRoleEnum = UserRoleEnum.getEnumByValue(userRole);
            if (newRoleEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效角色信息");
            }
            // 如果当前登录用户不是管理员，则只能传递普通用户角色
            if (UserRoleEnum.USER.equals(roleEnum) && UserRoleEnum.ADMIN.equals(newRoleEnum)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

        // 更新
        User user = BeanUtil.copyProperties(updateDto, User.class);
        return this.updateById(user);
    }

    @Override
    public boolean freezeUsersByIds(List<Long> ids, HttpServletRequest request) {
        // 当前登录用户不能冻结
        User loginUser = this.getLoginUser(request);
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

    @Override
    public boolean isAdmin(Long userId) {
        User user = this.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        Integer userRole = user.getUserRole();
        UserRoleEnum roleEnum = UserRoleEnum.getEnumByValue(userRole);
        return UserRoleEnum.ADMIN.equals(roleEnum);
    }

    @Override
    public boolean isAdmin(User user) {
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        Integer userRole = user.getUserRole();
        UserRoleEnum roleEnum = UserRoleEnum.getEnumByValue(userRole);
        return UserRoleEnum.ADMIN.equals(roleEnum);
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




