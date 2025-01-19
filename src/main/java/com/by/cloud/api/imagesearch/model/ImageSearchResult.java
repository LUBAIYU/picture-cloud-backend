package com.by.cloud.api.imagesearch.model;

import lombok.Data;

/**
 * 图片搜索结果
 *
 * @author lzh
 */
@Data
public class ImageSearchResult {

    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源图地址
     */
    private String fromUrl;
}
