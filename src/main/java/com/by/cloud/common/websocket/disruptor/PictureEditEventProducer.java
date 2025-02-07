package com.by.cloud.common.websocket.disruptor;

import com.by.cloud.common.websocket.model.PictureEditRequestMessage;
import com.by.cloud.model.entity.User;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * 图片编辑事件生产者
 *
 * @author lzh
 */
@Component
@Slf4j
public class PictureEditEventProducer {

    @Resource
    private Disruptor<PictureEditEvent> pictureEditEventDisruptor;

    /**
     * 发布事件到队列中
     *
     * @param pictureEditRequestMessage 消息
     * @param pictureId                 图片ID
     * @param user                      用户
     * @param session                   当前会话
     */
    public void publishEvent(PictureEditRequestMessage pictureEditRequestMessage, Long pictureId, User user, WebSocketSession session) {
        // 获取RingBuffer
        RingBuffer<PictureEditEvent> ringBuffer = pictureEditEventDisruptor.getRingBuffer();
        // 获取可以生成的位置
        long next = ringBuffer.next();
        PictureEditEvent pictureEditEvent = ringBuffer.get(next);
        pictureEditEvent.setPictureEditRequestMessage(pictureEditRequestMessage);
        pictureEditEvent.setPictureId(pictureId);
        pictureEditEvent.setUser(user);
        pictureEditEvent.setSession(session);
        // 发布事件
        ringBuffer.publish(next);
    }

    /**
     * 优雅停机
     */
    @PreDestroy
    public void destroy() {
        pictureEditEventDisruptor.shutdown();
    }
}
