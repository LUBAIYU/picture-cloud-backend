package com.by.cloud.model.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author lzh
 */
@Data
public class UserVo implements Serializable {

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

    @ApiModelProperty("用户状态：0-不可用/1-可用")
    private Integer userStatus;

    @ApiModelProperty("用户角色：0-管理员/1-用户")
    private Integer userRole;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
