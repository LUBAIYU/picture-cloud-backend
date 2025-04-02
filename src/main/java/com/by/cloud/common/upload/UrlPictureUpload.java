package com.by.cloud.common.upload;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.by.cloud.constants.PictureConstant;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.utils.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * URL上传
 *
 * @author lzh
 */
@Service
public class UrlPictureUpload extends BasePictureUploadTemplate {

    @Override
    protected void validateFile(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 校验非空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR);

        // 校验 URL 格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }

        // 校验 URL 协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");

        // 发送 HEAD 请求验证文件是否存在
        try (HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute()) {
            // 不支持HEAD请求，无需其他处理
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 文件类型校验
            String contentType = httpResponse.header(Header.CONTENT_TYPE);
            if (StrUtil.isNotBlank(contentType)) {
                ThrowUtils.throwIf(!PictureConstant.ALLOW_CONTENT_TYPE_LIST.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型异常");
            }
            // 文件大小校验
            String contentLengthStr = httpResponse.header(Header.CONTENT_LENGTH);
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    ThrowUtils.throwIf(contentLength > PictureConstant.MAX_FILE_SIZE,
                            ErrorCode.PARAMS_ERROR, "文件大小不能超过 10MB");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式异常");
                }
            }
        }
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 获取URL地址中的文件后缀
        int lastSlashIndex = fileUrl.lastIndexOf('/');
        int questionIndex = fileUrl.indexOf('?');
        if (questionIndex == -1) {
            questionIndex = fileUrl.length();
        }
        // 返回包含后缀的文件名
        return fileUrl.substring(lastSlashIndex + 1, questionIndex);
    }

    @Override
    protected void  transferFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl, file);
    }
}
