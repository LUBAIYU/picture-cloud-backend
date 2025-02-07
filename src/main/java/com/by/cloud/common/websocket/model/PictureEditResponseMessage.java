package com.by.cloud.common.websocket.model;

import com.by.cloud.model.vo.user.UserVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片编辑请求响应消息
 *
 * @author lzh
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditResponseMessage {

    @ApiModelProperty("消息类型，例如 \"INFO\", \"ERROR\", \"ENTER_EDIT\", \"EXIT_EDIT\", \"EDIT_ACTION\"")
    private String type;

    @ApiModelProperty("信息")
    private String message;

    @ApiModelProperty("执行的编辑动作")
    private String editAction;

    @ApiModelProperty("用户信息")
    private UserVo user;
}
