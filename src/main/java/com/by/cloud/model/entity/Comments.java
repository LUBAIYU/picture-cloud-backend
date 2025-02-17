package com.by.cloud.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体
 *
 * @author lzh
 */
@TableName(value = "comments")
@Data
public class Comments {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 图片ID
     */
    private Long picId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 父级评论ID
     */
    private Long parentId;

    /**
     * 0-待审核，1-通过，2-拒绝
     */
    private Integer status;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
}