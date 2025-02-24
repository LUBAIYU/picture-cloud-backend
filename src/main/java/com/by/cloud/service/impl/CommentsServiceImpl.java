package com.by.cloud.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.enums.CommentReviewStatusEnum;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.ReviewResultStatusEnum;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.mapper.CommentsMapper;
import com.by.cloud.model.dto.comment.CommentPageDto;
import com.by.cloud.model.dto.comment.CommentPublishDto;
import com.by.cloud.model.dto.comment.CommentReviewDto;
import com.by.cloud.model.entity.*;
import com.by.cloud.model.vo.comment.CommentsViewVo;
import com.by.cloud.model.vo.user.UserVo;
import com.by.cloud.mq.MessageProducer;
import com.by.cloud.service.*;
import com.by.cloud.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @author lzh
 */
@Service
@Slf4j
public class CommentsServiceImpl extends ServiceImpl<CommentsMapper, Comments> implements CommentsService {

    @Resource
    private UserService userService;

    @Lazy
    @Resource
    private PictureService pictureService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ExecutorService executorService;

    @Resource
    private CommentLikesService commentLikesService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private MessageProducer messageProducer;

    @Resource
    private CommentReviewsService commentReviewsService;


    @Override
    public long publishComments(CommentPublishDto commentPublishDto, HttpServletRequest request) {
        // 1. 参数校验
        Long picId = commentPublishDto.getPicId();
        String content = commentPublishDto.getContent();
        Long parentId = commentPublishDto.getParentId();
        if (picId == null || picId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR);

        // 2. 获取登录用户ID
        Long loginUserId = userService.getLoginUserId(request);

        // 3. 判断图片是否存在
        Picture picture = pictureService.getById(picId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 4. 如果有传父级评论ID，判断父级评论是否存在
        if (parentId != null && parentId > 0) {
            Comments comment = this.getById(parentId);
            ThrowUtils.throwIf(comment == null, ErrorCode.NOT_FOUND_ERROR, "父级评论不存在");
        }

        // 5. 设置评论审核状态为待审核
        Comments comments = new Comments();
        comments.setPicId(picId);
        comments.setContent(content);
        comments.setParentId(parentId);
        comments.setUserId(loginUserId);
        comments.setStatus(CommentReviewStatusEnum.SUBMITTED.getValue());

        // 6. 写入评论数据
        boolean saved = this.save(comments);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR);

        // 7. 发送消息到消息队列，异步审核
        long commentsId = comments.getId();
        messageProducer.sendMessage(String.valueOf(commentsId));

        // 8. 返回评论ID
        return commentsId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommentsById(Long id, HttpServletRequest request) {
        // 1. 判断用户是否已登录并获取用户ID
        Long loginUserId = userService.getLoginUserId(request);

        // 2. 判断评论是否是当前登录用户所发布
        Comments comments = this.getById(id);
        if (comments == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!comments.getUserId().equals(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 3.递归删除子级评论
        deleteChildrenComments(id);

        // 4.删除当前评论
        boolean removed = this.removeById(id);
        ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR, "删除当前评论失败");
    }

    @Override
    public IPage<CommentsViewVo> queryCommentsByPage(CommentPageDto commentPageDto) {
        // 1. 获取参数
        int current = commentPageDto.getCurrent();
        int pageSize = commentPageDto.getPageSize();
        String content = commentPageDto.getContent();
        Long picId = commentPageDto.getPicId();
        Long createUserId = commentPageDto.getUserId();
        Integer status = commentPageDto.getStatus();

        // 2. 分页查询所有评论
        IPage<Comments> page = new Page<>(current, pageSize);
        LambdaQueryWrapper<Comments> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(content), Comments::getContent, content);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picId), Comments::getPicId, picId);
        queryWrapper.eq(ObjectUtil.isNotEmpty(createUserId), Comments::getUserId, createUserId);
        queryWrapper.eq(ObjectUtil.isNotEmpty(status), Comments::getStatus, status);
        queryWrapper.orderByDesc(Comments::getCreateTime);
        page = this.page(page, queryWrapper);

        // 获取总数和评论列表
        long total = page.getTotal();
        List<Comments> commentsList = page.getRecords();
        if (total == 0 || CollUtil.isEmpty(commentsList)) {
            return new Page<>(current, pageSize, 0);
        }

        // 3. 批量获取用户信息
        // 获取用户ID集合
        Set<Long> userIdSet = commentsList.stream()
                .map(Comments::getUserId)
                .collect(Collectors.toSet());
        // 获取用户ID对应的用户信息集合
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getUserId));

        // 4.封装顶级评论为VO并关联用户信息
        // 保存每条顶级评论的VO
        List<CommentsViewVo> rootCommentsViewVoList = new ArrayList<>();
        // 保存每条评论的VO
        Map<Long, CommentsViewVo> commentIdCommentVoMap = new HashMap<>(20);
        for (Comments comments : commentsList) {
            CommentsViewVo commentsViewVo = new CommentsViewVo();
            BeanUtil.copyProperties(comments, commentsViewVo);
            // 设置每条评论的用户信息
            Long userId = comments.getUserId();
            if (userIdUserListMap.containsKey(userId)) {
                User user = userIdUserListMap.get(userId).get(0);
                UserVo userVo = BeanUtil.copyProperties(user, UserVo.class);
                commentsViewVo.setUser(userVo);
            }
            // 添加到评论VO集合
            commentIdCommentVoMap.put(comments.getId(), commentsViewVo);
        }

        // 5. 构建评论树
        for (CommentsViewVo commentsViewVo : commentIdCommentVoMap.values()) {
            Long parentId = commentsViewVo.getParentId();
            // 判断是否是顶级评论
            if (parentId == null) {
                rootCommentsViewVoList.add(commentsViewVo);
            } else {
                CommentsViewVo parentComment = commentIdCommentVoMap.get(parentId);
                if (parentComment != null) {
                    // 如果父级评论的子级评论为空，则创建一个空的子级评论列表
                    if (parentComment.getChildren() == null) {
                        parentComment.setChildren(new ArrayList<>());
                    }
                    // 将当前评论添加到父级评论的子级评论列表中
                    parentComment.getChildren().add(commentsViewVo);
                }
            }
        }

        // 6. 设置参数并返回
        IPage<CommentsViewVo> result = new Page<>(current, pageSize, total);
        result.setRecords(rootCommentsViewVoList);
        return result;
    }

    @Override
    public int getCommentCountByPicId(Long picId) {
        // 判断图片是否存在
        Picture picture = pictureService.getById(picId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 统计评论数
        return this.lambdaQuery()
                .eq(Comments::getPicId, picId)
                .count()
                .intValue();
    }

    @Override
    public void thumbComment(Long commentId, HttpServletRequest request) {
        // 1. 校验参数
        Comments comments = this.getById(commentId);
        ThrowUtils.throwIf(comments == null, ErrorCode.NOT_FOUND_ERROR);

        // 2. 判断用户是否登录
        Long loginUserId = userService.getLoginUserId(request);

        // 3. 定义 Redis Key
        String userLikeKey = "comment:like:users:" + commentId;
        String likeCountKey = "comment:like:count:" + commentId;

        // 4. 使用 Lua 脚本保证原子性（检查 + 计数）
        String luaScript = """
                if redis.call('SISMEMBER',KEYS[1],ARGV[1]) == 1 then
                    return 0
                end
                redis.call('SADD',KEYS[1],ARGV[1])
                return redis.call('INCR',KEYS[2])
                """;
        // 执行脚本获取返回值（点赞总数）
        Long likeCount = redisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Long.class),
                Arrays.asList(userLikeKey, likeCountKey),
                loginUserId.toString()
        );

        // 5. 已点赞则直接返回
        if (likeCount == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Redis 操作失败");
        }
        if (likeCount == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已点赞");
        }

        // 6. 异步持久化
        executorService.submit(() -> persistLikeAsync(commentId, loginUserId, userLikeKey, likeCountKey, likeCount, false));
    }

    @Override
    public void cancelThumbComment(Long commentId, HttpServletRequest request) {
        // 1. 校验参数
        Comments comments = this.getById(commentId);
        ThrowUtils.throwIf(comments == null, ErrorCode.NOT_FOUND_ERROR);

        // 2. 判断用户是否登录
        Long loginUserId = userService.getLoginUserId(request);

        // 3. 定义 Redis Key
        String userLikeKey = "comment:like:users:" + commentId;
        String likeCountKey = "comment:like:count:" + commentId;

        // 4. 使用 Lua 脚本保证原子性（检查 + 计数）
        String luaScript = """
                if redis.call('SISMEMBER',KEYS[1],ARGV[1]) == 0 then
                    return -1
                end
                redis.call('SREM',KEYS[1],ARGV[1])
                return redis.call('DECR',KEYS[2])
                """;
        // 执行脚本获取返回值（点赞总数）
        Long likeCount = redisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Long.class),
                Arrays.asList(userLikeKey, likeCountKey),
                loginUserId.toString()
        );

        // 5. 未点赞则直接返回
        if (likeCount == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Redis 操作失败");
        }
        if (likeCount == -1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未点赞，无法取消");
        }

        // 6. 异步持久化
        executorService.submit(() -> persistLikeAsync(commentId, loginUserId, userLikeKey, likeCountKey, likeCount, true));
    }

    @Override
    public Map<Long, Long> queryBatchCommentCount(List<Long> picIdList) {
        // 构建查询条件
        QueryWrapper<Comments> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("pic_id", picIdList);
        queryWrapper.groupBy("pic_id");
        queryWrapper.select("pic_id", "COUNT(*) as commentsCount");

        // 执行查询
        List<Map<String, Object>> mapList = this.listMaps(queryWrapper);

        // 封装结果到Map
        Map<Long, Long> resultMap = new HashMap<>(10);
        for (Map<String, Object> objectMap : mapList) {
            Long picId = (Long) objectMap.get("pic_id");
            Long commentsCount = (Long) objectMap.get("commentsCount");
            resultMap.put(picId, commentsCount);
        }

        // 处理没有评论的图片
        for (Long picId : picIdList) {
            resultMap.putIfAbsent(picId, 0L);
        }

        return resultMap;
    }

    @Override
    public void commentReview(CommentReviewDto commentReviewDto, HttpServletRequest request) {
        // 获取登录用户ID
        Long loginUserId = userService.getLoginUserId(request);
        // 获取参数
        Long commentId = commentReviewDto.getCommentId();
        Integer reviewStatus = commentReviewDto.getReviewStatus();
        String reviewMsg = commentReviewDto.getReviewMsg();
        // 判断评论是否存在
        if (commentId == null || commentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Comments comments = this.getById(commentId);
        if (comments == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 判断评论状态是否合法
        CommentReviewStatusEnum statusEnum = CommentReviewStatusEnum.getEnumByValue(reviewStatus);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 修改评论状态
        boolean update = this.lambdaUpdate()
                .eq(Comments::getId, commentId)
                .set(Comments::getStatus, reviewStatus)
                .update();
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "评论状态更新失败");
        // 写入审核记录
        CommentReviews commentReviews = new CommentReviews();
        commentReviews.setCommentId(commentId);
        commentReviews.setReviewerId(loginUserId);
        commentReviews.setReviewMsg(reviewMsg);
        commentReviews.setReviewTime(LocalDateTime.now());
        // 设置相同的状态值
        switch (statusEnum) {
            case PASS:
                commentReviews.setReviewStatus(ReviewResultStatusEnum.APPROVE.getValue());
                break;
            case REJECT:
                commentReviews.setReviewStatus(ReviewResultStatusEnum.REJECT.getValue());
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不合理的状态值");
        }
        boolean saved = commentReviewsService.save(commentReviews);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "审核记录写入失败");
    }

    /**
     * 写入点赞记录并更新点赞总数
     *
     * @param commentId    评论ID
     * @param userId       用户ID
     * @param userLikeKey  用户点赞 Key
     * @param likeCountKey 点赞总数 Key
     * @param likeCount    点赞总数
     * @param isCancel     是否是取消操作
     */
    public void persistLikeAsync(Long commentId, Long userId, String userLikeKey, String likeCountKey, Long likeCount, boolean isCancel) {
        try {
            // 设置事务隔离级别为创建新事务
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            // 开启事务
            transactionTemplate.executeWithoutResult(transactionStatus -> {
                if (!isCancel) {
                    // 写入点赞记录
                    CommentLikes commentLikes = new CommentLikes();
                    commentLikes.setCommentId(commentId);
                    commentLikes.setUserId(userId);
                    commentLikes.setCreateTime(LocalDateTime.now());
                    commentLikesService.save(commentLikes);
                } else {
                    // 删除点赞记录
                    commentLikesService.lambdaUpdate()
                            .eq(CommentLikes::getCommentId, commentId)
                            .eq(CommentLikes::getUserId, userId)
                            .remove();
                }

                // 更新点赞总数
                Comments comments = new Comments();
                comments.setId(commentId);
                comments.setLikeCount(likeCount.intValue());
                comments.setUpdateTime(LocalDateTime.now());
                this.updateById(comments);
            });
        } catch (DuplicateKeyException e) {
            // 唯一索引冲突，防止并发重复插入
            log.error("重复点赞：commentId={},userId={}", commentId, userId);
        } catch (Exception e) {
            // 回滚 Redis
            if (!isCancel) {
                redisTemplate.opsForSet().remove(userLikeKey, userId);
                redisTemplate.opsForValue().decrement(likeCountKey);
            } else {
                redisTemplate.opsForSet().add(userLikeKey, userId);
                redisTemplate.opsForValue().increment(likeCountKey);
            }
            log.error("持久化失败，已回滚", e);
        }
    }


    /**
     * 递归删除子级评论
     *
     * @param parentId 父级评论ID
     */
    private void deleteChildrenComments(Long parentId) {
        // 查询所有子级评论
        List<Comments> childrenCommentList = this.lambdaQuery()
                .eq(Comments::getParentId, parentId)
                .list();
        if (CollUtil.isNotEmpty(childrenCommentList)) {
            // 递归删除子级评论
            for (Comments comments : childrenCommentList) {
                deleteChildrenComments(comments.getId());
            }
            // 批量删除当前层级评论
            List<Long> childrenIdList = childrenCommentList.stream().map(Comments::getId).toList();
            CommentsService proxyService = (CommentsService) AopContext.currentProxy();
            boolean removed = proxyService.removeBatchByIds(childrenIdList);
            ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR, "删除子级评论失败");
        }
    }
}




