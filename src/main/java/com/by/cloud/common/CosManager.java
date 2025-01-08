package com.by.cloud.common;

import cn.hutool.core.io.FileUtil;
import com.by.cloud.config.CosClientConfig;
import com.by.cloud.constants.PictureConstant;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lzh
 */
@Component
public class CosManager {

    @Resource
    private COSClient cosClient;

    @Resource
    private CosClientConfig clientConfig;

    /**
     * 上传文件
     *
     * @param key  唯一标识
     * @param file 文件
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(clientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载文件
     *
     * @param key 唯一键
     * @return 文件对象
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(clientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传图片并获取图片基本信息
     *
     * @param key  唯一标识
     * @param file 文件
     * @return 图片信息
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(clientConfig.getBucket(), key, file);
        // 配置获取图片基本信息
        PicOperations picOperations = new PicOperations();
        // 1 表示开启图片信息获取
        picOperations.setIsPicInfo(1);

        // 图片处理规则
        List<PicOperations.Rule> rules = new ArrayList<>();

        // 图片格式处理(转成 webp 格式)
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setBucket(clientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/webp");
        rules.add(compressRule);

        // 缩略图处理(imageMogr2/thumbnail/<Width>x<Height>>) 如果大于原图宽高，则不处理
        // 如果原文件大小大于 20 KB，才做缩略处理
        if (file.length() > PictureConstant.THUMBNAIL_FILE_SIZE) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            // 拼接缩略图的路径
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setBucket(clientConfig.getBucket());
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            rules.add(thumbnailRule);
        }

        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除文件
     *
     * @param key 文件路径
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(clientConfig.getBucket(), key);
    }
}
