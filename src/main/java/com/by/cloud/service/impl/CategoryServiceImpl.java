package com.by.cloud.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.mapper.CategoryMapper;
import com.by.cloud.model.dto.category.CategoryUpdateDto;
import com.by.cloud.model.entity.Category;
import com.by.cloud.model.entity.PictureCategoryTag;
import com.by.cloud.model.vo.category.CategoryListVo;
import com.by.cloud.service.CategoryService;
import com.by.cloud.service.PictureCategoryTagService;
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
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Resource
    private PictureCategoryTagService pictureCategoryTagService;

    @Override
    public void addBatchCategory(List<String> categoryNameList) {
        // 判断分类是否存在
        // 1.先查出所有的分类
        List<Category> dbCategoryList = this.list();
        if (CollUtil.isNotEmpty(dbCategoryList)) {
            // 2.获取已经存在的分类名称列表
            List<String> dbNameList = dbCategoryList.stream()
                    .map(Category::getName)
                    .toList();
            // 3.判断是否有已存在的分类名称
            for (String categoryName : categoryNameList) {
                if (dbNameList.contains(categoryName)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类已存在");
                }
            }
        }
        // 保存新分类
        List<Category> categoryList = new ArrayList<>();
        for (String categoryName : categoryNameList) {
            Category category = new Category();
            category.setName(categoryName);
            categoryList.add(category);
        }
        // 使用代理对象调用事务方法
        CategoryService proxyService = (CategoryService) AopContext.currentProxy();
        boolean saved = proxyService.saveBatch(categoryList);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public List<CategoryListVo> listCategory() {
        List<Category> categoryList = this.list();
        if (CollUtil.isEmpty(categoryList)) {
            return Collections.emptyList();
        }
        List<CategoryListVo> categoryListVos = new ArrayList<>();
        for (Category category : categoryList) {
            CategoryListVo categoryListVo = new CategoryListVo();
            categoryListVo.setId(category.getId());
            categoryListVo.setName(category.getName());
            categoryListVos.add(categoryListVo);
        }
        return categoryListVos;
    }

    @Override
    public void delBatchCategory(List<Long> ids) {
        // 判断删除的分类是否有使用中
        Long count = pictureCategoryTagService.lambdaQuery()
                .in(PictureCategoryTag::getCategoryId, ids)
                .count();
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类在使用中，无法删除");
        }
        // 批量删除
        CategoryService proxyService = (CategoryService) AopContext.currentProxy();
        boolean removed = proxyService.removeBatchByIds(ids);
        ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void updateCategoryById(CategoryUpdateDto updateDto) {
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
        // 判断分类是否存在
        Category category = this.getById(id);
        ThrowUtils.throwIf(category == null, ErrorCode.NOT_FOUND_ERROR);
        // 判断新的分类名称是否与现有的重复
        Long count = this.lambdaQuery()
                .eq(Category::getName, name)
                .count();
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "分类名称已存在");
        // 更新
        Category updateCategory = new Category();
        updateCategory.setId(id);
        updateCategory.setName(name);
        boolean updated = this.updateById(updateCategory);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR);
    }
}




