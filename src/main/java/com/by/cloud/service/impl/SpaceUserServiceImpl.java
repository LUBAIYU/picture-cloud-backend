package com.by.cloud.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.SpaceRoleEnum;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.mapper.SpaceUserMapper;
import com.by.cloud.model.dto.spaceuser.SpaceUserAddDto;
import com.by.cloud.model.dto.spaceuser.SpaceUserEditDto;
import com.by.cloud.model.dto.spaceuser.SpaceUserQueryDto;
import com.by.cloud.model.entity.Space;
import com.by.cloud.model.entity.SpaceUser;
import com.by.cloud.model.entity.User;
import com.by.cloud.model.vo.space.SpaceVo;
import com.by.cloud.model.vo.spaceuser.SpaceUserVo;
import com.by.cloud.model.vo.user.UserVo;
import com.by.cloud.service.SpaceService;
import com.by.cloud.service.SpaceUserService;
import com.by.cloud.service.UserService;
import com.by.cloud.utils.ThrowUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author lzh
 */
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private SpaceService spaceService;

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean isAdd) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 取出校验参数
        Long userId = spaceUser.getUserId();
        Long spaceId = spaceUser.getSpaceId();
        // 如果是创建操作，执行以下校验
        if (isAdd) {
            ThrowUtils.throwIf(ObjectUtil.hasEmpty(userId, spaceId), ErrorCode.PARAMS_ERROR);
            // 判断用户是否存在
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在");
            // 判断空间是否存在
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
        }
        // 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

    @Override
    public void deleteById(Long id) {
        // 判断数据是否存在
        SpaceUser spaceUser = this.getById(id);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 删除
        boolean removed = this.removeById(id);
        ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void editSpaceUserById(SpaceUserEditDto editDto) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(editDto, spaceUser);
        // 参数校验
        validSpaceUser(spaceUser, false);
        Long id = spaceUser.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        // 判断数据是否存在
        SpaceUser dbSpaceUser = this.getById(id);
        ThrowUtils.throwIf(dbSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 更新
        boolean result = this.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public SpaceUserVo getSpaceUserVoById(Long id) {
        // 判断数据是否存在
        SpaceUser spaceUser = this.getById(id);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 关联查询用户信息
        SpaceUserVo spaceUserVo = SpaceUserVo.objToVo(spaceUser);
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            spaceUserVo.setUserVo(BeanUtil.copyProperties(user, UserVo.class));
        }
        // 关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            spaceUserVo.setSpaceVo(BeanUtil.copyProperties(space, SpaceVo.class));
        }
        return spaceUserVo;
    }

    @Override
    public List<SpaceUser> querySpaceList(SpaceUserQueryDto queryDto) {
        // 获取参数
        Long id = queryDto.getId();
        Long userId = queryDto.getUserId();
        Long spaceId = queryDto.getSpaceId();
        String spaceRole = queryDto.getSpaceRole();

        // 构建查询条件
        LambdaQueryWrapper<SpaceUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(id != null, SpaceUser::getId, id);
        queryWrapper.eq(userId != null, SpaceUser::getUserId, userId);
        queryWrapper.eq(spaceId != null, SpaceUser::getSpaceId, spaceId);
        queryWrapper.eq(StrUtil.isNotBlank(spaceRole), SpaceUser::getSpaceRole, spaceRole);
        queryWrapper.orderByDesc(SpaceUser::getCreateTime);

        // 查询
        return this.list(queryWrapper);
    }

    @Override
    public List<SpaceUserVo> querySpaceVoList(SpaceUserQueryDto queryDto) {
        // 先查询所有信息
        List<SpaceUser> spaceUserList = this.querySpaceList(queryDto);

        // 如果为空直接返回
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }

        // 对象列表 => VO列表
        List<SpaceUserVo> spaceUserVoList = spaceUserList.stream().map(SpaceUserVo::objToVo).toList();

        // 关联查询用户信息
        // 获取去重的用户ID列表和空间ID列表
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());

        // 查询用户ID列表对应的用户信息
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getUserId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet)
                .stream()
                .collect(Collectors.groupingBy(Space::getId));

        // 设置用户信息和空间信息
        spaceUserVoList.forEach(spaceUserVo -> {
            Long userId = spaceUserVo.getUserId();
            Long spaceId = spaceUserVo.getSpaceId();
            // 用户信息
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVo.setUserVo(BeanUtil.copyProperties(user, UserVo.class));
            // 空间信息
            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVo.setSpaceVo(SpaceVo.objToVo(space));
        });

        // 封装返回
        return spaceUserVoList;
    }

    @Override
    public long addSpaceUser(SpaceUserAddDto addDto) {
        // 参数校验
        ThrowUtils.throwIf(addDto == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(addDto, spaceUser);
        validSpaceUser(spaceUser, true);
        // 写入数据库
        boolean saved = this.save(spaceUser);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }
}




