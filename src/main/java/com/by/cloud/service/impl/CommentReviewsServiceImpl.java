package com.by.cloud.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.common.PageResult;
import com.by.cloud.mapper.CommentReviewsMapper;
import com.by.cloud.model.dto.comment.CommentReviewsPageDto;
import com.by.cloud.model.entity.CommentReviews;
import com.by.cloud.service.CommentReviewsService;
import org.springframework.stereotype.Service;

/**
 * @author lzh
 */
@Service
public class CommentReviewsServiceImpl extends ServiceImpl<CommentReviewsMapper, CommentReviews> implements CommentReviewsService {

    @Override
    public PageResult<CommentReviews> queryCommentReviewsByPage(CommentReviewsPageDto pageDto) {
        // 获取参数
        Long commentId = pageDto.getCommentId();
        Long reviewerId = pageDto.getReviewerId();
        Integer reviewStatus = pageDto.getReviewStatus();
        String reviewMsg = pageDto.getReviewMsg();
        int current = pageDto.getCurrent();
        int pageSize = pageDto.getPageSize();
        // 创建分页对象
        IPage<CommentReviews> page = new Page<>(current, pageSize);
        // 构建查询条件
        LambdaQueryWrapper<CommentReviews> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjectUtil.isNotEmpty(commentId), CommentReviews::getCommentId, commentId);
        queryWrapper.eq(ObjectUtil.isNotEmpty(reviewerId), CommentReviews::getReviewerId, reviewerId);
        queryWrapper.eq(ObjectUtil.isNotEmpty(reviewStatus), CommentReviews::getReviewStatus, reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMsg), CommentReviews::getReviewMsg, reviewMsg);
        queryWrapper.orderByDesc(CommentReviews::getReviewTime);
        // 查询
        page = this.page(page, queryWrapper);
        // 返回结果
        return PageResult.of(page.getTotal(), page.getRecords());
    }
}




