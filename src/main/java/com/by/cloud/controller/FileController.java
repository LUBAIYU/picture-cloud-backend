package com.by.cloud.controller;

import com.by.cloud.aop.PreAuthorize;
import com.by.cloud.common.BaseResponse;
import com.by.cloud.common.CosManager;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.UserRoleEnum;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.utils.ResultUtils;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @author lzh
 */
@RestController
@RequestMapping("/file")
@Slf4j
@Api(tags = "文件模块")
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传
     *
     * @param multipartFile 文件
     * @return 访问路径
     */
    @ApiOperation("文件上传")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @PostMapping("/test/upload")
    public BaseResponse<String> textFileUpload(@RequestPart("file") MultipartFile multipartFile) {
        // 文件目录
        String originalFilename = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", originalFilename);
        File tempFile = null;
        try {
            // 上传文件
            tempFile = File.createTempFile(filePath, null);
            multipartFile.transferTo(tempFile);
            cosManager.putObject(filePath, tempFile);
            // 返回可访问的地址
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error("file upload error, filePath = {}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (tempFile != null) {
                // 删除临时文件，释放资源
                boolean delete = tempFile.delete();
                if (!delete) {
                    log.error("file delete error, filePath = {}", filePath);
                }
            }
        }
    }

    /**
     * 测试文件下载
     *
     * @param filePath 文件路径
     * @param response 响应对象
     */
    @ApiOperation("文件下载")
    @PreAuthorize(role = UserRoleEnum.ADMIN)
    @GetMapping("/test/download")
    public void testFileDownload(String filePath, HttpServletResponse response) throws IOException {
        COSObjectInputStream objectInputStream = null;
        try {
            COSObject cosObject = cosManager.getObject(filePath);
            objectInputStream = cosObject.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(objectInputStream);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filepath = " + filePath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filePath = {}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (objectInputStream != null) {
                objectInputStream.close();
            }
        }
    }
}
