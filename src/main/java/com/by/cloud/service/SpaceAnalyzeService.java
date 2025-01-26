package com.by.cloud.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.model.dto.space.analyze.SpaceAnalyzeDto;
import com.by.cloud.model.dto.space.analyze.SpaceCategoryAnalyzeDto;
import com.by.cloud.model.dto.space.analyze.SpaceTagAnalyzeDto;
import com.by.cloud.model.dto.space.analyze.SpaceUsageAnalyzeDto;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.entity.Space;
import com.by.cloud.model.vo.space.analyze.SpaceCategoryAnalyzeVo;
import com.by.cloud.model.vo.space.analyze.SpaceTagAnalyzeVo;
import com.by.cloud.model.vo.space.analyze.SpaceUsageAnalyzeVo;

import java.util.List;

/**
 * @author lzh
 */
public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 检查空间分析权限
     *
     * @param spaceAnalyzeDto 空间分析参数
     * @param loginUserId     登录用户ID
     */
    void checkSpaceAnalyzeAuth(SpaceAnalyzeDto spaceAnalyzeDto, Long loginUserId);

    /**
     * 根据分析参数填充查询条件
     *
     * @param spaceAnalyzeDto 分析参数
     * @param queryWrapper    查询条件
     */
    void fillAnalyzeQueryWrapper(SpaceAnalyzeDto spaceAnalyzeDto, LambdaQueryWrapper<Picture> queryWrapper);

    /**
     * 获取空间使用情况分析
     *
     * @param spaceUsageAnalyzeDto 分析参数
     * @return 使用情况
     */
    SpaceUsageAnalyzeVo getSpaceUsageAnalyze(SpaceUsageAnalyzeDto spaceUsageAnalyzeDto);

    /**
     * 获取空间分类情况分析
     *
     * @param spaceCategoryAnalyzeDto 分析参数
     * @return 分析结果
     */
    List<SpaceCategoryAnalyzeVo> getCategoryAnalyze(SpaceCategoryAnalyzeDto spaceCategoryAnalyzeDto);

    /**
     * 获取空间标签情况分析
     *
     * @param spaceTagAnalyzeDto 分析参数
     * @return 分析结果
     */
    List<SpaceTagAnalyzeVo> getTagAnalyze(SpaceTagAnalyzeDto spaceTagAnalyzeDto);
}
