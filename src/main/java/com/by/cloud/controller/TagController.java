package com.by.cloud.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.by.cloud.aop.PreAuthorize;
import com.by.cloud.common.BaseResponse;
import com.by.cloud.common.PageResult;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.UserRoleEnum;
import com.by.cloud.model.dto.tag.TagPageDto;
import com.by.cloud.model.dto.tag.TagUpdateDto;
import com.by.cloud.model.entity.Tag;
import com.by.cloud.model.vo.tag.TagListVo;
import com.by.cloud.service.TagService;
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
@RequestMapping("/tag")
@Api(tags = "标签模块")
public class TagController {

    @Resource
    private TagService tagService;

    @ApiOperation("批量新增标签")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PostMapping("/batch/add")
    public BaseResponse<Boolean> addBatchTags(@RequestBody List<String> tagNameList) {
        ThrowUtils.throwIf(CollUtil.isEmpty(tagNameList), ErrorCode.PARAMS_ERROR);
        tagService.addBatchTags(tagNameList);
        return ResultUtils.success(true);
    }

    @ApiOperation("批量删除标签")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> delBatchTag(@RequestParam("ids") List<Long> ids) {
        ThrowUtils.throwIf(CollectionUtil.isEmpty(ids), ErrorCode.PARAMS_ERROR);
        tagService.delBatchTag(ids);
        return ResultUtils.success(true);
    }

    @ApiOperation("修改标签名称")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PutMapping("/update")
    public BaseResponse<Boolean> updateTagById(@RequestBody TagUpdateDto updateDto) {
        ThrowUtils.throwIf(updateDto == null, ErrorCode.PARAMS_ERROR);
        tagService.updateTagById(updateDto);
        return ResultUtils.success(true);
    }

    @ApiOperation("根据ID查询标签")
    @GetMapping("/get/{id}")
    public BaseResponse<Tag> getTagById(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Tag tag = tagService.getById(id);
        return ResultUtils.success(tag);
    }

    @ApiOperation("查询标签列表")
    @GetMapping("/list")
    public BaseResponse<List<TagListVo>> listTag() {
        List<TagListVo> tagListVos = tagService.listTag();
        return ResultUtils.success(tagListVos);
    }

    @ApiOperation("分页查询标签")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PostMapping("/page")
    public BaseResponse<PageResult<Tag>> listTagByPage(@RequestBody TagPageDto pageDto) {
        ThrowUtils.throwIf(pageDto == null, ErrorCode.PARAMS_ERROR);
        PageResult<Tag> pageResult = tagService.listTagByPage(pageDto);
        return ResultUtils.success(pageResult);
    }
}
