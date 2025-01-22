package com.by.cloud.model.vo.picture;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.vo.user.UserVo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
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

    @ApiModelProperty("简介")
    private String introduction;

    @ApiModelProperty("分类")
    private String category;

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

    @ApiModelProperty("创建用户ID")
    private Long userId;

    @ApiModelProperty("空间 id（为空表示公共空间）")
    private Long spaceId;

    @ApiModelProperty("创建用户")
    private UserVo userVo;

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
        // 类型不同进行转换
        picture.setTags(JSONUtil.toJsonStr(pictureVo.getTagList()));
        return picture;
    }

    public static PictureVo objToVo(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVo pictureVo = new PictureVo();
        BeanUtil.copyProperties(picture, pictureVo);
        // 类型不同进行转换
        pictureVo.setTagList(JSONUtil.toList(picture.getTags(), String.class));
        return pictureVo;
    }
}