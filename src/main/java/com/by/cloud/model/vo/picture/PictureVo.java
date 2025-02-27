package com.by.cloud.model.vo.picture;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.vo.user.UserVo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 图片返回类
 *
 * @author lzh
 */
@Data
public class PictureVo implements Serializable {

    @ApiModelProperty("图片ID")
    private Long picId;

    @ApiModelProperty("未转为 webp 格式的原始图片 url")
    private String rawUrl;

    @ApiModelProperty("图片url")
    private String picUrl;

    @ApiModelProperty("缩略图url")
    private String thumbnailUrl;

    @ApiModelProperty("图片名称")
    private String picName;

    @ApiModelProperty("分类")
    private String category;

    @ApiModelProperty("简介")
    private String introduction;

    @ApiModelProperty("标签（列表）")
    private List<String> tagList;

    @ApiModelProperty("图片体积")
    private Long picSize;

    @ApiModelProperty("图片宽度")
    private Integer picWidth;

    @ApiModelProperty("图片高度")
    private Integer picHeight;

    @ApiModelProperty("图片宽高比例")
    private Double picScale;

    @ApiModelProperty("图片格式")
    private String picFormat;

    @ApiModelProperty("图片主色调")
    private String picColor;

    @ApiModelProperty("分类id")
    private Long categoryId;

    @ApiModelProperty("创建用户ID")
    private Long userId;

    @ApiModelProperty("空间 id（为空表示公共空间）")
    private Long spaceId;

    @ApiModelProperty("点赞数")
    private Long likeCount;

    @ApiModelProperty("创建用户")
    private UserVo userVo;

    @ApiModelProperty("核状态：0-待审核；1-通过；2-拒绝")
    private Integer reviewStatus;

    @ApiModelProperty("审核信息")
    private String reviewMessage;

    @ApiModelProperty("审核人ID")
    private Long reviewerId;

    @ApiModelProperty("权限列表")
    private List<String> permissionList = Collections.emptyList();

    @ApiModelProperty("评论总数")
    private Long commentsCount;

    @ApiModelProperty("审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "YYYY-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("编辑时间")
    @JsonFormat(pattern = "YYYY-MM-dd HH:mm:ss")
    private LocalDateTime editTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "YYYY-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public static Picture voToObj(PictureVo pictureVo) {
        if (pictureVo == null) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureVo, picture);
        return picture;
    }

    public static PictureVo objToVo(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVo pictureVo = new PictureVo();
        BeanUtil.copyProperties(picture, pictureVo);
        return pictureVo;
    }
}