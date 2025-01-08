package com.by.cloud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.common.PageResult;
import com.by.cloud.model.dto.picture.*;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.vo.picture.PictureTagCategoryVo;
import com.by.cloud.model.vo.picture.PictureVo;
import com.by.cloud.model.vo.user.UserVo;

/**
 * @author lzh
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputSource 文件输入源
     * @param dto         请求数据
     * @return 图片信息
     */
    PictureVo uploadPicture(Object inputSource, PictureUploadDto dto);

    /**
     * 根据ID获取图片包装类
     *
     * @param picId 图片ID
     * @return 图片包装类
     */
    PictureVo getPictureVo(Long picId);

    /**
     * 根据ID删除图片
     *
     * @param picId 图片ID
     * @return 是否删除成功
     */
    boolean deleteById(Long picId);

    /**
     * 分页查询图片（仅管理员）
     *
     * @param pageDto 分页参数
     * @return 分页结果
     */
    PageResult<Picture> queryPictureByPage(PicturePageDto pageDto);

    /**
     * 分页查询图片（封装类）
     *
     * @param pageDto 分页参数
     * @return 分页结果
     */
    PageResult<PictureVo> queryPictureVoByPage(PicturePageDto pageDto);

    /**
     * 更新图片信息
     *
     * @param updateDto 图片信息请求体
     * @return 是否更新成功
     */
    boolean updatePicture(PictureUpdateDto updateDto);

    /**
     * 校验图片
     *
     * @param picture 图片信息
     */
    void validPicture(Picture picture);


    /**
     * 图片审核（仅管理员）
     *
     * @param reviewDto 审核参数
     */
    void pictureReview(PictureReviewDto reviewDto);

    /**
     * 填充审核参数
     *
     * @param picture   图片信息
     * @param loginUser 登录用户
     */
    void fillReviewParams(Picture picture, UserVo loginUser);

    /**
     * 批量抓取并上传图片
     *
     * @param batchDto 请求参数
     * @return 上传成功的图片数量
     */
    int uploadPictureByBatch(PictureBatchDto batchDto);

    /**
     * 分页查询图片（封装类），有缓存
     *
     * @param pageDto 分页参数
     * @return 分页结果
     */
    PageResult<PictureVo> queryPictureVoByPageWithCache(PicturePageDto pageDto);

    /**
     * 获取图片标签分类列表
     *
     * @return 图片标签分类列表
     */
    PictureTagCategoryVo listPictureTagCategory();

    /**
     * 根据ID删除图片
     *
     * @param picId 图片ID
     */
    void deletePictureById(Long picId);

    /**
     * 清理图片文件
     *
     * @param picture 图片
     */
    void clearPictureFile(Picture picture);
}
