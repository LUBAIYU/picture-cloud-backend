package com.by.cloud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.common.PageResult;
import com.by.cloud.model.dto.user.UserLoginDto;
import com.by.cloud.model.dto.user.UserPageDto;
import com.by.cloud.model.dto.user.UserRegisterDto;
import com.by.cloud.model.dto.user.UserUpdateDto;
import com.by.cloud.model.entity.User;
import com.by.cloud.model.vo.UserLoginVo;
import com.by.cloud.model.vo.UserVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    /**
     * 上传头像
     *
     * @param multipartFile 文件
     * @return 图片地址
     */
    String uploadAvatar(MultipartFile multipartFile);

    /**
     * 批量删除用户
     *
     * @param ids 用户ID列表
     * @return 是否删除成功
     */
    boolean deleteBatchByIds(List<Long> ids);

    /**
     * 根据ID修改用户信息
     *
     * @param updateDto 请求参数
     * @return 是否修改成功
     */
    boolean updateUserById(UserUpdateDto updateDto);

    /**
     * 批量冻结用户
     *
     * @param ids 用户ID列表
     * @return 是否冻结成功
     */
    boolean freezeUsersByIds(List<Long> ids);

    /**
     * 分页查询用户信息
     *
     * @param pageDto 分页请求参数
     * @return 分页结果
     */
    PageResult<UserVo> pageUsers(UserPageDto pageDto);
}
