package com.by.cloud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.model.dto.tag.TagUpdateDto;
import com.by.cloud.model.entity.Tag;

import java.util.List;

/**
 * @author lzh
 */
public interface TagService extends IService<Tag> {

    /**
     * 批量新增标签
     *
     * @param tagNameList 标签名称列表
     */
    void addBatchTags(List<String> tagNameList);

    /**
     * 批量删除标签
     *
     * @param ids 标签ID列表
     */
    void delBatchTag(List<Long> ids);

    /**
     * 修改标签名称
     *
     * @param updateDto 请求参数
     */
    void updateTagById(TagUpdateDto updateDto);

    /**
     * 获取标签名称列表
     *
     * @return 标签名称列表
     */
    List<String> listTagName();
}
