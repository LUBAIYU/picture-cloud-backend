package com.by.cloud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.model.dto.category.CategoryUpdateDto;
import com.by.cloud.model.entity.Category;

import java.util.List;

/**
 * @author lzh
 */
public interface CategoryService extends IService<Category> {

    /**
     * 批量删除分类
     *
     * @param ids ID列表
     */
    void delBatchCategory(List<Long> ids);

    /**
     * 修改分类
     *
     * @param updateDto 请求参数
     */
    void updateCategoryById(CategoryUpdateDto updateDto);

    /**
     * 批量添加分类
     *
     * @param categoryNameList 分类列表
     */
    void addBatchCategory(List<String> categoryNameList);
}
