package com.by.cloud.common.websocket.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片编辑请求消息
 *
 * @author lzh
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureEditRequestMessage {

    @ApiModelProperty("消息类型，如'ENTER_EDIT','EXIT_EDIT',''EDIT_ACTION")
    private String type;

    @ApiModelProperty("编辑动作，如放大、缩小")
    private String editAction;
}
