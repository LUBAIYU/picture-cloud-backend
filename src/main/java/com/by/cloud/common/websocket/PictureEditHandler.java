package com.by.cloud.common.websocket;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.by.cloud.common.websocket.disruptor.PictureEditEventProducer;
import com.by.cloud.common.websocket.enums.PictureEditActionEnum;
import com.by.cloud.common.websocket.enums.PictureEditMessageTypeEnum;
import com.by.cloud.common.websocket.model.PictureEditRequestMessage;
import com.by.cloud.common.websocket.model.PictureEditResponseMessage;
import com.by.cloud.model.entity.User;
import com.by.cloud.model.vo.user.UserVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lzh
 */
@Slf4j
@Component
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    @Lazy
    private PictureEditEventProducer pictureEditEventProducer;

    /**
     * 正在编辑的图片集合，key为图片id，value为用户id
     */
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    /**
     * 保存所有连接的会话，key为图片id，value为会话集合
     */
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * 建立连接成功
     *
     * @param session 会话
     * @throws Exception 异常
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        // 获取 session 中的属性
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        // 初始化 pictureSessions
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);

        // 构造响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户 %s 加入编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(BeanUtil.copyProperties(user, UserVo.class));

        // 广播消息
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        // 将消息转换为 PictureEditRequestMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        // 获取公共属性
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 生产消息到 Disruptor 环形队列中
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, pictureId, user, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        // 获取公共属性
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 移除当前用户的编辑状态
        handleExitEditMessage(pictureId, user);
        // 删除会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
        // 构造响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户 %s 离开编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(BeanUtil.copyProperties(user, UserVo.class));
        // 广播
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 处理退出编辑的消息
     *
     * @param pictureId 图片ID
     * @param user      用户
     */
    public void handleExitEditMessage(Long pictureId, User user) throws IOException {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getUserId())) {
            // 移除当前编辑用户
            pictureEditingUsers.remove(pictureId);
            // 构造响应，发送退出编辑通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("用户 %s 退出编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(BeanUtil.copyProperties(user, UserVo.class));
            // 广播
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 处理编辑操作的消息
     *
     * @param pictureEditRequestMessage 编辑操作消息
     * @param pictureId                 图片ID
     * @param user                      用户
     * @param session                   会话
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, Long pictureId, User user, WebSocketSession session) throws IOException {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            log.error("无效的编辑动作");
            return;
        }
        // 确定是获得编辑权限的用户才能编辑
        if (editingUserId != null && editingUserId.equals(user.getUserId())) {
            // 构造响应
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("用户 %s 执行 %s", user.getUserName(), actionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUser(BeanUtil.copyProperties(user, UserVo.class));
            // 广播给除了当前客户端的其他客户端，防止重复编辑
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    /**
     * 处理进入编辑状态的消息
     *
     * @param pictureId 图片ID
     * @param user      用户信息
     */
    public void handleEnterEditMessage(Long pictureId, User user) throws IOException {
        // 如果当前没有用户在编辑，才能进入编辑
        if (!pictureEditingUsers.containsKey(pictureId)) {
            // 将当前用户加入编辑
            pictureEditingUsers.put(pictureId, user.getUserId());
            // 构造响应消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("用户 %s 开始编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(BeanUtil.copyProperties(user, UserVo.class));
            // 广播消息
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 给指定图片的所有会话广播消息
     *
     * @param pictureId                  图片ID
     * @param pictureEditResponseMessage 消息
     * @param excludeSession             排除掉的会话
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession) throws IOException {
        // 获取 session 集合
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isEmpty(sessionSet)) {
            return;
        }

        // 自定义序列化器，将 Long 类型转为 String
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(module);
        // 序列化为 json 字符串
        String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
        TextMessage textMessage = new TextMessage(message);

        // 对每个 session 发送消息
        for (WebSocketSession session : sessionSet) {
            if (excludeSession != null && excludeSession.equals(session)) {
                continue;
            }
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        }
    }

    /**
     * 给指定图片的所有会话广播消息
     *
     * @param pictureId                  图片ID
     * @param pictureEditResponseMessage 消息
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws IOException {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }
}
