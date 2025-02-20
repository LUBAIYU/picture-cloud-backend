package com.by.cloud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.common.PageResult;
import com.by.cloud.model.dto.user.UserLoginDto;
import com.by.cloud.model.dto.user.UserPageDto;
import com.by.cloud.model.dto.user.UserRegisterDto;
import com.by.cloud.model.dto.user.UserUpdateDto;
import com.by.cloud.model.entity.User;
import com.by.cloud.model.vo.user.UserVo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author lzh
 */
public interface UserService extends IService<User> {

    /**
     * 获取当前登录用户
     *
     * @param request 请求对象
     * @return 用户信息
     */
    User getLoginUser(HttpServletRequest request);


    /**
     * 获取当前登录用户Id
     *
     * @param request 请求对象
     * @return 登录用户ID
     */
    Long getLoginUserId(HttpServletRequest request);

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
     * @param dto     请求数据
     * @param request 请求对象
     * @return UserVo
     */
    UserVo userLogin(UserLoginDto dto, HttpServletRequest request);

    /**
     * 上传头像
     *
     * @param multipartFile 文件
     * @param request       请求对象
     * @return 图片地址
     */
    String uploadAvatar(MultipartFile multipartFile, HttpServletRequest request);

    /**
     * 批量删除用户
     *
     * @param ids     用户ID列表
     * @param request 请求对象
     * @return 是否删除成功
     */
    boolean deleteBatchByIds(List<Long> ids, HttpServletRequest request);

    /**
     * 根据ID修改用户信息
     *
     * @param updateDto 请求参数
     * @param request   请求对象
     * @return 是否修改成功
     */
    boolean updateUserById(UserUpdateDto updateDto, HttpServletRequest request);

    /**
     * 批量冻结用户
     *
     * @param ids     用户ID列表
     * @param request 请求对象
     * @return 是否冻结成功
     */
    boolean freezeUsersByIds(List<Long> ids, HttpServletRequest request);

    /**
     * 分页查询用户信息
     *
     * @param pageDto 分页请求参数
     * @return 分页结果
     */
    PageResult<UserVo> pageUsers(UserPageDto pageDto);

    /**
     * 判断是否是管理员
     *
     * @param userId 用户ID
     * @return boolean
     */
    boolean isAdmin(Long userId);

    /**
     * 判断是否是管理员
     *
     * @param user 用户信息
     * @return boolean
     */
    boolean isAdmin(User user);

    /**
     * 获取AI助手角色ID
     *
     * @return ID
     */
    long getAiUserId();
}
