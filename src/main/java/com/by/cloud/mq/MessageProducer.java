package com.by.cloud.mq;

import com.by.cloud.constants.MqConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 消息生产者
 *
 * @author lzh
 */
@Slf4j
@Component
public class MessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息到队列中
     *
     * @param message 消息
     */
    public void sendMessage(String message) {
        log.info("send message = {}", message);
        rabbitTemplate.convertAndSend(MqConstant.COMMENT_EXCHANGE, MqConstant.COMMENT_ROUTING_KEY, message);
    }
}
