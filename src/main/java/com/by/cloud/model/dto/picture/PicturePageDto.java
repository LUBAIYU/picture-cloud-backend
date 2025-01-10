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

    @ApiModelProperty("审核状态：0-待审核；1-通过；2-拒绝")
    private Integer reviewStatus;

    @ApiModelProperty("审核信息")
    private String reviewMessage;

    @ApiModelProperty("空间ID")
    private Long spaceId;

    @ApiModelProperty("是否只查询 spaceId 为 null 的数据")
    private boolean nullSpaceId;
}
