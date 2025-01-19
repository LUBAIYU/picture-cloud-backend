package com.by.cloud.api.imagesearch.sub;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取以图搜图页面地址（API）
 *
 * @author lzh
 */
@Slf4j
public class GetImagePageUrlApi {

    /**
     * 获取以图搜图页面地址
     *
     * @param imageUrl 图片地址
     * @return 相似图片页面地址
     */
    public static String getImagePageUrl(String imageUrl) {
        // 封装参数
        Map<String, Object> formData = new HashMap<>(4);
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");

        // 当前时间戳
        long uptime = System.currentTimeMillis();
        String destUrl = "https://graph.baidu.com/upload?uptime=" + uptime;
        try {
            // 发送请求
            HttpResponse httpResponse = HttpRequest.post(destUrl)
                    .form(formData)
                    .timeout(5000)
                    .execute();

            // 解析结果
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            String response = httpResponse.body();
            if (StrUtil.isBlank(response)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }

            // 转为KV对象
            Map<String, Object> resultMap = JSONUtil.toBean(response, Map.class);
            final String code = "status";
            if (resultMap == null || !Integer.valueOf(0).equals(resultMap.get(code))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }

            // 获取结果数据
            Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
            String rawUrl = (String) data.get("url");
            // 对URL进行解码
            String searchResult = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            if (StrUtil.isBlank(searchResult)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");
            }
            return searchResult;
        } catch (Exception e) {
            log.error("调用百度以图搜图接口失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    public static void main(String[] args) {
        String imageUrl = "https://www.codefather.cn/logo.png";
        String result = getImagePageUrl(imageUrl);
        System.out.println("搜索成功，结果：" + result);
    }
}
