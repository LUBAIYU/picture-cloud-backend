package com.by.cloud.api.imagesearch.sub;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.by.cloud.api.imagesearch.model.ImageSearchResult;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author lzh
 */
@Slf4j
public class GetImageListApi {

    public static final String DATA = "data";
    public static final String LIST = "list";

    /**
     * 获取图片列表
     *
     * @param imageUrl 图片列表地址
     * @return 图片列表
     */
    public static List<ImageSearchResult> getImageList(String imageUrl) {
        try {
            // 发送请求
            HttpResponse httpResponse = HttpRequest.get(imageUrl).execute();
            // 校验状态码
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            // 解析JSON数据
            String response = httpResponse.body();
            return processResponse(response);
        } catch (Exception e) {
            log.error("获取图片列表失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用失败");
        }
    }

    /**
     * 处理响应
     *
     * @param response 结果字符串
     * @return 图片列表
     */
    private static List<ImageSearchResult> processResponse(String response) {
        // 解析响应对象
        JSONObject jsonObject = new JSONObject(response);
        if (!jsonObject.containsKey(DATA)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到图片列表");
        }
        JSONObject dataObject = jsonObject.getJSONObject(DATA);
        if (!dataObject.containsKey(LIST)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到图片列表");
        }
        JSONArray jsonArray = dataObject.getJSONArray(LIST);
        return JSONUtil.toList(jsonArray, ImageSearchResult.class);
    }

    public static void main(String[] args) {
        String imageUrl = "https://graph.baidu.com/ajax/pcsimi?carousel=503&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&inspire=general_pc&limit=30&next=2&render_type=card&session_id=11556632806729372880&sign=126afe97cd54acd88139901737251403&tk=18e40&tpl_from=pc";
        List<ImageSearchResult> imageList = getImageList(imageUrl);
        System.out.println("搜索成功，" + imageList);
    }
}
