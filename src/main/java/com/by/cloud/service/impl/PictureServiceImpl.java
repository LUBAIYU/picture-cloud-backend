package com.by.cloud.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.api.aliyunai.AliYunAiApi;
import com.by.cloud.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.by.cloud.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.by.cloud.api.imagesearch.ImageSearchApiFacade;
import com.by.cloud.api.imagesearch.model.ImageSearchResult;
import com.by.cloud.common.CosManager;
import com.by.cloud.common.PageResult;
import com.by.cloud.common.auth.SpaceUserAuthManager;
import com.by.cloud.common.auth.StpKit;
import com.by.cloud.common.upload.BasePictureUploadTemplate;
import com.by.cloud.common.upload.FilePictureUpload;
import com.by.cloud.common.upload.UrlPictureUpload;
import com.by.cloud.constants.PictureConstant;
import com.by.cloud.constants.SpaceUserPermissionConstant;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.PictureReviewStatusEnum;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.mapper.PictureMapper;
import com.by.cloud.model.dto.file.UploadPictureResult;
import com.by.cloud.model.dto.picture.*;
import com.by.cloud.model.entity.*;
import com.by.cloud.model.vo.category.CategoryListVo;
import com.by.cloud.model.vo.picture.PictureTagCategoryVo;
import com.by.cloud.model.vo.picture.PictureVo;
import com.by.cloud.model.vo.tag.TagListVo;
import com.by.cloud.model.vo.user.UserVo;
import com.by.cloud.service.*;
import com.by.cloud.utils.ColorSimilarUtils;
import com.by.cloud.utils.ThrowUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author lzh
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    private final UserService userService;
    private final FilePictureUpload filePictureUpload;
    private final UrlPictureUpload urlPictureUpload;
    private final StringRedisTemplate stringRedisTemplate;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final PictureTagService pictureTagService;
    private final CosManager cosManager;
    private final SpaceService spaceService;
    private final TransactionTemplate transactionTemplate;
    private final PictureMapper pictureMapper;
    private final AliYunAiApi aliYunAiApi;
    private final SpaceUserAuthManager spaceUserAuthManager;
    private final CommentsService commentsService;

    /**
     * 本地缓存
     */
    public static final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            // 最大 10000 条
            .maximumSize(10_000L)
            // 缓存 5 分钟
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    @Override
    public PictureVo uploadPicture(Object inputSource, PictureUploadDto dto, HttpServletRequest request) {
        // 获取登录用户ID
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getUserId();

        // 校验空间是否存在
        Long spaceId = null;
        if (dto != null && dto.getSpaceId() != null) {
            spaceId = dto.getSpaceId();
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间允许图片条数已满");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间允许图片总大小已满");
            }
        }

        // 判断是新增还是修改
        Long picId = null;
        if (dto != null) {
            picId = dto.getPicId();
        }
        if (picId != null) {
            Picture picture = this.lambdaQuery().eq(Picture::getPicId, picId).one();
            ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 更新图片则需校验空间是否一致
            if (spaceId == null) {
                if (picture.getSpaceId() != null) {
                    spaceId = picture.getSpaceId();
                }
            } else {
                // 传了空间ID则需校验是否和原图的空间ID一致
                if (!ObjectUtil.equals(spaceId, picture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间ID不一致");
                }
            }
        }

        // 上传图片获取图片信息
        String uploadPathPrefix;
        if (spaceId == null) {
            //公共图库
            uploadPathPrefix = String.format("public/%s", loginUserId);
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }

        // 根据输入源类型调用不同的上传逻辑
        BasePictureUploadTemplate basePictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            basePictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = basePictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 构造入库信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        BeanUtil.copyProperties(uploadPictureResult, picture);
        // 设置自定义图片名称
        if (dto != null && StrUtil.isNotBlank(dto.getPicName())) {
            picture.setPicName(dto.getPicName());
        }
        picture.setUserId(loginUserId);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        if (picId != null) {
            picture.setPicId(picId);
            picture.setEditTime(LocalDateTime.now());
        }

        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.executeWithoutResult(status -> {
            // 获取代理对象
            PictureService proxyService = (PictureService) AopContext.currentProxy();
            // 使用代理对象调用，防止事务失效
            boolean result = proxyService.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

            // 更新空间额度
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("total_size = total_size + " + picture.getPicSize())
                        .setSql("total_count = total_count + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR);
            }
        });

        return PictureVo.objToVo(picture);
    }

    @Override
    public PictureVo getPictureVo(Long picId, HttpServletRequest request) {
        // 判断图片是否存在
        Picture picture = this.getById(picId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        // 校验权限
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId != null) {
            // 判断是否有权限
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            // 获取空间
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 获取权限列表
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser.getUserId());

        // 获取封装类
        PictureVo pictureVo = PictureVo.objToVo(picture);
        pictureVo.setPermissionList(permissionList);

        // 设置分类
        Long categoryId = pictureVo.getCategoryId();
        if (categoryId != null) {
            Category category = categoryService.lambdaQuery()
                    .eq(Category::getId, categoryId)
                    .one();
            if (category != null) {
                pictureVo.setCategory(category.getName());
            }
        }

        // 设置标签列表
        // 获取图片标签关联数据
        List<PictureTag> pictureTagList = pictureTagService.lambdaQuery()
                .eq(PictureTag::getPictureId, picId)
                .list();
        if (CollUtil.isNotEmpty(pictureTagList)) {
            List<Long> tagIdList = pictureTagList.stream().map(PictureTag::getTagId).toList();
            // 获取标签列表
            List<String> tagList = tagService.lambdaQuery()
                    .in(Tag::getId, tagIdList)
                    .list()
                    .stream()
                    .map(Tag::getName)
                    .toList();
            pictureVo.setTagList(tagList);
        }

        // 设置图片的评论总数
        long count = commentsService.getCommentCountByPicId(pictureVo.getPicId());
        pictureVo.setCommentsCount(count);

        // 设置图片的创建用户信息
        Long userId = picture.getUserId();
        User user = userService.getById(userId);
        UserVo userVo = BeanUtil.copyProperties(user, UserVo.class);
        pictureVo.setUserVo(userVo);
        return pictureVo;
    }

    @Override
    public PageResult<Picture> queryPictureByPage(PicturePageDto pageDto) {
        // 校验参数
        int current = pageDto.getCurrent();
        int pageSize = pageDto.getPageSize();
        ThrowUtils.throwIf(current <= 0 || pageSize <= 0, ErrorCode.PARAMS_ERROR);
        // 构建分页对象
        IPage<Picture> pageParams = new Page<>(current, pageSize);
        // 查询
        IPage<Picture> pageRes = pictureMapper.queryPictureByPage(pageParams, pageDto);
        // 返回
        return PageResult.of(pageRes.getTotal(), pageRes.getRecords());
    }

    @Override
    public PageResult<PictureVo> queryPictureVoByPage(PicturePageDto pageDto) {
        // 先查询所有信息
        PageResult<Picture> pageResult = this.queryPictureByPage(pageDto);

        // 如果为空直接返回
        List<Picture> records = pageResult.getRecords();
        if (CollUtil.isEmpty(records)) {
            return PageResult.of(0L, Collections.emptyList());
        }

        // 对象列表 => VO列表
        List<PictureVo> pictureVoList = records.stream().map(PictureVo::objToVo).toList();

        // 关联查询分类
        // 获取去重的分类ID列表
        Set<Long> categoryIdSet = records.stream().
                map(Picture::getCategoryId).collect(Collectors.toSet());
        // 查询分类ID列表对应的分类信息
        Map<Long, List<Category>> categoryIdCategoryListMap;
        if (CollUtil.isNotEmpty(categoryIdSet)) {
            categoryIdCategoryListMap = categoryService.listByIds(categoryIdSet)
                    .stream()
                    .collect(Collectors.groupingBy(Category::getId));
        } else {
            categoryIdCategoryListMap = null;
        }

        // 关联查询用户信息
        // 获取去重的用户ID列表
        Set<Long> userIdSet = records.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 查询用户ID列表对应的用户信息
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getUserId));

        // 设置分类和用户信息
        pictureVoList.forEach(pictureVo -> {
            Long categoryId = pictureVo.getCategoryId();
            Long userId = pictureVo.getUserId();
            if (categoryIdCategoryListMap != null && categoryIdCategoryListMap.containsKey(categoryId)) {
                Category category = categoryIdCategoryListMap.get(categoryId).get(0);
                pictureVo.setCategory(category.getName());
            }
            if (userIdUserListMap.containsKey(userId)) {
                User user = userIdUserListMap.get(userId).get(0);
                pictureVo.setUserVo(BeanUtil.copyProperties(user, UserVo.class));
            }
        });

        // 定义一个Map用于存储每个图片对应的标签列表
        Map<Long, List<String>> pictureIdTagListMap = new HashMap<>(records.size());

        // 关联查询标签信息
        List<Long> pictureIdList = records.stream().map(Picture::getPicId).toList();
        List<PictureTag> pictureTagList = pictureTagService.lambdaQuery()
                .in(PictureTag::getPictureId, pictureIdList)
                .list();
        // 无关联标签直接返回
        if (CollUtil.isEmpty(pictureTagList)) {
            return PageResult.of(pageResult.getTotal(), pictureVoList);
        }
        // 获取图片ID对应的标签列表
        Map<Long, List<PictureTag>> pictureIdPictureTagListMap = pictureTagList.stream()
                .collect(Collectors.groupingBy(PictureTag::getPictureId));
        pictureIdPictureTagListMap.keySet().forEach(pictureId -> {
            // 获取每个图片ID对应的标签ID列表
            List<Long> tagIdList = pictureIdPictureTagListMap.get(pictureId)
                    .stream()
                    .map(PictureTag::getTagId)
                    .toList();
            // 批量查询标签信息
            List<String> tagList = tagService.lambdaQuery()
                    .in(Tag::getId, tagIdList)
                    .list()
                    .stream().map(Tag::getName)
                    .toList();
            // 存入Map
            pictureIdTagListMap.put(pictureId, tagList);
        });

        // 获取每个图片的评论数
        Map<Long, Long> picIdCommentsCountMap = commentsService.queryBatchCommentCount(pictureIdList);

        // 设置标签信息和评论数
        for (PictureVo pictureVo : pictureVoList) {
            Long picId = pictureVo.getPicId();
            if (pictureIdTagListMap.containsKey(picId)) {
                pictureVo.setTagList(pictureIdTagListMap.get(picId));
            }
            if (picIdCommentsCountMap.containsKey(picId)) {
                pictureVo.setCommentsCount(picIdCommentsCountMap.get(picId));
            }
        }

        // 封装返回
        return PageResult.of(pageResult.getTotal(), pictureVoList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePicture(PictureUpdateDto updateDto, HttpServletRequest request) {
        // 校验参数
        Long picId = updateDto.getPicId();
        Long categoryId = updateDto.getCategoryId();
        List<Long> tagIdList = updateDto.getTagIdList();
        if (picId == null || picId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (categoryId != null && categoryId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        this.validIdList(tagIdList);

        // 复制参数
        Picture picture = new Picture();
        BeanUtil.copyProperties(updateDto, picture);

        // 如果有传标签，则更新标签信息及关联
        if (CollUtil.isNotEmpty(tagIdList)) {
            // 开启事务
            transactionTemplate.executeWithoutResult(transactionStatus -> {
                // 查询是否有关联
                boolean exists = pictureTagService.lambdaQuery()
                        .eq(PictureTag::getPictureId, picId)
                        .exists();
                if (exists) {
                    // 删除关联
                    pictureTagService.lambdaUpdate()
                            .eq(PictureTag::getPictureId, picId)
                            .remove();
                }
                // 插入新的关联数据
                List<PictureTag> pictureTagList = new ArrayList<>();
                for (Long tagId : tagIdList) {
                    PictureTag pictureTag = new PictureTag();
                    pictureTag.setPictureId(picId);
                    pictureTag.setTagId(tagId);
                    pictureTagList.add(pictureTag);
                }
                pictureTagService.saveBatch(pictureTagList);
            });
        }

        // 校验
        this.validPicture(picture);

        // 判断ID是否存在
        Picture dbPicture = this.getById(picId);
        ThrowUtils.throwIf(dbPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 填充审核参数
        this.fillReviewParams(picture, userService.getLoginUser(request));

        // 更新数据
        boolean updated = this.updateById(picture);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public void validPicture(Picture picture) {
        // 获取校验字段
        String picName = picture.getPicName();
        String introduction = picture.getIntroduction();
        String picUrl = picture.getPicUrl();
        // 校验
        if (StrUtil.isNotBlank(picName)) {
            ThrowUtils.throwIf(picName.length() > PictureConstant.MAX_NAME_LENGTH, ErrorCode.PARAMS_ERROR, "图片名称过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > PictureConstant.MAX_INTRODUCTION_LENGTH, ErrorCode.PARAMS_ERROR, "图片简介过长");
        }
        if (StrUtil.isNotBlank(picUrl)) {
            ThrowUtils.throwIf(picUrl.length() > PictureConstant.MAX_URL_LENGTH, ErrorCode.PARAMS_ERROR, "图片URL过长");
        }
    }

    @Override
    public void pictureReview(PictureReviewDto reviewDto, HttpServletRequest request) {
        // 1.校验参数
        Long id = reviewDto.getId();
        Integer reviewStatus = reviewDto.getReviewStatus();
        String reviewMessage = reviewDto.getReviewMessage();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.UNREVIEWED.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2.判断图片是否存在
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        // 3.校验审核状态是否重复
        if (picture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }

        // 4.数据库操作
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getUserId();

        Picture updatePicture = new Picture();
        updatePicture.setPicId(id);
        updatePicture.setReviewStatus(reviewStatus);
        updatePicture.setReviewMessage(reviewMessage);
        updatePicture.setReviewerId(loginUserId);
        updatePicture.setReviewTime(LocalDateTime.now());
        boolean updated = this.updateById(updatePicture);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        // 如果是管理员则自动过审
        if (userService.isAdmin(loginUser)) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getUserId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(LocalDateTime.now());
        } else {
            // 用户则待审核
            picture.setReviewStatus(PictureReviewStatusEnum.UNREVIEWED.getValue());
        }
    }

    @Override
    public int uploadPictureByBatch(PictureBatchDto batchDto, HttpServletRequest request) {
        // 校验参数
        String searchText = batchDto.getSearchText();
        Integer count = batchDto.getCount();
        String namePrefix = batchDto.getNamePrefix();
        // 如果名称前缀为空，则默认为搜索关键词
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        if (StrUtil.isBlank(searchText)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ThrowUtils.throwIf(count == null || count <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多上传30张图片");

        // 抓取图片
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }

        // 解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjectUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element element : imgElementList) {
            String fileUrl = element.attr("src");
            // 如果链接为空，直接跳过
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            // 处理图片地址
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadDto uploadDto = new PictureUploadDto();
            uploadDto.setPicName(namePrefix + (uploadCount + 1));
            try {
                PictureVo pictureVo = this.uploadPicture(fileUrl, uploadDto, request);
                log.info("图片上传成功，id = {}", pictureVo.getPicId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Override
    public PageResult<PictureVo> queryPictureVoByPageWithCache(PicturePageDto pageDto) {
        // 查询条件转JSON，然后再Md5加密
        String questionStr = JSONUtil.toJsonStr(pageDto);
        String hashKey = DigestUtils.md5DigestAsHex(questionStr.getBytes());
        // 构建缓存Key
        String cacheKey = String.format("picture-cloud-backend:queryPictureVoByPage:%s", hashKey);

        // 1.查询本地缓存
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            // 缓存命中，直接返回
            return JSONUtil.toBean(cachedValue, PageResult.class);
        }

        // 2.查询 Redis 缓存
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        cachedValue = opsForValue.get(cacheKey);
        if (cachedValue != null) {
            // 更新本地缓存
            LOCAL_CACHE.put(cacheKey, cachedValue);
            // 缓存命中，直接返回
            return JSONUtil.toBean(cachedValue, PageResult.class);
        }

        // 3.查询数据库
        PageResult<PictureVo> voPageResult = this.queryPictureVoByPage(pageDto);

        // 4.设置过期时间，更新 Redis 缓存
        int cacheTime = 300 + RandomUtil.randomInt(0, 300);
        String resultStr = JSONUtil.toJsonStr(voPageResult);
        opsForValue.set(cacheKey, resultStr, cacheTime, TimeUnit.SECONDS);

        // 5.更新本地缓存
        LOCAL_CACHE.put(cacheKey, resultStr);

        // 返回结果
        return voPageResult;
    }

    @Override
    public PictureTagCategoryVo listPictureTagCategory() {
        // 创建返回对象
        PictureTagCategoryVo pictureTagCategoryVo = new PictureTagCategoryVo();

        // 获取分类名称列表
        List<CategoryListVo> categoryListVos = categoryService.listCategory();
        if (CollUtil.isEmpty(categoryListVos)) {
            pictureTagCategoryVo.setCategoryList(Collections.emptyList());
        } else {
            pictureTagCategoryVo.setCategoryList(categoryListVos.stream()
                    .map(CategoryListVo::getName)
                    .toList());
        }

        // 获取标签名称列表
        List<TagListVo> tagListVos = tagService.listTag();
        if (CollUtil.isEmpty(tagListVos)) {
            pictureTagCategoryVo.setTagList(Collections.emptyList());
        } else {
            pictureTagCategoryVo.setTagList(tagListVos.stream()
                    .map(TagListVo::getName)
                    .toList());
        }

        return pictureTagCategoryVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePictureById(Long picId) {
        // 判断图片是否存在
        Picture picture = this.getById(picId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        // 删除图片与标签的绑定关系
        pictureTagService.lambdaUpdate()
                .eq(PictureTag::getPictureId, picId)
                .remove();

        // 删除远程COS图片
        this.clearPictureFile(picture);

        // 如果是公共图库的图片，则删除缓存
        if (picture.getSpaceId() == null) {
            LOCAL_CACHE.invalidateAll();
            stringRedisTemplate.execute((RedisConnection connection) -> {
                connection.flushDb();
                return null;
            });
        }

        // 开启事务
        transactionTemplate.executeWithoutResult(status -> {
            // 删除
            boolean result = this.removeById(picId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

            // 更新空间额度
            if (picture.getSpaceId() != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, picture.getSpaceId())
                        .setSql("total_size = total_size - " + picture.getPicSize())
                        .setSql("total_count = total_count - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR);
            }
        });
    }

    @Async
    @Override
    public void clearPictureFile(Picture picture) {
        // 校验文件地址是否被多条记录引用
        String picUrl = picture.getPicUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getPicUrl, picUrl)
                .count();
        // 如果大于一条，则说明不止被一条记录引用，不清理
        if (count > 1) {
            return;
        }
        // 删除图片
        cosManager.deleteObject(picUrl);
        // 删除缩略图
        String thumbnailUrl = picture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void checkPictureAuth(Picture picture, HttpServletRequest request) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getUserId();
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!loginUserId.equals(picture.getUserId()) && !userService.isAdmin(loginUserId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!loginUserId.equals(picture.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Override
    public List<ImageSearchResult> searchPictureByPicture(PictureSearchByPictureDto searchByPictureDto) {
        // 校验参数
        Long pictureId = searchByPictureDto.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 搜图
        if (StrUtil.isBlank(picture.getRawUrl())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该格式图片暂时不支持搜图");
        }
        return ImageSearchApiFacade.searchImage(picture.getRawUrl());
    }

    @Override
    public List<PictureVo> searchPictureByColor(Long spaceId, String hexColor, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(hexColor), ErrorCode.PARAMS_ERROR);

        // 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getUserId();
        if (!loginUserId.equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无该空间访问权限");
        }

        // 查询该空间下有主色调的图片
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }

        // 计算相似度并排序
        Color targetColor = Color.decode(hexColor);
        List<Picture> sortedPictureList = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    // 获取图片主色调
                    String hexPicColor = picture.getPicColor();
                    if (StrUtil.isBlank(hexPicColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color picColor = Color.decode(hexPicColor);
                    // 越大越相似，由于默认从小到大排序，取反则逆序
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, picColor);
                }))
                // 取前12个
                .limit(12)
                .toList();

        // 返回结果
        return sortedPictureList.stream().map(PictureVo::objToVo).toList();
    }

    @Override
    public void editPictureByBatch(PictureEditByBatchDto editByBatchDto, HttpServletRequest request) {
        // 校验参数
        List<Long> pictureIdList = editByBatchDto.getPictureIdList();
        Long spaceId = editByBatchDto.getSpaceId();
        Long categoryId = editByBatchDto.getCategoryId();
        List<Long> tagIdList = editByBatchDto.getTagIdList();
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);

        // 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getUserId();
        if (!loginUserId.equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无该空间访问权限");
        }

        // 查询该空间下的指定图片
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getPicId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getPicId, pictureIdList)
                .list();
        if (CollUtil.isEmpty(pictureList)) {
            return;
        }

        // 开启事务
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            // 设置分类
            for (Picture picture : pictureList) {
                if (categoryId != null) {
                    picture.setCategoryId(categoryId);
                }
            }
            if (CollUtil.isNotEmpty(tagIdList)) {
                // 查询是否有图片标签关联表数据
                boolean exists = pictureTagService.lambdaQuery()
                        .in(PictureTag::getPictureId, pictureIdList)
                        .exists();
                if (exists) {
                    // 删除图片标签关联表数据
                    pictureTagService.lambdaUpdate()
                            .in(PictureTag::getPictureId, pictureIdList)
                            .remove();
                }
                // 插入新的关联数据
                List<PictureTag> pictureTagList = new ArrayList<>();
                for (Long pictureId : pictureIdList) {
                    for (Long tagId : tagIdList) {
                        PictureTag pictureTag = new PictureTag();
                        pictureTag.setPictureId(pictureId);
                        pictureTag.setTagId(tagId);
                        pictureTagList.add(pictureTag);
                    }
                }
                pictureTagService.saveBatch(pictureTagList);
            }

            // 批量重命名
            String nameRule = editByBatchDto.getNameRule();
            fillPictureWithNameRule(pictureList, nameRule);

            // 批量更新
            PictureService proxyService = (PictureService) AopContext.currentProxy();
            boolean updated = proxyService.updateBatchById(pictureList);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR);
        });
    }

    @Override
    public CreateOutPaintingTaskResponse createOutPaintingTask(PictureCreateOutPaintingTaskDto createOutPaintingTaskDto) {
        // 校验ID
        Long pictureId = createOutPaintingTaskDto.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        // 判断图片是否存在
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));

        // 设置请求参数
        CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getPicUrl());
        createOutPaintingTaskRequest.setInput(input);
        createOutPaintingTaskRequest.setParameters(createOutPaintingTaskDto.getParameters());
        // 调用返回
        return aliYunAiApi.createOutPaintingTask(createOutPaintingTaskRequest);
    }

    @Override
    public void refreshCache(PicturePageDto pageDto) {
        // 查询条件转JSON，然后再Md5加密
        String questionStr = JSONUtil.toJsonStr(pageDto);
        String hashKey = DigestUtils.md5DigestAsHex(questionStr.getBytes());
        // 构建缓存Key
        String cacheKey = String.format("picture-cloud-backend:queryPictureVoByPage:%s", hashKey);
        // 查询数据库
        PageResult<PictureVo> pageResult = queryPictureVoByPage(pageDto);
        String resultStr = JSONUtil.toJsonStr(pageResult);
        // 更新 Redis 缓存
        int cacheTime = 300 + RandomUtil.randomInt(0, 300);
        stringRedisTemplate.opsForValue().set(cacheKey, resultStr, cacheTime, TimeUnit.SECONDS);
        // 更新本地缓存
        LOCAL_CACHE.put(cacheKey, resultStr);
    }

    @Override
    public List<String> getAllCacheKeys(String prefix) {
        String pattern = "*";
        if (StrUtil.isNotBlank(prefix)) {
            pattern = prefix + "*";
        }
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (CollUtil.isEmpty(keys)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(keys);
    }

    @Override
    public boolean removeCacheByKey(String hashKey) {
        // 判断Key是否存在
        List<String> keyList = getAllCacheKeys(null);
        if (!keyList.contains(hashKey)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 判断本地缓存是否存在该Key
        String localCache = LOCAL_CACHE.getIfPresent(hashKey);
        if (StrUtil.isNotBlank(localCache)) {
            // 删除本地缓存
            LOCAL_CACHE.invalidate(hashKey);
        }
        // 删除指定Key
        Boolean isDeleted = stringRedisTemplate.delete(hashKey);
        ThrowUtils.throwIf(Boolean.FALSE.equals(isDeleted), ErrorCode.SYSTEM_ERROR);
        return true;
    }

    /**
     * 批量重命名
     *
     * @param pictureList 图片列表
     * @param nameRule    命名规则 如：图片{序号}
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setPicName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "名称解析错误");
        }
    }

    /**
     * 校验ID列表
     *
     * @param idList ID列表
     */
    private void validIdList(List<Long> idList) {
        if (CollUtil.isEmpty(idList)) {
            return;
        }
        for (Long id : idList) {
            if (id != null && id <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        }
    }
}




