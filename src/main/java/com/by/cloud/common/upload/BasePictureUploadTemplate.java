package com.by.cloud.common.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.by.cloud.common.CosManager;
import com.by.cloud.config.CosClientConfig;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * 图片上传模板
 *
 * @author lzh
 */
@Slf4j
public abstract class BasePictureUploadTemplate {

    @Resource
    private CosClientConfig clientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param inputSource      文件输入源
     * @param uploadPathPrefix 文件路径前缀
     * @return 图片信息
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 校验文件
        validateFile(inputSource);

        // 重新拼接图片路径，不使用原始的文件名，避免重复和增加安全性
        String uuid = RandomUtil.randomString(16);
        String date = DateUtil.formatDate(new Date());
        // 获取文件名
        String filename = getOriginalFilename(inputSource);
        String uploadFileName = String.format("%s_%s.%s", date, uuid, FileUtil.getSuffix(filename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);

        // 上传图片
        File tempFile = null;
        try {
            // 上传文件
            tempFile = File.createTempFile(uploadPath, null);
            // 转储文件
            transferFile(inputSource, tempFile);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);
            // 获取图片信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 获取图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                // 获取到压缩后的图片信息
                CIObject compressedImageInfo = objectList.get(0);
                // 默认为压缩图片信息，如果有进行缩略处理，再重新赋值
                CIObject thumbnailImageInfo = compressedImageInfo;
                if (objectList.size() > 1) {
                    thumbnailImageInfo = objectList.get(1);
                }
                // 返回压缩后的图片信息
                UploadPictureResult uploadPictureResult = buildResult(filename, compressedImageInfo, thumbnailImageInfo);
                // 设置原图URL
                String rawUrl = clientConfig.getHost() + uploadPath;
                uploadPictureResult.setRawUrl(rawUrl);
                return uploadPictureResult;
            }
            // 返回
            return buildResult(imageInfo, uploadPath, filename, tempFile);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 删除临时文件，释放资源
            this.deleteTempFile(tempFile);
        }
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

    /**
     * 校验文件
     *
     * @param inputSource 输入源
     */
    protected abstract void validateFile(Object inputSource);

    /**
     * 获取原始文件名
     *
     * @param inputSource 输入源
     * @return 文件名
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 将输入源中的文件保存到临时文件中
     *
     * @param inputSource 输入源
     * @param file        临时文件
     * @throws Exception 转换异常
     */
    protected abstract void transferFile(Object inputSource, File file) throws Exception;

    /**
     * 封装图片返回结果
     *
     * @param imageInfo  上传返回的图片信息
     * @param uploadPath 上传路径
     * @param filename   文件名
     * @param tempFile   临时文件
     * @return 结果
     */
    private UploadPictureResult buildResult(ImageInfo imageInfo, String uploadPath, String filename, File tempFile) {
        // 获取图片宽高、宽高比、图片地址
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        String filePath = clientConfig.getHost() + uploadPath;

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
    }

    /**
     * 封装返回结果
     *
     * @param filename            原始文件名
     * @param compressedImageInfo 压缩后的图片信息
     * @param thumbnailImageInfo  缩略图信息
     * @return 压缩结果
     */
    private UploadPictureResult buildResult(String filename, CIObject compressedImageInfo, CIObject thumbnailImageInfo) {
        // 获取图片宽高、宽高比、图片地址
        int picWidth = compressedImageInfo.getWidth();
        int picHeight = compressedImageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        String filePath = clientConfig.getHost() + "/" + compressedImageInfo.getKey();

        // 创建图片返回信息类
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        // 设置压缩后的原图地址
        uploadPictureResult.setPicUrl(filePath);
        uploadPictureResult.setPicName(FileUtil.mainName(filename));
        uploadPictureResult.setPicSize(compressedImageInfo.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedImageInfo.getFormat());
        // 设置缩略图地址
        uploadPictureResult.setThumbnailUrl(clientConfig.getHost() + "/" + thumbnailImageInfo.getKey());

        // 返回
        return uploadPictureResult;
    }
}
