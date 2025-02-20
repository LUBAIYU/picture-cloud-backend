package com.by.cloud.api.baiduai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 文本审核响应类
 *
 * @author lzh
 */
@Data
public class TextAuditResponse {

    @JsonProperty("conclusion")
    private String conclusion;

    @JsonProperty("log_id")
    private String logId;

    @JsonProperty("phoneRisk")
    private Map<String, Object> phoneRisk;

    @JsonProperty("data")
    private List<DataItem> data;

    @JsonProperty("isHitMd5")
    private Boolean isHitMd5;

    @JsonProperty("conclusionType")
    private Integer conclusionType;

    /**
     * 内部数据项类
     */
    @Data
    public static class DataItem {

        @JsonProperty("msg")
        private String msg;

        @JsonProperty("conclusion")
        private String conclusion;

        @JsonProperty("hits")
        private List<Hit> hits;

        @JsonProperty("subType")
        private Integer subType;

        @JsonProperty("type")
        private Integer type;
    }

    /**
     * 内部命中项类
     */
    @Data
    public static class Hit {

        @JsonProperty("probability")
        private Double probability;

        @JsonProperty("datasetName")
        private String datasetName;

        @JsonProperty("words")
        private List<String> words;

        @JsonProperty("modelHitPositions")
        private List<List<Integer>> modelHitPositions;
    }
}