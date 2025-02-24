package com.by.cloud.api.baiduai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.by.cloud.api.baiduai.model.TextAuditResponse;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 百度AI接口
 *
 * @author lzh
 */
@Slf4j
@Component
public class BaiduAiApi {

    @Value("${baidu.ai.api-key}")
    private String apiKey;

    @Value("${baidu.ai.secret-key}")
    private String secretKey;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    /**
     * AI审核文本
     *
     * @param text 文本
     * @return 审核结果
     */
    public TextAuditResponse aiTextAudit(String text) throws IOException {
        // 参数校验
        if (StrUtil.isBlank(text)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核文本不能为空");
        }

        // 使用FormBody将参数进行URL编码
        FormBody formBody = new FormBody.Builder()
                .add("text", text)
                .build();

        // 创建请求并发送
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/rest/2.0/solution/v1/text_censor/v2/user_defined?access_token=" + getAccessToken())
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept", "application/json")
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        if (response.body() == null) {
            log.error("AI 文本审核接口调用失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取文本审核结果失败");
        }
        return JSONUtil.toBean(response.body().string(), TextAuditResponse.class);
    }

    /**
     * 从用户的AK，SK生成鉴权签名（Access Token）
     *
     * @return 鉴权签名（Access Token）
     * @throws IOException IO异常
     */
    public String getAccessToken() throws IOException {
        // 判断 Redis 中是否存在，存在直接返回
        String key = "baidu:accessToken";
        String accessToken = (String) redisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(accessToken)) {
            return accessToken;
        }

        // 请求百度接口获取Token
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody requestBody = RequestBody.Companion.create("", mediaType);
        // 请求地址
        String requestUrl = "https://aip.baidubce.com/oauth/2.0/token?client_id=" + apiKey + "&client_secret=" + secretKey + "&grant_type=client_credentials";
        // 发送请求
        Request request = new Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        if (response.body() == null) {
            log.error("获取 AccessToken 接口调用失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取 AccessToken 接口调用失败");
        }
        accessToken = new JSONObject(response.body().string()).get("access_token").toString();

        // 存入 Redis
        redisTemplate.opsForValue().set(key, accessToken, 2, TimeUnit.HOURS);

        return accessToken;
    }
}
