package com.by.cloud.model.dto.picture;

import com.by.cloud.common.PageRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * @author lzh
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PicturePageDto extends PageRequest implements Serializable {

    @ApiModelProperty("图片名称")
    private String picName;

    @ApiModelProperty("简介")
    private String introduction;

    @ApiModelProperty("分类")
    private String category;

    @ApiModelProperty("标签列表")
    private List<String> tagList;

    @ApiModelProperty("搜索关键词")
    private String searchText;
}
