package com.by.cloud.common.upload;

import cn.hutool.core.io.FileUtil;
import com.by.cloud.constants.PictureConstant;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.utils.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 文件上传
 *
 * @author lzh
 */
@Service
public class FilePictureUpload extends BasePictureUploadTemplate {

    @Override
    protected void validateFile(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 校验大小(限制最大为 10MB)
        long fileSize = multipartFile.getSize();
        ThrowUtils.throwIf(fileSize > PictureConstant.MAX_FILE_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2MB");
        // 校验后缀
        String filename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(filename);
        ThrowUtils.throwIf(!PictureConstant.ALLOW_SUFFIX_LIST.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式不支持");
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void transferFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
