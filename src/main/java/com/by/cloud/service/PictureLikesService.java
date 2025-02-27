package com.by.cloud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.by.cloud.model.dto.picture.PictureLikesDto;
import com.by.cloud.model.entity.PictureLikes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author lzh
 */
public interface PictureLikesService extends IService<PictureLikes> {

    /**
     * 点赞或取消点赞
     *
     * @param pictureLikesDto 请求参数
     * @param request         请求对象
     */
    void thumbOrCancelThumbPicture(PictureLikesDto pictureLikesDto, HttpServletRequest request);
}
