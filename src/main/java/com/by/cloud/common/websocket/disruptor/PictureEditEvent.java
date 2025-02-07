package com.by.cloud.common.websocket.disruptor;

import com.by.cloud.common.websocket.model.PictureEditRequestMessage;
import com.by.cloud.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 图片编辑事件
 *
 * @author lzh
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片ID
     */
    private Long pictureId;
}
