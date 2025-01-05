package com.by.cloud.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.by.cloud.aop.PreAuthorize;
import com.by.cloud.common.BaseResponse;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.UserRoleEnum;
import com.by.cloud.model.dto.category.CategoryUpdateDto;
import com.by.cloud.model.entity.Category;
import com.by.cloud.service.CategoryService;
import com.by.cloud.utils.ResultUtils;
import com.by.cloud.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author lzh
 */
@RestController
@RequestMapping("/category")
@Api(tags = "分类模块")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @ApiOperation("批量新增分类")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PostMapping("/batch/add")
    public BaseResponse<Boolean> addBatchCategory(@RequestBody List<String> categoryNameList) {
        ThrowUtils.throwIf(CollUtil.isEmpty(categoryNameList), ErrorCode.PARAMS_ERROR);
        categoryService.addBatchCategory(categoryNameList);
        return ResultUtils.success(true);
    }

    @ApiOperation("批量删除分类")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> delBatchCategory(@RequestParam("ids") List<Long> ids) {
        ThrowUtils.throwIf(CollectionUtil.isEmpty(ids), ErrorCode.PARAMS_ERROR);
        categoryService.delBatchCategory(ids);
        return ResultUtils.success(true);
    }

    @ApiOperation("修改分类名称")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PutMapping("/update")
    public BaseResponse<Boolean> updateCategoryById(@RequestBody CategoryUpdateDto updateDto) {
        ThrowUtils.throwIf(updateDto == null, ErrorCode.PARAMS_ERROR);
        categoryService.updateCategoryById(updateDto);
        return ResultUtils.success(true);
    }

    @ApiOperation("根据ID查询分类")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @GetMapping("/get/{id}")
    public BaseResponse<Category> getCategoryById(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Category category = categoryService.getById(id);
        return ResultUtils.success(category);
    }

    @ApiOperation("查询分类列表")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @GetMapping("/list")
    public BaseResponse<List<Category>> listCategory() {
        List<Category> categoryList = categoryService.list();
        return ResultUtils.success(categoryList);
    }
}
