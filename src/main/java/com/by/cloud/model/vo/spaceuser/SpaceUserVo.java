package com.by.cloud.model.vo.spaceuser;

import com.by.cloud.model.entity.SpaceUser;
import com.by.cloud.model.vo.space.SpaceVo;
import com.by.cloud.model.vo.user.UserVo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author lzh
 */
@Data
public class SpaceUserVo implements Serializable {

    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("空间 id")
    private Long spaceId;

    @ApiModelProperty("用户 id")
    private Long userId;

    @ApiModelProperty("空间角色：viewer/editor/admin")
    private String spaceRole;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @ApiModelProperty("用户信息")
    private UserVo userVo;

    @ApiModelProperty("空间信息")
    private SpaceVo spaceVo;

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 封装类转对象
     *
     * @param spaceUserVo 封装类
     * @return 对象
     */
    public static SpaceUser voToObj(SpaceUserVo spaceUserVo) {
        if (spaceUserVo == null) {
            return null;
        }
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserVo, spaceUser);
        return spaceUser;
    }

    /**
     * 对象转封装类
     *
     * @param spaceUser 对象
     * @return 封装类
     */
    public static SpaceUserVo objToVo(SpaceUser spaceUser) {
        if (spaceUser == null) {
            return null;
        }
        SpaceUserVo spaceUserVo = new SpaceUserVo();
        BeanUtils.copyProperties(spaceUser, spaceUserVo);
        return spaceUserVo;
    }
}
