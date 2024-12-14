package com.by.cloud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.model.dto.picture.PictureUploadDto;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.vo.PictureVo;
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
}
