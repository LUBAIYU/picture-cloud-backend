package com.by.cloud.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.common.FileManager;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.mapper.PictureMapper;
import com.by.cloud.model.dto.file.UploadPictureResult;
import com.by.cloud.model.dto.picture.PictureUploadDto;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.vo.PictureVo;
import com.by.cloud.model.vo.UserVo;
import com.by.cloud.service.PictureService;
import com.by.cloud.service.UserService;
import com.by.cloud.utils.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * @author lzh
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public PictureVo uploadPicture(MultipartFile multipartFile, PictureUploadDto dto) {
        // 判断是新增还是修改
        Long picId = null;
        if (dto != null) {
            picId = dto.getPicId();
        }
        if (picId != null) {
            Picture picture = this.lambdaQuery().eq(Picture::getPicId, picId).one();
            ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }

        // 获取当前登录用户
        UserVo loginUser = userService.getLoginUser();

        // 上传图片获取图片信息
        String uploadPathPrefix = String.format("public/%s", loginUser.getUserId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);

        // 构造入库信息
        Picture picture = new Picture();
        BeanUtil.copyProperties(uploadPictureResult, picture);
        if (picId != null) {
            picture.setPicId(picId);
            picture.setEditTime(LocalDateTime.now());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片保存失败");
        return PictureVo.objToVo(picture);
    }
}




