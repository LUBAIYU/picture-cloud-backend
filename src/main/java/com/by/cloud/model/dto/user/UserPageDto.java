package com.by.cloud.model.dto.user;

import com.by.cloud.common.PageRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author lzh
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserPageDto extends PageRequest implements Serializable {

    @ApiModelProperty("账号")
    private String userAccount;

    @ApiModelProperty("用户昵称")
    private String userName;

    @ApiModelProperty("用户状态：0-不可用/1-可用")
    private Integer userStatus;

    @ApiModelProperty("用户角色：0-管理员/1-用户")
    private Integer userRole;
}
