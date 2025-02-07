package com.by.cloud.common.websocket;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.by.cloud.common.auth.SpaceUserAuthManager;
import com.by.cloud.constants.SpaceUserPermissionConstant;
import com.by.cloud.enums.SpaceTypeEnum;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.entity.Space;
import com.by.cloud.model.entity.User;
import com.by.cloud.service.PictureService;
import com.by.cloud.service.SpaceService;
import com.by.cloud.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 拦截器
 *
 * @author lzh
 */
@Slf4j
@Component
public class WsHandShakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 握手之前的执行逻辑
     *
     * @param request    请求
     * @param response   响应
     * @param wsHandler  执行器
     * @param attributes 属性
     * @return 是否握手
     * @throws Exception 异常
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            // 获取请求对象
            HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            // 获取请求参数
            String pictureId = httpServletRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureId)) {
                log.error("缺少图片参数，拒绝握手");
                return false;
            }
            // 判断用户是否登录
            User loginUser = userService.getLoginUser(httpServletRequest);
            if (ObjectUtil.isEmpty(loginUser)) {
                log.error("用户未登录，拒绝握手");
                return false;
            }
            // 判断用户是否有编辑的权限
            Picture picture = pictureService.getById(pictureId);
            if (ObjectUtil.isEmpty(picture)) {
                log.error("图片不存在，拒绝握手");
            }
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null) {
                space = spaceService.getById(spaceId);
                if (ObjectUtil.isEmpty(space)) {
                    log.error("空间不存在，拒绝握手");
                    return false;
                }
                if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                    log.error("不是团队空间，拒绝握手");
                    return false;
                }
            }
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser.getUserId());
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("用户没有编辑权限，拒绝握手");
                return false;
            }
            // 设置属性
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getUserId());
            attributes.put("pictureId", Long.parseLong(pictureId));
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
