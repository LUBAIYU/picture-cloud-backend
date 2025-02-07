package com.by.cloud.common.websocket.disruptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.by.cloud.common.websocket.PictureEditHandler;
import com.by.cloud.common.websocket.enums.PictureEditMessageTypeEnum;
import com.by.cloud.common.websocket.model.PictureEditRequestMessage;
import com.by.cloud.common.websocket.model.PictureEditResponseMessage;
import com.by.cloud.model.entity.User;
import com.by.cloud.model.vo.user.UserVo;
import com.lmax.disruptor.WorkHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * 图片编辑事件处理器
 *
 * @author lzh
 */
@Slf4j
@Component
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    private PictureEditHandler pictureEditHandler;

    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        // 获取消息和公共属性
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        WebSocketSession session = pictureEditEvent.getSession();
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();
        // 获取到消息类别
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum messageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        // 根据消息类型处理消息
        switch (messageTypeEnum) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(pictureId, user);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(pictureEditRequestMessage, pictureId, user, session);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(pictureId, user);
                break;
            default:
                // 构造响应
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("消息类型错误");
                pictureEditResponseMessage.setUser(BeanUtil.copyProperties(user, UserVo.class));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
        }
    }
}
