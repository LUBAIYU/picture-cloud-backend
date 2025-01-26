package com.by.cloud.controller;

import com.by.cloud.common.BaseResponse;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.model.dto.space.analyze.SpaceCategoryAnalyzeDto;
import com.by.cloud.model.dto.space.analyze.SpaceTagAnalyzeDto;
import com.by.cloud.model.dto.space.analyze.SpaceUsageAnalyzeDto;
import com.by.cloud.model.vo.space.analyze.SpaceCategoryAnalyzeVo;
import com.by.cloud.model.vo.space.analyze.SpaceTagAnalyzeVo;
import com.by.cloud.model.vo.space.analyze.SpaceUsageAnalyzeVo;
import com.by.cloud.service.SpaceAnalyzeService;
import com.by.cloud.utils.ResultUtils;
import com.by.cloud.utils.ThrowUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author lzh
 */
@RestController
@RequestMapping("/space/analyze")
@Api(tags = "空间分析模块")
public class SpaceAnalyzeController {

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    @ApiOperation("空间资源使用分析")
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeVo> getSpaceUsageAnalyze(@RequestBody SpaceUsageAnalyzeDto spaceUsageAnalyzeDto) {
        ThrowUtils.throwIf(spaceUsageAnalyzeDto == null, ErrorCode.PARAMS_ERROR);
        SpaceUsageAnalyzeVo spaceUsageAnalyzeVo = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeDto);
        return ResultUtils.success(spaceUsageAnalyzeVo);
    }

    @ApiOperation("空间图片分类分析")
    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeVo>> getCategoryAnalyze(@RequestBody SpaceCategoryAnalyzeDto spaceCategoryAnalyzeDto) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeDto == null, ErrorCode.PARAMS_ERROR);
        List<SpaceCategoryAnalyzeVo> spaceCategoryAnalyzeVoList = spaceAnalyzeService.getCategoryAnalyze(spaceCategoryAnalyzeDto);
        return ResultUtils.success(spaceCategoryAnalyzeVoList);
    }

    @ApiOperation("空间图片标签分析")
    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeVo>> getTagAnalyze(@RequestBody SpaceTagAnalyzeDto spaceTagAnalyzeDto) {
        ThrowUtils.throwIf(spaceTagAnalyzeDto == null, ErrorCode.PARAMS_ERROR);
        List<SpaceTagAnalyzeVo> spaceTagAnalyzeVoList = spaceAnalyzeService.getTagAnalyze(spaceTagAnalyzeDto);
        return ResultUtils.success(spaceTagAnalyzeVoList);
    }
}
