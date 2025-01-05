package com.by.cloud.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图片分类标签关联表
 *
 * @author lzh
 */
@TableName(value = "picture_category_tag")
@Data
public class PictureCategoryTag implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 标签ID
     */
    private Long tagId;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}