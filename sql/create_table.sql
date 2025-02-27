use picture_cloud;
-- 用户表
create table if not exists user
(
    user_id         bigint auto_increment comment 'id' primary key,
    user_account    varchar(256)                       not null comment '账号',
    user_password   varchar(512)                       not null comment '密码',
    user_name       varchar(256)                       null comment '用户昵称',
    user_avatar     varchar(1024)                      null comment '用户头像',
    user_profile    varchar(512)                       null comment '用户简介',
    user_status     tinyint  default 1                 not null comment '用户状态：0-不可用/1-可用',
    user_role       tinyint  default 1                 not null comment '用户角色：0-管理员/1-用户',
    vip_expire_time datetime                           null comment '会员过期时间',
    vip_code        varchar(128)                       null comment '会员兑换码',
    vip_number      bigint                             null comment '会员编号',
    edit_time       datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    create_time     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete       tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uk_user_account (user_account),
    INDEX idx_user_name (user_name)
) comment '用户表' collate = utf8mb4_unicode_ci;

-- 图片表
create table if not exists picture
(
    pic_id         bigint auto_increment comment 'id' primary key,
    pic_url        varchar(512)                       not null comment '图片 url',
    thumbnail_url  varchar(512)                       null comment '缩略图 url',
    pic_name       varchar(128)                       not null comment '图片名称',
    introduction   varchar(512)                       null comment '简介',
    pic_size       bigint                             null comment '图片体积',
    pic_width      int                                null comment '图片宽度',
    pic_height     int                                null comment '图片高度',
    pic_scale      double                             null comment '图片宽高比例',
    pic_format     varchar(32)                        null comment '图片格式',
    pic_color      varchar(16)                        null comment '图片主色调',
    category_id    bigint                             null comment '分类 id',
    user_id        bigint                             not null comment '创建用户 id',
    space_id       bigint                             null comment '空间 id (为空表示公共空间)',
    like_count     bigint   default 0                 not null comment '点赞数',
    review_status  int      default 0                 not null comment '审核状态：0-待审核；1-通过；2-拒绝',
    review_message varchar(512)                       null comment '审核信息',
    reviewer_id    bigint                             null comment '审核人ID',
    review_time    datetime                           null comment '审核时间',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    edit_time      datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete      tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (pic_name),               -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction),   -- 用于模糊搜索图片简介
    INDEX idx_category_id (category_id),     -- 提升基于分类 ID 的查询性能
    INDEX idx_user_id (user_id),             -- 提升基于用户 ID 的查询性能
    INDEX idx_review_status (review_status), -- 提升基于审核状态的查询性能
    INDEX idx_space_id (space_id)            -- 提升基于空间 ID 的查询性能
) comment '图片表' collate = utf8mb4_unicode_ci;

-- 分类表
create table if not exists category
(
    id          bigint auto_increment comment '主键ID' primary key,
    name        varchar(256)                       not null comment '分类名称',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uk_name (name) -- 唯一索引，防止名称重复
) comment '分类表' collate = utf8mb4_unicode_ci;

-- 标签表
create table if not exists tag
(
    id          bigint auto_increment comment '主键ID' primary key,
    name        varchar(256)                       not null comment '标签名称',
    use_count   int      default 0                 not null comment '使用次数',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uk_name (name) -- 唯一索引，防止名称重复
) comment '标签表' collate = utf8mb4_unicode_ci;

-- 图片标签关联表
create table if not exists picture_tag
(
    id         bigint auto_increment comment '主键ID' primary key,
    picture_id bigint not null comment '图片ID',
    tag_id     bigint not null comment '标签ID'
) comment '图片标签关联表' collate = utf8mb4_unicode_ci;

-- 空间表
create table if not exists space
(
    id          bigint auto_increment comment 'id' primary key,
    space_name  varchar(128)                       null comment '空间名称',
    space_level int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    space_type  int      default 0                 not null comment '空间类型：0-私有 1-团队',
    max_size    bigint   default 0                 null comment '空间图片的最大总大小',
    max_count   bigint   default 0                 null comment '空间图片的最大数量',
    total_size  bigint   default 0                 null comment '当前空间下图片的总大小',
    total_count bigint   default 0                 null comment '当前空间下的图片数量',
    user_id     bigint                             not null comment '创建用户 id',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    edit_time   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 not null comment '是否删除',
    -- 索引设计
    index idx_user_id (user_id),         -- 提升基于用户的查询效率
    index idx_space_name (space_name),   -- 提升基于空间名称的查询效率
    index idx_space_level (space_level), -- 提升按空间级别查询的效率
    index idx_space_type (space_type)    -- 提升按空间类型查询的效率
) comment '空间表' collate = utf8mb4_unicode_ci;

-- 空间成员表
create table if not exists space_user
(
    id          bigint auto_increment comment 'id' primary key,
    space_id    bigint                                 not null comment '空间 id',
    user_id     bigint                                 not null comment '用户 id',
    space_role  varchar(128) default 'viewer'          null comment '空间角色：viewer/editor/admin',
    create_time datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    -- 索引设计
    UNIQUE KEY uk_spaceId_userId (space_id, user_id), -- 唯一索引，用户在一个空间中只能有一个角色
    INDEX idx_space_id (space_id),                    -- 提升按空间查询的性能
    INDEX idx_user_id (user_id)                       -- 提升按用户查询的性能
) comment '空间用户关联' collate = utf8mb4_unicode_ci;

-- 评论表
create table if not exists comments
(
    id          bigint auto_increment comment '主键ID' primary key,
    content     text                               not null comment '评论内容',
    pic_id      bigint                             not null comment '图片ID',
    user_id     bigint                             not null comment '用户ID',
    parent_id   bigint                             null comment '父级评论ID',
    status      tinyint  default 0                 not null comment '0-已提交，1-审核中，2-通过，3-拒绝，4-失败',
    like_count  int      default 0                 not null comment '点赞数',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 not null comment '是否删除',
    -- 索引设计
    INDEX idx_pic_id (pic_id),      -- 提升基于图片ID的查询性能
    INDEX idx_user_id (user_id),    -- 提升基于用户ID的查询性能
    INDEX idx_parent_id (parent_id) -- 提升基于父级评论ID的查询性能
) comment '评论表' collate = utf8mb4_unicode_ci;

-- 评论点赞表
create table if not exists comment_likes
(
    id          bigint auto_increment comment '主键ID' primary key,
    comment_id  bigint                             not null comment '评论ID',
    user_id     bigint                             not null comment '用户ID',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    -- 索引设计
    UNIQUE KEY uk_commentId_userId (comment_id, user_id),-- 唯一索引，防止用户重复点赞
    INDEX idx_user_id (user_id)                          -- 查询用户的点赞记录
) comment '评论点赞表' collate = utf8mb4_unicode_ci;

-- 评论审核表
create table if not exists comment_reviews
(
    id            bigint auto_increment comment '主键ID' primary key,
    comment_id    bigint                             not null comment '评论ID',
    reviewer_id   bigint                             not null comment '审核人ID',
    review_status tinyint                            not null comment '审核状态：0-审核通过；1-审核拒绝；2-系统异常',
    review_msg    varchar(255)                       null comment '审核信息',
    review_time   datetime default CURRENT_TIMESTAMP not null comment '审核时间'
) comment '评论审核表' collate = utf8mb4_unicode_ci;

-- 图片点赞记录表
create table if not exists `picture_likes`
(
    id          bigint auto_increment comment '主键ID' primary key,
    picture_id  bigint                             not null comment '图片ID',
    user_id     bigint                             not null comment '用户ID',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    -- 索引设计
    INDEX idx_picture_id (picture_id),
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_pictureId_userId (picture_id, user_id)
) comment '图片点赞记录表' collate = utf8mb4_unicode_ci;