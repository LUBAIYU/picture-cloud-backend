package com.by.cloud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.common.PageResult;
import com.by.cloud.model.dto.space.SpaceCreateDto;
import com.by.cloud.model.dto.space.SpaceEditDto;
import com.by.cloud.model.dto.space.SpacePageDto;
import com.by.cloud.model.dto.space.SpaceUpdateDto;
import com.by.cloud.model.entity.Space;
import com.by.cloud.model.vo.space.SpaceVo;

import javax.servlet.http.HttpServletRequest;

/**
 * @author lzh
 */
public interface SpaceService extends IService<Space> {

    /**
     * 空间参数校验
     *
     * @param space 空间对象
     * @param isAdd 是否为创建操作
     */
    void validSpace(Space space, boolean isAdd);

    /**
     * 根据空间等级填充空间限额信息
     *
     * @param space 空间
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 根据id删除空间
     *
     * @param id      空间ID
     * @param request 请求对象
     */
    void deleteById(Long id, HttpServletRequest request);

    /**
     * 编辑空间
     *
     * @param editDto 请求信息
     * @param request 请求对象
     */
    void editSpaceById(SpaceEditDto editDto, HttpServletRequest request);

    /**
     * 更新空间（仅管理员可用）
     *
     * @param updateDto 更新信息
     */
    void updateSpaceById(SpaceUpdateDto updateDto);

    /**
     * 根据ID获取空间信息
     *
     * @param id      空间ID
     * @param request 请求对象
     * @return 空间信息
     */
    SpaceVo getSpaceVoById(Long id, HttpServletRequest request);

    /**
     * 分页查询空间（仅管理员）
     *
     * @param pageDto 分页请求
     * @return 分页结果
     */
    PageResult<Space> querySpaceByPage(SpacePageDto pageDto);

    /**
     * 分页查询空间（封装类）
     *
     * @param pageDto 分页请求
     * @return 分页结果
     */
    PageResult<SpaceVo> querySpaceVoByPage(SpacePageDto pageDto);

    /**
     * 创建空间
     *
     * @param addDto  请求参数
     * @param request 请求对象
     * @return 空间ID
     */
    long createSpace(SpaceCreateDto addDto, HttpServletRequest request);

    /**
     * 校验空间权限
     *
     * @param space       空间
     * @param loginUserId 登录用户ID
     * @param isView      是否为查询操作
     */
    void checkSpaceAuth(Space space, Long loginUserId, boolean isView);
}
