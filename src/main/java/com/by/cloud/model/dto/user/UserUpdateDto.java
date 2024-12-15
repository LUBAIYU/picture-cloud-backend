package com.by.cloud.model.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class UserUpdateDto implements Serializable {

    @ApiModelProperty("ID")
    private Long userId;

    @ApiModelProperty("账号")
    private String userAccount;

    @ApiModelProperty("用户昵称")
    private String userName;

    @ApiModelProperty("用户头像")
    private String userAvatar;

    @ApiModelProperty("用户简介")
    private String userProfile;

    @ApiModelProperty("用户角色：0-管理员/1-用户")
    private Integer userRole;
}
