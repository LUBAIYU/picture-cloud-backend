package com.by.cloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.by.cloud.model.dto.picture.PicturePageDto;
import com.by.cloud.model.dto.space.analyze.SpaceCategoryAnalyzeDto;
import com.by.cloud.model.dto.space.analyze.SpaceTagAnalyzeDto;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.vo.space.analyze.SpaceCategoryAnalyzeVo;
import com.by.cloud.model.vo.space.analyze.SpaceTagAnalyzeVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author lzh
 */
public interface PictureMapper extends BaseMapper<Picture> {

    /**
     * 分页查询图片
     *
     * @param pageParams 分页参数
     * @param pageDto    查询条件
     * @return 分页结果
     */
    IPage<Picture> queryPictureByPage(IPage<Picture> pageParams, @Param("dto") PicturePageDto pageDto);

    /**
     * 图片分类分析
     *
     * @param spaceCategoryAnalyzeDto 分析参数
     * @return 分类列表
     */
    List<SpaceCategoryAnalyzeVo> getCategoryAnalyze(@Param("dto") SpaceCategoryAnalyzeDto spaceCategoryAnalyzeDto);

    /**
     * 图片标签分析
     *
     * @param spaceTagAnalyzeDto 分析参数
     * @return 分析结果
     */
    List<SpaceTagAnalyzeVo> getTagAnalyze(@Param("dto") SpaceTagAnalyzeDto spaceTagAnalyzeDto);
}




