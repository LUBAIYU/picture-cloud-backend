package com.by.cloud.model.vo.space;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.by.cloud.model.entity.Space;
import com.by.cloud.model.vo.user.UserVo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 空间返回类
 *
 * @author lzh
 */
@Data
public class SpaceVo implements Serializable {

    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("空间名称")
    private String spaceName;

    @ApiModelProperty("空间级别：0-普通版 1-专业版 2-旗舰版")
    private Integer spaceLevel;

    @ApiModelProperty("空间图片的最大总大小")
    private Long maxSize;

    @ApiModelProperty("空间图片的最大数量")
    private Long maxCount;

    @ApiModelProperty("当前空间下图片的总大小")
    private Long totalSize;

    @ApiModelProperty("当前空间下的图片数量")
    private Long totalCount;

    @ApiModelProperty("创建用户 id")
    private Long userId;

    @ApiModelProperty("创建用户")
    private UserVo userVo;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("编辑时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime editTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public static Space voToObj(SpaceVo spaceVo) {
        if (spaceVo == null) {
            return null;
        }
        Space space = new Space();
        BeanUtil.copyProperties(spaceVo, space);
        return space;
    }

    public static SpaceVo objToVo(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVo spaceVo = new SpaceVo();
        BeanUtil.copyProperties(space, spaceVo);
        return spaceVo;
    }
}