package com.by.cloud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.by.cloud.api.imagesearch.model.ImageSearchResult;
import com.by.cloud.common.PageResult;
import com.by.cloud.model.dto.picture.*;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.entity.User;
import com.by.cloud.model.vo.picture.PictureTagCategoryVo;
import com.by.cloud.model.vo.picture.PictureVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author lzh
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputSource 文件输入源
     * @param dto         请求数据
     * @param request     请求对象
     * @return 图片信息
     */
    PictureVo uploadPicture(Object inputSource, PictureUploadDto dto, HttpServletRequest request);

    /**
     * 根据ID获取图片包装类
     *
     * @param picId   图片ID
     * @param request 请求对象
     * @return 图片包装类
     */
    PictureVo getPictureVo(Long picId, HttpServletRequest request);

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
     * @param request   请求对象
     * @return 是否更新成功
     */
    boolean updatePicture(PictureUpdateDto updateDto, HttpServletRequest request);

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
     * @param request   请求对象
     */
    void pictureReview(PictureReviewDto reviewDto, HttpServletRequest request);

    /**
     * 填充审核参数
     *
     * @param picture   图片信息
     * @param loginUser 登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取并上传图片
     *
     * @param batchDto 请求参数
     * @param request  请求对象
     * @return 上传成功的图片数量
     */
    int uploadPictureByBatch(PictureBatchDto batchDto, HttpServletRequest request);

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

    /**
     * 校验图片权限
     *
     * @param picture 图片
     * @param request 请求对象
     */
    void checkPictureAuth(Picture picture, HttpServletRequest request);

    /**
     * 以图搜图
     *
     * @param searchByPictureDto 请求参数
     * @return 相似图片列表
     */
    List<ImageSearchResult> searchPictureByPicture(PictureSearchByPictureDto searchByPictureDto);

    /**
     * 根据颜色搜索图片
     *
     * @param spaceId  空间ID
     * @param hexColor 目标颜色（16进制）
     * @param request  请求对象
     * @return 图片列表
     */
    List<PictureVo> searchPictureByColor(Long spaceId, String hexColor, HttpServletRequest request);

    /**
     * 批量修改图片信息
     *
     * @param editByBatchDto 请求体
     * @param request        请求对象
     */
    void editPictureByBatch(PictureEditByBatchDto editByBatchDto, HttpServletRequest request);

    /**
     * 创建扩图任务
     *
     * @param createOutPaintingTaskDto 扩图参数
     * @return 扩图结果
     */
    CreateOutPaintingTaskResponse createOutPaintingTask(PictureCreateOutPaintingTaskDto createOutPaintingTaskDto);

    /**
     * 刷新指定缓存
     *
     * @param pageDto 查询参数
     */
    void refreshCache(PicturePageDto pageDto);

    /**
     * 获取 Redis 中的缓存 Key
     *
     * @param prefix 前缀
     * @return 缓存 Key 列表
     */
    List<String> getAllCacheKeys(String prefix);

    /**
     * 删除指定缓存
     *
     * @param hashKey 缓存Key
     * @return 是否删除成功
     */
    boolean removeCacheByKey(String hashKey);
}
