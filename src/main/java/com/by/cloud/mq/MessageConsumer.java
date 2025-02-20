package com.by.cloud.mq;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.by.cloud.api.baiduai.BaiduAiApi;
import com.by.cloud.api.baiduai.model.TextAuditResponse;
import com.by.cloud.constants.CommentConstant;
import com.by.cloud.constants.MqConstant;
import com.by.cloud.enums.CommentReviewStatusEnum;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.ReviewResultStatusEnum;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.model.entity.CommentReviews;
import com.by.cloud.model.entity.Comments;
import com.by.cloud.service.CommentReviewsService;
import com.by.cloud.service.CommentsService;
import com.by.cloud.service.UserService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.shaded.com.google.common.util.concurrent.RateLimiter;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 消息消费者
 *
 * @author lzh
 */
@Slf4j
@Component
public class MessageConsumer {

    @Resource
    private UserService userService;

    @Resource
    private CommentsService commentsService;

    @Resource
    private CommentReviewsService commentReviewsService;

    @Resource
    private BaiduAiApi baiduAiApi;

    /**
     * 限流器，QPS 1
     */
    public static final RateLimiter RATE_LIMITER = RateLimiter.create(1);

    @Transactional(rollbackFor = Exception.class)
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstant.COMMENT_QUEUE),
            exchange = @Exchange(name = MqConstant.COMMENT_EXCHANGE),
            key = MqConstant.COMMENT_ROUTING_KEY
    ), ackMode = "MANUAL")
    public void handleMessage(String message, Channel channel,
                              @Header(value = AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("received message = {}", message);
        Comments comments = null;
        long aiUserId = 0L;
        try {
            // 1. 判断消息是否丢失
            if (StrUtil.isBlank(message)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息为空");
            }

            // 限流检测
            if (!RATE_LIMITER.tryAcquire()) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "服务繁忙，请稍后重试");
            }

            // 2. 查询评论内容，修改评论状态为审核中
            long commentsId = Long.parseLong(message);
            comments = commentsService.getById(commentsId);
            comments.setStatus(CommentReviewStatusEnum.REVIEWING.getValue());
            boolean updated = commentsService.updateById(comments);
            if (!updated) {
                log.error("审核中状态更新失败");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "审核中状态更新失败");
            }

            // 3. 调用百度AI对内容进行审核
            String content = comments.getContent();
            TextAuditResponse textAuditResponse = baiduAiApi.aiTextAudit(content);
            String conclusion = textAuditResponse.getConclusion();

            // 4. 根据审核结果，修改评论的状态以及写入不同的审核记录
            aiUserId = userService.getAiUserId();
            CommentReviews commentReviews = new CommentReviews();
            commentReviews.setCommentId(commentsId);
            commentReviews.setReviewerId(aiUserId);
            commentReviews.setReviewTime(LocalDateTime.now());
            if (CommentConstant.COMMENT_REVIEW_PASS.equals(conclusion)) {
                commentReviews.setReviewMsg("AI审核通过");
                commentReviews.setReviewStatus(ReviewResultStatusEnum.APPROVE.getValue());
                comments.setStatus(CommentReviewStatusEnum.PASS.getValue());
            } else {
                commentReviews.setReviewMsg("AI审核拒绝");
                commentReviews.setReviewStatus(ReviewResultStatusEnum.REJECT.getValue());
                comments.setStatus(CommentReviewStatusEnum.REJECT.getValue());
            }
            // 修改评论状态
            boolean update = commentsService.updateById(comments);
            if (!update) {
                log.error("审核通过状态更新失败");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "审核通过状态更新失败");
            }
            // 写入审核记录
            boolean saved = commentReviewsService.save(commentReviews);
            if (!saved) {
                log.error("审核记录写入失败");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "审核记录写入失败");
            }

            // 5. 手动确认消息
            channel.basicAck(deliveryTag, false);

        } catch (BusinessException be) {
            log.error("消息处理失败，原因：{}", be.getMessage());
            if (comments != null) {
                handleError(comments.getId(), be.getMessage(), aiUserId);
            }
            // 手动拒绝消息
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("数据库异常", e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 处理审核失败结果
     *
     * @param commentId 评论ID
     * @param message   错误消息
     * @param aiUserId  AI审核用户ID
     */
    private void handleError(Long commentId, String message, Long aiUserId) {
        try {
            // 回滚评论的审核状态为已提交
            commentsService.lambdaUpdate()
                    .eq(Comments::getId, commentId)
                    .eq(Comments::getStatus, CommentReviewStatusEnum.REVIEWING.getValue())
                    .set(Comments::getStatus, CommentReviewStatusEnum.SUBMITTED.getValue())
                    .update();
            // 如果AI审核用户不存在，则查询
            if (ObjectUtil.isEmpty(aiUserId)) {
                aiUserId = userService.getAiUserId();
            }
            // 写入审核失败的审核记录
            CommentReviews commentReviews = new CommentReviews();
            commentReviews.setCommentId(commentId);
            commentReviews.setReviewMsg(message);
            commentReviews.setReviewerId(aiUserId);
            commentReviews.setReviewStatus(ReviewResultStatusEnum.FAILED.getValue());
            commentReviews.setReviewTime(LocalDateTime.now());
            commentReviewsService.save(commentReviews);
        } catch (Exception e) {
            log.error("处理审核失败时发生异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "审核失败处理异常");
        }
    }
}
