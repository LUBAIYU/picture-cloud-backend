package com.by.cloud.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.common.BaseContext;
import com.by.cloud.common.PageResult;
import com.by.cloud.common.auth.SpaceUserAuthManager;
import com.by.cloud.constants.SpaceConstant;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.SpaceLevelEnum;
import com.by.cloud.enums.SpaceRoleEnum;
import com.by.cloud.enums.SpaceTypeEnum;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.mapper.SpaceMapper;
import com.by.cloud.model.dto.space.SpaceCreateDto;
import com.by.cloud.model.dto.space.SpaceEditDto;
import com.by.cloud.model.dto.space.SpacePageDto;
import com.by.cloud.model.dto.space.SpaceUpdateDto;
import com.by.cloud.model.entity.Space;
import com.by.cloud.model.entity.SpaceUser;
import com.by.cloud.model.entity.User;
import com.by.cloud.model.vo.space.SpaceVo;
import com.by.cloud.model.vo.user.UserVo;
import com.by.cloud.service.SpaceService;
import com.by.cloud.service.SpaceUserService;
import com.by.cloud.service.UserService;
import com.by.cloud.utils.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lzh
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public void validSpace(Space space, boolean isAdd) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 取出校验参数
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        // 如果是创建操作，执行以下校验
        if (isAdd) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevelEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
            if (spaceTypeEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
            }
        }
        // 修改数据校验
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        // 修改数据时，如果要改空间级别
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不存在");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > SpaceConstant.SPACE_NAME_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            Long maxSize = space.getMaxSize();
            if (maxSize == null) {
                space.setMaxSize(spaceLevelEnum.getMaxSize());
            }
            Long maxCount = space.getMaxCount();
            if (maxCount == null) {
                space.setMaxCount(spaceLevelEnum.getMaxCount());
            }
        }
    }

    @Override
    public void deleteById(Long id) {
        // 判断空间是否存在
        Space space = this.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 判断登录用户是否有权限删除
        Long loginUserId = BaseContext.getLoginUserId();
        checkSpaceAuth(space, loginUserId);
        // 删除
        boolean removed = this.removeById(id);
        ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void editSpaceById(SpaceEditDto editDto) {
        // 校验参数
        Long id = editDto.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Space space = new Space();
        BeanUtil.copyProperties(editDto, space);
        this.validSpace(space, false);

        // 判断是否存在
        Space dbSpace = this.getById(id);
        ThrowUtils.throwIf(dbSpace == null, ErrorCode.NOT_FOUND_ERROR);

        // 判断权限，仅空间创建人可编辑
        Long loginUserId = BaseContext.getLoginUserId();
        if (!loginUserId.equals(dbSpace.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 更新操作
        this.updateById(space);
    }

    @Override
    public void updateSpaceById(SpaceUpdateDto updateDto) {
        // 校验参数
        Long id = updateDto.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 自动填充参数
        Space space = new Space();
        BeanUtil.copyProperties(updateDto, space);
        this.fillSpaceBySpaceLevel(space);
        // 数据校验
        this.validSpace(space, false);
        // 判断是否存在
        Space dbSpace = this.getById(id);
        ThrowUtils.throwIf(dbSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 更新
        boolean updated = this.updateById(space);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public SpaceVo getSpaceVoById(Long id) {
        // 判断是否存在
        Space space = this.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

        // 判断是否有权限
        Long loginUserId = BaseContext.getLoginUserId();
        checkSpaceAuth(space, loginUserId);

        // 获取创建的用户信息
        User user = userService.getById(space.getUserId());
        UserVo userVo = null;
        if (user != null) {
            userVo = BeanUtil.copyProperties(user, UserVo.class);
        }
        SpaceVo spaceVo = SpaceVo.objToVo(space);
        spaceVo.setUserVo(userVo);

        // 获取权限列表
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUserId);
        spaceVo.setPermissionList(permissionList);

        return spaceVo;
    }

    @Override
    public PageResult<Space> querySpaceByPage(SpacePageDto pageDto) {
        // 获取参数
        int current = pageDto.getCurrent();
        int pageSize = pageDto.getPageSize();
        Long userId = pageDto.getUserId();
        String spaceName = pageDto.getSpaceName();
        Integer spaceLevel = pageDto.getSpaceLevel();
        Integer spaceType = pageDto.getSpaceType();

        // 校验参数
        ThrowUtils.throwIf(current <= 0 || pageSize <= 0, ErrorCode.PARAMS_ERROR);

        // 构建分页条件
        IPage<Space> page = new Page<>(current, pageSize);
        // 构建查询条件
        LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(spaceName), Space::getSpaceName, spaceName);
        queryWrapper.eq(userId != null, Space::getUserId, userId);
        queryWrapper.eq(spaceLevel != null, Space::getSpaceLevel, spaceLevel);
        queryWrapper.eq(spaceType != null, Space::getSpaceType, spaceType);
        queryWrapper.orderByDesc(Space::getCreateTime);

        // 查询
        this.page(page, queryWrapper);

        // 返回
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public PageResult<SpaceVo> querySpaceVoByPage(SpacePageDto pageDto) {
        // 先查询所有信息
        PageResult<Space> pageResult = this.querySpaceByPage(pageDto);

        // 如果为空直接返回
        List<Space> records = pageResult.getRecords();
        if (CollUtil.isEmpty(records)) {
            return PageResult.of(0L, Collections.emptyList());
        }

        // 对象列表 => VO列表
        List<SpaceVo> spaceVoList = records.stream().map(SpaceVo::objToVo).toList();

        // 关联查询用户信息
        // 获取去重的用户ID列表
        Set<Long> userIdSet = records.stream().map(Space::getUserId).collect(Collectors.toSet());
        // 查询用户ID列表对应的用户信息
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getUserId));

        // 设置用户信息
        spaceVoList.forEach(spaceVo -> {
            Long userId = spaceVo.getUserId();
            if (userIdUserListMap.containsKey(userId)) {
                User user = userIdUserListMap.get(userId).get(0);
                spaceVo.setUserVo(BeanUtil.copyProperties(user, UserVo.class));
            }
        });

        // 封装返回
        return PageResult.of(pageResult.getTotal(), spaceVoList);
    }

    @Override
    public long createSpace(SpaceCreateDto addDto) {
        // 1.填充默认参数
        Space space = new Space();
        BeanUtil.copyProperties(addDto, space);
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        // 如果没传空间类型，默认为私有空间
        if (space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 填充默认级别和大小
        this.fillSpaceBySpaceLevel(space);
        // 2.校验参数
        this.validSpace(space, true);
        // 3.校验权限，非管理员只能创建普通级别的空间
        Long loginUserId = BaseContext.getLoginUserId();
        space.setUserId(loginUserId);
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }

        // 4.控制同一个用户只能创建一个私有空间或一个团队空间,intern()保证锁是同一个对象
        String lock = String.valueOf(loginUserId).intern();
        synchronized (lock) {
            Long spaceId = transactionTemplate.execute(status -> {
                // 判断是否已存在空间
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, loginUserId)
                        .eq(Space::getSpaceType, space.getSpaceType())
                        .exists();
                // 如果存在则抛异常
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户只能创建一个私有空间");
                // 插入数据
                boolean saved = this.save(space);
                ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "保存空间到数据库失败");
                // 如果是团队空间，关联新增团队成员记录
                if (space.getSpaceType() == SpaceTypeEnum.TEAM.getValue()) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setUserId(loginUserId);
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    boolean result = spaceUserService.save(spaceUser);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                }
                return space.getId();
            });
            // 如果为空返回-1
            return Optional.ofNullable(spaceId).orElse(-1L);
        }
    }

    @Override
    public void checkSpaceAuth(Space space, Long loginUserId) {
        if (!loginUserId.equals(space.getUserId()) && !userService.isAdmin(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }
}




