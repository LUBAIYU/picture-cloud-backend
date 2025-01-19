package com.by.cloud.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.api.imagesearch.ImageSearchApiFacade;
import com.by.cloud.api.imagesearch.model.ImageSearchResult;
import com.by.cloud.common.BaseContext;
import com.by.cloud.common.CosManager;
import com.by.cloud.common.PageResult;
import com.by.cloud.common.upload.BasePictureUploadTemplate;
import com.by.cloud.common.upload.FilePictureUpload;
import com.by.cloud.common.upload.UrlPictureUpload;
import com.by.cloud.constants.PictureConstant;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
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
    private final PictureCategoryTagService pictureCategoryTagService;
    private final CosManager cosManager;
    private final SpaceService spaceService;
    private final TransactionTemplate transactionTemplate;

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
    public PictureVo uploadPicture(Object inputSource, PictureUploadDto dto) {
        // 获取登录用户ID
        UserVo loginUser = userService.getLoginUser();
        Long loginUserId = loginUser.getUserId();

        // 校验空间是否存在
        Long spaceId = null;
        if (dto != null && dto.getSpaceId() != null) {
            spaceId = dto.getSpaceId();
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 判断用户是否有权限上传
            if (!loginUserId.equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限对该空间上传图片");
            }
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
            // 仅本人或管理员可编辑
            if (!picture.getUserId().equals(loginUserId) && !userService.isAdmin(loginUserId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
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
    public PictureVo getPictureVo(Long picId) {
        // 判断图片是否存在
        Picture picture = this.getById(picId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验空间权限
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            checkPictureAuth(picture);
        }
        // 获取封装类
        PictureVo pictureVo = PictureVo.objToVo(picture);
        // 设置图片的创建用户信息
        Long userId = picture.getUserId();
        User user = userService.getById(userId);
        UserVo userVo = BeanUtil.copyProperties(user, UserVo.class);
        pictureVo.setUserVo(userVo);
        return pictureVo;
    }

    @Override
    public PageResult<Picture> queryPictureByPage(PicturePageDto pageDto) {
        // 获取参数
        int current = pageDto.getCurrent();
        int pageSize = pageDto.getPageSize();
        String picName = pageDto.getPicName();
        String introduction = pageDto.getIntroduction();
        String category = pageDto.getCategory();
        List<String> tagList = pageDto.getTagList();
        String picFormat = pageDto.getPicFormat();
        String searchText = pageDto.getSearchText();
        Integer reviewStatus = pageDto.getReviewStatus();
        String reviewMessage = pageDto.getReviewMessage();
        Long spaceId = pageDto.getSpaceId();
        boolean nullSpaceId = pageDto.isNullSpaceId();
        LocalDateTime startEditTime = pageDto.getStartEditTime();
        LocalDateTime endEditTime = pageDto.getEndEditTime();

        // 校验参数
        ThrowUtils.throwIf(current <= 0 || pageSize <= 0, ErrorCode.PARAMS_ERROR);

        // 构建分页条件
        IPage<Picture> page = new Page<>(current, pageSize);
        // 构建查询条件
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(picName), Picture::getPicName, picName);
        queryWrapper.like(StrUtil.isNotBlank(introduction), Picture::getIntroduction, introduction);
        queryWrapper.eq(StrUtil.isNotBlank(category), Picture::getCategory, category);
        queryWrapper.eq(reviewStatus != null, Picture::getReviewStatus, reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), Picture::getReviewMessage, reviewMessage);
        queryWrapper.eq(spaceId != null, Picture::getSpaceId, spaceId);
        queryWrapper.isNull(nullSpaceId, Picture::getSpaceId);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), Picture::getPicFormat, picFormat);
        // >= startEditTime
        queryWrapper.ge(ObjectUtil.isNotEmpty(startEditTime), Picture::getEditTime, startEditTime);
        // < endEditTime
        queryWrapper.lt(ObjectUtil.isNotEmpty(endEditTime), Picture::getEditTime, endEditTime);
        // order by createTime desc
        queryWrapper.orderByDesc(Picture::getCreateTime);

        // 标签查询
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like(Picture::getTags, "\"" + tag + "\"");
            }
        }
        if (StrUtil.isNotBlank(searchText)) {
            // 搜索关键词同时匹配名称和简介
            queryWrapper.and(qw -> qw.like(Picture::getPicName, searchText)
                    .or()
                    .like(Picture::getIntroduction, searchText)
            );
        }

        // 查询
        this.page(page, queryWrapper);

        // 返回
        return PageResult.of(page.getTotal(), page.getRecords());
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

        // 关联查询用户信息
        // 获取去重的用户ID列表
        Set<Long> userIdSet = records.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 查询用户ID列表对应的用户信息
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getUserId));

        // 设置用户信息
        pictureVoList.forEach(pictureVo -> {
            Long userId = pictureVo.getUserId();
            if (userIdUserListMap.containsKey(userId)) {
                User user = userIdUserListMap.get(userId).get(0);
                pictureVo.setUserVo(BeanUtil.copyProperties(user, UserVo.class));
            }
        });

        // 封装返回
        return PageResult.of(pageResult.getTotal(), pictureVoList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePicture(PictureUpdateDto updateDto) {
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

        // 先删除图片原有的绑定关系
        pictureCategoryTagService.lambdaUpdate()
                .eq(PictureCategoryTag::getPictureId, picId)
                .remove();

        // 如果只传递了标签，但是没有传分类，则报错，此业务默认标签属于分类下的一个层级
        if (categoryId == null && CollUtil.isNotEmpty(tagIdList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "需先指定分类，再选标签");
        }

        // 保存新的绑定关系
        if (categoryId != null) {
            List<PictureCategoryTag> pictureCategoryTagList = new ArrayList<>();
            if (CollUtil.isNotEmpty(tagIdList)) {
                for (Long tagId : tagIdList) {
                    PictureCategoryTag pictureCategoryTag = new PictureCategoryTag();
                    pictureCategoryTag.setPictureId(picId);
                    pictureCategoryTag.setCategoryId(categoryId);
                    pictureCategoryTag.setTagId(tagId);
                    pictureCategoryTagList.add(pictureCategoryTag);
                }
            } else {
                PictureCategoryTag pictureCategoryTag = new PictureCategoryTag();
                pictureCategoryTag.setPictureId(picId);
                pictureCategoryTag.setCategoryId(categoryId);
                pictureCategoryTagList.add(pictureCategoryTag);
            }
            // 插入数据库
            boolean saved = pictureCategoryTagService.saveBatch(pictureCategoryTagList);
            ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR);
        }

        // 复制参数
        Picture picture = new Picture();
        BeanUtil.copyProperties(updateDto, picture);

        // 如果有传分类，则冗余分类名称
        if (categoryId != null) {
            Category category = categoryService.lambdaQuery()
                    .eq(Category::getId, categoryId)
                    .one();
            ThrowUtils.throwIf(category == null, ErrorCode.NOT_FOUND_ERROR);
            picture.setCategory(category.getName());
        }

        // 如果有传标签，则冗余标签列表
        if (CollUtil.isNotEmpty(tagIdList)) {
            List<Tag> tagList = tagService.lambdaQuery()
                    .in(Tag::getId, tagIdList)
                    .list();
            if (CollUtil.isEmpty(tagList) || tagList.size() != tagIdList.size()) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
            }
            List<String> tagNameList = tagList.stream().map(Tag::getName).toList();
            picture.setTags(JSONUtil.toJsonStr(tagNameList));
        }

        // 校验
        this.validPicture(picture);

        // 判断ID是否存在
        Picture dbPicture = this.getById(picId);
        ThrowUtils.throwIf(dbPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 判断是否有权限，只有创建用户本人或管理员才能更改
        this.checkPictureAuth(dbPicture);

        // 填充审核参数
        this.fillReviewParams(picture, userService.getLoginUser());

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
    public void pictureReview(PictureReviewDto reviewDto) {
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
        Picture updatePicture = new Picture();
        updatePicture.setPicId(id);
        updatePicture.setReviewStatus(reviewStatus);
        updatePicture.setReviewMessage(reviewMessage);
        Long loginUserId = BaseContext.getLoginUserId();
        updatePicture.setReviewerId(loginUserId);
        updatePicture.setReviewTime(LocalDateTime.now());
        boolean updated = this.updateById(updatePicture);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture, UserVo loginUser) {
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
    public int uploadPictureByBatch(PictureBatchDto batchDto) {
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
                PictureVo pictureVo = this.uploadPicture(fileUrl, uploadDto);
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

        // 只有创建用户或者管理员可以删除图片
        this.checkPictureAuth(picture);

        // 删除图片与分类标签的绑定关系
        pictureCategoryTagService.lambdaUpdate()
                .eq(PictureCategoryTag::getPictureId, picId)
                .remove();

        // 删除远程COS图片
        this.clearPictureFile(picture);

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
    public void checkPictureAuth(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        Long loginUserId = BaseContext.getLoginUserId();
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
        return ImageSearchApiFacade.searchImage(picture.getPicUrl());
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




