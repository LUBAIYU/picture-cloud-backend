package com.by.cloud.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.mapper.PictureLikesMapper;
import com.by.cloud.model.dto.picture.PictureLikesDto;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.entity.PictureLikes;
import com.by.cloud.service.CommentsService;
import com.by.cloud.service.PictureLikesService;
import com.by.cloud.service.PictureService;
import com.by.cloud.service.UserService;
import com.by.cloud.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

/**
 * @author lzh
 */
@Slf4j
@Service
public class PictureLikesServiceImpl extends ServiceImpl<PictureLikesMapper, PictureLikes> implements PictureLikesService {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private CommentsService commentsService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private ExecutorService executorService;

    @Override
    public void thumbOrCancelThumbPicture(PictureLikesDto pictureLikesDto, HttpServletRequest request) {
        // 1.参数校验
        Long pictureId = pictureLikesDto.getPictureId();
        Boolean isLiked = pictureLikesDto.getIsLiked();
        if (pictureId == null || pictureId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (isLiked == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2.判断图片是否存在
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        // 3.根据状态获取不同的 Lua 脚本
        String luaScript = commentsService.getLuaScript(isLiked);
        // 获取登录用户ID
        Long loginUserId = userService.getLoginUserId(request);

        // 4.执行 Lua 脚本获取点赞数
        String userLikeKey = "picture:like:users:" + pictureId;
        String likeCountKey = "picture:like:count:" + pictureId;
        Long likeCount = redisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Long.class),
                Arrays.asList(userLikeKey, likeCountKey),
                loginUserId.toString()
        );

        // 5.判断是否点赞或未点赞
        if (likeCount == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Redis 操作失败");
        }
        if (isLiked && likeCount == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已点赞");
        } else if (!isLiked && likeCount == -1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未点赞，无法取消");
        }

        // 6.异步写入点赞记录
        executorService.submit(() -> persistLikeAsync(pictureId, loginUserId, userLikeKey, likeCountKey, likeCount, isLiked));
    }

    /**
     * 写入点赞记录并更新点赞总数
     *
     * @param pictureId    图片ID
     * @param userId       用户ID
     * @param userLikeKey  用户点赞 Key
     * @param likeCountKey 点赞总数 Key
     * @param likeCount    点赞总数
     * @param isLiked      是否是点赞操作
     */
    public void persistLikeAsync(Long pictureId, Long userId, String userLikeKey, String likeCountKey, Long likeCount, boolean isLiked) {
        try {
            // 设置事务隔离级别为创建新事务
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            // 开启事务
            transactionTemplate.executeWithoutResult(transactionStatus -> {
                if (isLiked) {
                    // 写入点赞记录
                    PictureLikes pictureLikes = new PictureLikes();
                    pictureLikes.setPictureId(pictureId);
                    pictureLikes.setUserId(userId);
                    pictureLikes.setCreateTime(LocalDateTime.now());
                    this.save(pictureLikes);
                } else {
                    // 删除点赞记录
                    this.lambdaUpdate()
                            .eq(PictureLikes::getPictureId, pictureId)
                            .eq(PictureLikes::getUserId, userId)
                            .remove();
                }

                // 更新点赞总数
                Picture picture = new Picture();
                picture.setPicId(pictureId);
                picture.setLikeCount(likeCount);
                picture.setUpdateTime(LocalDateTime.now());
                pictureService.updateById(picture);
            });
        } catch (DuplicateKeyException e) {
            // 唯一索引冲突，防止并发重复插入
            log.error("重复点赞：pictureId={},userId={}", pictureId, userId);
        } catch (Exception e) {
            // 回滚 Redis
            if (isLiked) {
                redisTemplate.opsForSet().remove(userLikeKey, userId);
                redisTemplate.opsForValue().decrement(likeCountKey);
            } else {
                redisTemplate.opsForSet().add(userLikeKey, userId);
                redisTemplate.opsForValue().increment(likeCountKey);
            }
            log.error("持久化失败，已回滚", e);
        }
    }
}




