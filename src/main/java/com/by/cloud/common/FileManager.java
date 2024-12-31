package com.by.cloud.common;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.by.cloud.config.CosClientConfig;
import com.by.cloud.constants.PictureConstant;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;

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
     * 上传头像
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 路径前缀
     * @return 头像地址
     */
    public String uploadAvatar(MultipartFile multipartFile, String uploadPathPrefix) {
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
            cosManager.putObject(uploadPath, tempFile);

            // 返回图片地址
            return clientConfig.getHost() + uploadPath;

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
        long fileSize = multipartFile.getSize();
        ThrowUtils.throwIf(fileSize > PictureConstant.MAX_FILE_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2MB");
        // 校验后缀
        String filename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(filename);
        ThrowUtils.throwIf(!PictureConstant.ALLOW_SUFFIX_LIST.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式不支持");
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
