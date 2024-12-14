package com.by.cloud.common;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.by.cloud.config.CosClientConfig;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.model.dto.file.UploadPictureResult;
import com.by.cloud.utils.ThrowUtils;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * @author lzh
 */
@Slf4j
@Service
public class FileManager {

    @Resource
    private CosClientConfig clientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片到对象存储COS
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 文件路径前缀
     * @return 图片信息
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验文件
        validateFile(multipartFile);

        // 重新拼接图片路径，不使用原始的文件名，避免重复和增加安全性
        String uuid = RandomUtil.randomString(16);
        String date = DateUtil.formatDate(new Date());
        String filename = multipartFile.getOriginalFilename();
        String uploadFileName = String.format("%s_%s.%s", date, uuid, FileUtil.getSuffix(filename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);

        // 上传图片
        File tempFile = null;
        try {
            // 上传文件
            tempFile = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(tempFile);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);

            // 获取图片信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 获取图片宽高、宽高比、图片地址
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            String filePath = clientConfig.getHost() + File.separator + uploadPath;

            // 创建图片返回信息类
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setPicUrl(filePath);
            uploadPictureResult.setPicName(FileUtil.mainName(filename));
            uploadPictureResult.setPicSize(FileUtil.size(tempFile));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());

            // 返回
            return uploadPictureResult;

        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 删除临时文件，释放资源
            this.deleteTempFile(tempFile);
        }
    }

    private void validateFile(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 校验大小(限制最大为 2MB)
        final long twoMb = 2 * 1024 * 1024;
        long fileSize = multipartFile.getSize();
        ThrowUtils.throwIf(fileSize > twoMb, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2MB");
        // 校验后缀
        final List<String> allowSuffixList = List.of("jpeg", "png", "jpg", "webp");
        String filename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(filename);
        ThrowUtils.throwIf(!allowSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式不支持");
    }


    /**
     * 删除临时文件
     *
     * @param file 文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean delete = file.delete();
        if (!delete) {
            log.error("file delete error, filePath = {}", file.getAbsolutePath());
        }
    }
}
