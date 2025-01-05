use picture_cloud;
-- 用户表
create table if not exists user
(
    user_id       bigint auto_increment comment 'id' primary key,
    user_account  varchar(256)                       not null comment '账号',
    user_password varchar(512)                       not null comment '密码',
    user_name     varchar(256)                       null comment '用户昵称',
    user_avatar   varchar(1024)                      null comment '用户头像',
    user_profile  varchar(512)                       null comment '用户简介',
    user_status   tinyint  default 1                 not null comment '用户状态：0-不可用/1-可用',
    user_role     tinyint  default 1                 not null comment '用户角色：0-管理员/1-用户',
    edit_time     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uk_user_account (user_account),
    INDEX idx_user_name (user_name)
) comment '用户表' collate = utf8mb4_unicode_ci;

-- 图片表
create table if not exists picture
(
    pic_id         bigint auto_increment comment 'id' primary key,
    pic_url        varchar(512)                       not null comment '图片 url',
    pic_name       varchar(128)                       not null comment '图片名称',
    introduction   varchar(512)                       null comment '简介',
    category       varchar(64)                        null comment '分类',
    tags           varchar(512)                       null comment '标签（JSON 数组）',
    pic_size       bigint                             null comment '图片体积',
    pic_width      int                                null comment '图片宽度',
    pic_height     int                                null comment '图片高度',
    pic_scale      double                             null comment '图片宽高比例',
    pic_format     varchar(32)                        null comment '图片格式',
    user_id        bigint                             not null comment '创建用户 id',
    review_status  int      default 0                 not null comment '审核状态：0-待审核；1-通过；2-拒绝',
    review_message varchar(512)                       null comment '审核信息',
    reviewer_id    bigint                             null comment '审核人ID',
    review_time    datetime                           null comment '审核时间',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    edit_time      datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete      tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (pic_name),              -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction),  -- 用于模糊搜索图片简介
    INDEX idx_category (category),          -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                  -- 提升基于标签的查询性能
    INDEX idx_user_id (user_id),            -- 提升基于用户 ID 的查询性能
    INDEX idx_review_status (review_status) -- 提升基于审核状态的查询性能
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

-- 图片分类标签关联表
create table if not exists picture_category_tag
(
    id          bigint auto_increment comment '主键ID' primary key,
    picture_id  bigint not null comment '图片ID',
    category_id bigint not null comment '分类ID',
    tag_id      bigint not null comment '标签ID'
) comment '图片分类标签关联表' collate = utf8mb4_unicode_ci;