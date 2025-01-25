package com.by.cloud.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.by.cloud.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.by.cloud.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.by.cloud.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author lzh
 */
@Slf4j
@Component
public class AliYunAiApi {

    @Value("${aliyun.ai.api-key}")
    private String apiKey;

    /**
     * 创建任务请求地址
     */
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    /**
     * 查询任务结果请求地址
     */
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建AI扩图任务
     *
     * @param createOutPaintingTaskRequest 扩图参数
     * @return 响应结果
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        if (createOutPaintingTaskRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "扩图参数为空");
        }
        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                // 开启异步处理
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        // 获取结果
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("创建任务接口请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 扩图失败");
            }
            CreateOutPaintingTaskResponse createOutPaintingTaskResponse = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            if (createOutPaintingTaskResponse.getCode() != null) {
                String errorMessage = createOutPaintingTaskResponse.getMessage();
                log.error("创建任务接口请求异常：{}", errorMessage);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 扩图失败");
            }
            return createOutPaintingTaskResponse;
        }
    }

    /**
     * 查询任务结果
     *
     * @param taskId 任务ID
     * @return 执行结果
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务ID为空");
        }
        // 发送请求
        String url = String.format(GET_OUT_PAINTING_TASK_URL, taskId);
        try (HttpResponse httpResponse = HttpRequest.get(url)
                .header("Authorization", "Bearer " + apiKey)
                .execute()) {
            // 处理响应
            if (!httpResponse.isOk()) {
                log.error("查询任务接口请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取任务失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }
}
