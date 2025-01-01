package com.by.cloud.model.dto.picture;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 图片抓取请求体
 *
 * @author lzh
 */
@Data
public class PictureBatchDto implements Serializable {

    @ApiModelProperty("搜索关键词")
    private String searchText;

    @ApiModelProperty("抓取数量")
    private Integer count = 10;

    @ApiModelProperty("图片名称前缀")
    private String namePrefix;
}
