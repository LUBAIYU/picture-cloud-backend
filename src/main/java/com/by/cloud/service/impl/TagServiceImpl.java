package com.by.cloud.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.common.PageResult;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.mapper.TagMapper;
import com.by.cloud.model.dto.tag.TagPageDto;
import com.by.cloud.model.dto.tag.TagUpdateDto;
import com.by.cloud.model.entity.PictureTag;
import com.by.cloud.model.entity.Tag;
import com.by.cloud.model.vo.tag.TagListVo;
import com.by.cloud.service.PictureTagService;
import com.by.cloud.service.TagService;
import com.by.cloud.utils.ThrowUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lzh
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Resource
    private PictureTagService pictureTagService;

    @Override
    public void addBatchTags(List<String> tagNameList) {
        // 判断标签是否存在
        List<Tag> dbTagList = this.list();
        if (CollUtil.isNotEmpty(dbTagList)) {
            List<String> dbNameList = dbTagList.stream()
                    .map(Tag::getName)
                    .toList();
            for (String tagName : tagNameList) {
                if (dbNameList.contains(tagName)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签已存在");
                }
            }
        }
        // 批量插入数据库
        List<Tag> tagList = new ArrayList<>();
        for (String tagName : tagNameList) {
            Tag tag = new Tag();
            tag.setName(tagName);
            tagList.add(tag);
        }
        TagService proxyService = (TagService) AopContext.currentProxy();
        boolean saved = proxyService.saveBatch(tagList);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void delBatchTag(List<Long> ids) {
        // 判断删除的分类是否有使用中
        Long count = pictureTagService.lambdaQuery()
                .in(PictureTag::getTagId, ids)
                .count();
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签在使用中，无法删除");
        }
        // 批量删除
        TagService proxyService = (TagService) AopContext.currentProxy();
        boolean removed = proxyService.removeBatchByIds(ids);
        ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void updateTagById(TagUpdateDto updateDto) {
        // 获取参数
        Long id = updateDto.getId();
        String name = updateDto.getName();
        // 校验参数
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StrUtil.isBlank(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断标签是否存在
        Tag tag = this.getById(id);
        ThrowUtils.throwIf(tag == null, ErrorCode.NOT_FOUND_ERROR);
        // 判断新的标签名称是否与现有的重复
        Long count = this.lambdaQuery()
                .eq(Tag::getName, name)
                .count();
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "标签名称已存在");
        // 更新
        Tag updateTag = new Tag();
        updateTag.setId(id);
        updateTag.setName(name);
        boolean updated = this.updateById(updateTag);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public List<TagListVo> listTag() {
        List<Tag> tagList = this.list();
        if (CollUtil.isEmpty(tagList)) {
            return Collections.emptyList();
        }
        List<TagListVo> tagListVos = new ArrayList<>();
        for (Tag tag : tagList) {
            TagListVo tagListVo = new TagListVo();
            tagListVo.setId(tag.getId());
            tagListVo.setName(tag.getName());
            tagListVos.add(tagListVo);
        }
        return tagListVos;
    }

    @Override
    public PageResult<Tag> listTagByPage(TagPageDto pageDto) {
        // 校验参数
        int current = pageDto.getCurrent();
        int pageSize = pageDto.getPageSize();
        String name = pageDto.getName();
        ThrowUtils.throwIf(current <= 0 || pageSize <= 0, ErrorCode.PARAMS_ERROR);
        // 添加查询条件
        IPage<Tag> page = new Page<>(current, pageSize);
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(name), Tag::getName, name);
        queryWrapper.orderByDesc(Tag::getCreateTime);
        // 查询
        this.page(page, queryWrapper);
        // 封装参数返回
        return PageResult.of(page.getTotal(), page.getRecords());
    }
}




