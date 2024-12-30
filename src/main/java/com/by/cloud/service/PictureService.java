package com.by.cloud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.common.PageResult;
import com.by.cloud.model.dto.picture.*;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.entity.User;
import com.by.cloud.model.vo.PictureVo;
import com.by.cloud.model.vo.UserVo;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author lzh
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param multipartFile 文件
     * @param dto           请求数据
     * @return 图片信息
     */
    PictureVo uploadPicture(MultipartFile multipartFile, PictureUploadDto dto);

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
     * 更新图片信息（仅管理员）
     *
     * @param updateDto 图片信息请求体
     * @return 是否更新成功
     */
    boolean updatePictureByAdmin(PictureUpdateDto updateDto);

    /**
     * 校验图片
     *
     * @param picture 图片信息
     */
    void validPicture(Picture picture);

    /**
     * 编辑图片信息（用户使用）
     *
     * @param editDto 编辑信息
     */
    void editPicture(PictureEditDto editDto);

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
}
