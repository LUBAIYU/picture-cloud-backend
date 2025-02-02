package com.by.cloud.common.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.by.cloud.common.auth.model.SpaceUserAuthContext;
import com.by.cloud.constants.SpaceUserPermissionConstant;
import com.by.cloud.constants.UserConstant;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.SpaceRoleEnum;
import com.by.cloud.enums.SpaceTypeEnum;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.entity.Space;
import com.by.cloud.model.entity.SpaceUser;
import com.by.cloud.model.entity.User;
import com.by.cloud.service.PictureService;
import com.by.cloud.service.SpaceService;
import com.by.cloud.service.SpaceUserService;
import com.by.cloud.service.UserService;
import com.by.cloud.utils.ThrowUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 自定义权限加载接口实现类
 *
 * @author lzh
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 校验登录类型，如果 loginType 不是 space,直接返回空权限列表
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return Collections.emptyList();
        }

        // 管理员权限列表
        List<String> adminPermissions = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());

        // 如果当前用户为管理员，则返回管理员权限列表
        if (userService.isAdmin((Long) loginId)) {
            return adminPermissions;
        }

        // 获取上下文对象，如果所有字段都为空，视为公共图库，返回管理员权限列表
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        if (isAllFieldsNull(authContext)) {
            return adminPermissions;
        }

        // 校验用户是否登录
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(UserConstant.USER_LOGIN_STATE);
        if (ObjectUtil.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 判断是否有 SpaceUser 对象，如果有直接根据角色获取权限
        Long loginUserId = loginUser.getUserId();
        // 是否为管理员
        boolean isAdmin = userService.isAdmin(loginUserId);
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }

        // 判断是否有 SpaceUserId，如果有说明是团队空间
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            // 获取空间用户信息
            spaceUser = spaceUserService.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
            }
            // 校验登录用户是否属于该空间，如果不是，返回空权限
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, loginUserId)
                    .one();
            if (loginSpaceUser == null) {
                return Collections.emptyList();
            }
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }

        // 根据 SpaceId 或者 PictureId 校验权限
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            Long pictureId = authContext.getPictureId();
            // 图片ID也没有，默认通过权限校验
            if (pictureId == null) {
                return adminPermissions;
            }
            // 根据图片获取空间ID
            Picture picture = pictureService.getById(pictureId);
            ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            spaceId = picture.getSpaceId();
            // spaceId 为空，是公共图库，仅图片创建者或管理员可操作
            if (spaceId == null) {
                if (picture.getUserId().equals(loginUserId) || isAdmin) {
                    return adminPermissions;
                }
                // 不是自己的图片，仅查看
                return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
            }
        }

        // 已经获取到 spaceId，开始校验权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");

        // 根据 Space 类型判断权限
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            // 私有空间，只有创建者或管理员可操作
            if (space.getUserId().equals(loginUserId) || isAdmin) {
                return adminPermissions;
            }
            return Collections.emptyList();
        }

        // 团队空间，查询用户角色获取权限
        spaceUser = spaceUserService.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, loginUserId)
                .one();
        if (spaceUser == null) {
            return Collections.emptyList();
        }
        return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return List.of();
    }

    /**
     * 从请求中获取上下文参数对象
     *
     * @return 上下文对象
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        // 获取请求对象
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        SpaceUserAuthContext authContext;

        // 区分是 Post、Put等请求还是 Get 请求
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        if (ContentType.JSON.getValue().equals(contentType)) {
            String body = ServletUtil.getBody(request);
            authContext = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authContext = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }

        // 根据请求路径区分 id 的含义
        Long id = authContext.getId();
        if (ObjectUtil.isNotNull(id)) {
            // 将请求前缀前的 /api/ 去掉
            String requestUri = request.getRequestURI();
            String partUri = requestUri.replace(contextPath + "/", "");
            // 获取确定的请求前缀，如 picture/add?aaa  =>  picture
            String moduleName = StrUtil.subBefore(partUri, "/", false);
            // 根据不同模块，填充不同的参数ID
            switch (moduleName) {
                case "picture":
                    authContext.setPictureId(id);
                    break;
                case "space":
                    authContext.setSpaceId(id);
                    break;
                case "spaceUser":
                    authContext.setSpaceUserId(id);
                    break;
                default:
            }
        }
        return authContext;
    }

    /**
     * 判断所有字段是否为空
     *
     * @param object 对象
     * @return 是否为空
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true;
        }
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                .map(field -> ReflectUtil.getFieldValue(object, field))
                .allMatch(ObjectUtil::isEmpty);
    }
}
