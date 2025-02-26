package com.by.cloud.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.common.PageResult;
import com.by.cloud.mapper.CommentLikesMapper;
import com.by.cloud.model.dto.comment.CommentLikesPageDto;
import com.by.cloud.model.entity.CommentLikes;
import com.by.cloud.service.CommentLikesService;
import org.springframework.stereotype.Service;

/**
 * @author lzh
 */
@Service
public class CommentLikesServiceImpl extends ServiceImpl<CommentLikesMapper, CommentLikes> implements CommentLikesService {

    @Override
    public PageResult<CommentLikes> queryCommentLikesByPage(CommentLikesPageDto pageDto) {
        // 获取参数
        Long commentId = pageDto.getCommentId();
        Long userId = pageDto.getUserId();
        int current = pageDto.getCurrent();
        int pageSize = pageDto.getPageSize();
        // 创建分页对象
        IPage<CommentLikes> page = new Page<>(current, pageSize);
        // 构建查询条件
        LambdaQueryWrapper<CommentLikes> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjectUtil.isNotEmpty(commentId), CommentLikes::getCommentId, commentId);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), CommentLikes::getUserId, userId);
        queryWrapper.orderByDesc(CommentLikes::getCreateTime);
        // 查询
        page = this.page(page, queryWrapper);
        // 返回结果
        return PageResult.of(page.getTotal(), page.getRecords());
    }
}




