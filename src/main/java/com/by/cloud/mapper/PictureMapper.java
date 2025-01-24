package com.by.cloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.by.cloud.model.dto.picture.PicturePageDto;
import com.by.cloud.model.entity.Picture;
import org.apache.ibatis.annotations.Param;

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
}




