package com.by.cloud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.model.dto.spaceuser.SpaceUserAddDto;
import com.by.cloud.model.dto.spaceuser.SpaceUserEditDto;
import com.by.cloud.model.dto.spaceuser.SpaceUserQueryDto;
import com.by.cloud.model.entity.SpaceUser;
import com.by.cloud.model.vo.spaceuser.SpaceUserVo;

import java.util.List;

/**
 * @author lzh
 */
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 空间用户关联数据参数校验
     *
     * @param spaceUser 空间用户对象
     * @param isAdd     是否为创建操作
     */
    void validSpaceUser(SpaceUser spaceUser, boolean isAdd);

    /**
     * 根据id删除空间用户关联数据
     *
     * @param id 空间用户ID
     */
    void deleteById(Long id);

    /**
     * 编辑空间用户对象信息
     *
     * @param editDto 请求信息
     */
    void editSpaceUserById(SpaceUserEditDto editDto);

    /**
     * 根据ID获取空间用户关联信息
     *
     * @param id 空间用户ID
     * @return 空间用户关联信息
     */
    SpaceUserVo getSpaceUserVoById(Long id);

    /**
     * 查询空间用户关联数据列表（仅管理员）
     *
     * @param queryDto 分页请求
     * @return 分页结果
     */
    List<SpaceUser> querySpaceList(SpaceUserQueryDto queryDto);

    /**
     * 查询空间用户关联数据列表（封装类）
     *
     * @param queryDto 分页请求
     * @return 分页结果
     */
    List<SpaceUserVo> querySpaceVoList(SpaceUserQueryDto queryDto);

    /**
     * 创建空间用户关联数据
     *
     * @param addDto 请求参数
     * @return 主键ID
     */
    long addSpaceUser(SpaceUserAddDto addDto);
}
