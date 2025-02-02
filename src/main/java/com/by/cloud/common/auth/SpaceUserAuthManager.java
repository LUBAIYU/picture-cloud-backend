package com.by.cloud.common.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.by.cloud.common.auth.model.SpaceUserAuthConfig;
import com.by.cloud.common.auth.model.SpaceUserRole;
import com.by.cloud.constants.SpaceUserPermissionConstant;
import com.by.cloud.enums.SpaceRoleEnum;
import com.by.cloud.enums.SpaceTypeEnum;
import com.by.cloud.model.entity.Space;
import com.by.cloud.model.entity.SpaceUser;
import com.by.cloud.service.SpaceUserService;
import com.by.cloud.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * @author lzh
 */
@Component
public class SpaceUserAuthManager {

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     *
     * @param spaceUserRole 角色
     * @return 权限列表
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isBlank(spaceUserRole)) {
            return Collections.emptyList();
        }
        // 找到匹配的角色
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> spaceUserRole.equals(r.getKey()))
                .findFirst()
                .orElse(null);
        if (role == null) {
            return Collections.emptyList();
        }
        return role.getPermissions();
    }

    /**
     * 根据空间和用户获取权限列表
     *
     * @param space       空间
     * @param loginUserId 登录用户ID
     * @return 权限列表
     */
    public List<String> getPermissionList(Space space, Long loginUserId) {
        // 管理员权限
        List<String> adminPermission = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (loginUserId != null && userService.isAdmin(loginUserId)) {
                return adminPermission;
            }
            return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
        }
        // 根据空间类型判断权限
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有全部权限
                if (space.getUserId().equals(loginUserId) || userService.isAdmin(loginUserId)) {
                    return adminPermission;
                }
                return Collections.emptyList();
            case TEAM:
                // 团队空间，根据用户角色获取权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUserId)
                        .one();
                if (spaceUser == null) {
                    return Collections.emptyList();
                }
                return getPermissionsByRole(spaceUser.getSpaceRole());
            default:
        }
        return Collections.emptyList();
    }
}
