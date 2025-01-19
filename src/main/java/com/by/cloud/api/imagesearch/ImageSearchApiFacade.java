package com.by.cloud.api.imagesearch;

import com.by.cloud.api.imagesearch.model.ImageSearchResult;
import com.by.cloud.api.imagesearch.sub.GetImageFirstUrlApi;
import com.by.cloud.api.imagesearch.sub.GetImageListApi;
import com.by.cloud.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author lzh
 */
@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索相似图片
     *
     * @param imageUrl 图片地址
     * @return 相似图片列表
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        return GetImageListApi.getImageList(imageFirstUrl);
    }

    public static void main(String[] args) {
        String imageUrl = "https://www.codefather.cn/logo.png";
        List<ImageSearchResult> imageList = searchImage(imageUrl);
        System.out.println("搜索成功，" + imageList);
    }
}
