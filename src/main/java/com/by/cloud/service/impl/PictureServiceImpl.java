package com.by.cloud.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.common.BaseContext;
import com.by.cloud.common.PageResult;
import com.by.cloud.common.upload.BasePictureUploadTemplate;
import com.by.cloud.common.upload.FilePictureUpload;
import com.by.cloud.common.upload.UrlPictureUpload;
import com.by.cloud.constants.PictureConstant;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.enums.PictureReviewStatusEnum;
import com.by.cloud.enums.UserRoleEnum;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.mapper.PictureMapper;
import com.by.cloud.model.dto.file.UploadPictureResult;
import com.by.cloud.model.dto.picture.*;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.entity.User;
import com.by.cloud.model.vo.PictureVo;
import com.by.cloud.model.vo.UserVo;
import com.by.cloud.service.PictureService;
import com.by.cloud.service.UserService;
import com.by.cloud.utils.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author lzh
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public PictureVo uploadPicture(Object inputSource, PictureUploadDto dto) {
        // 获取登录用户ID
        UserVo loginUser = userService.getLoginUser();
        Long loginUserId = loginUser.getUserId();

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
        }

        // 上传图片获取图片信息
        String uploadPathPrefix = String.format("public/%s", loginUserId);
        // 根据输入源类型调用不同的上传逻辑
        BasePictureUploadTemplate basePictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            basePictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = basePictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 构造入库信息
        Picture picture = new Picture();
        BeanUtil.copyProperties(uploadPictureResult, picture);
        picture.setUserId(loginUserId);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        if (picId != null) {
            picture.setPicId(picId);
            picture.setEditTime(LocalDateTime.now());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片保存失败");
        return PictureVo.objToVo(picture);
    }

    @Override
    public PictureVo getPictureVo(Long picId) {
        // 判断图片是否存在
        Picture picture = this.getById(picId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
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
    public boolean deleteById(Long picId) {
        // 查询图片
        Picture picture = this.getById(picId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        // 获取登录用户
        UserVo loginUser = userService.getLoginUser();
        Integer loginUserRole = loginUser.getUserRole();
        Long loginUserId = loginUser.getUserId();

        // 只有创建用户或者管理员可以删除图片
        if (!picture.getUserId().equals(loginUserId) && !UserRoleEnum.ADMIN.getValue().equals(loginUserRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 删除
        boolean result = this.removeById(picId);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);
        return true;
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
        String searchText = pageDto.getSearchText();
        Integer reviewStatus = pageDto.getReviewStatus();
        String reviewMessage = pageDto.getReviewMessage();

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
    public boolean updatePictureByAdmin(PictureUpdateDto updateDto) {
        // 复制参数
        Picture picture = new Picture();
        BeanUtil.copyProperties(updateDto, picture);
        picture.setTags(JSONUtil.toJsonStr(updateDto.getTagList()));
        // 校验
        this.validPicture(picture);
        // 判断ID是否存在
        Picture dbPicture = this.getById(updateDto.getPicId());
        ThrowUtils.throwIf(dbPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 填充审核参数
        UserVo loginUser = userService.getLoginUser();
        this.fillReviewParams(picture, loginUser);
        // 更新数据
        boolean updated = this.updateById(picture);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public void validPicture(Picture picture) {
        // 获取校验字段
        Long picId = picture.getPicId();
        String picName = picture.getPicName();
        String introduction = picture.getIntroduction();
        String picUrl = picture.getPicUrl();
        // 校验
        ThrowUtils.throwIf(picId == null, ErrorCode.PARAMS_ERROR, "图片ID不能为空");
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
    public void editPicture(PictureEditDto editDto) {
        // 复制信息
        Picture picture = new Picture();
        BeanUtil.copyProperties(editDto, picture);
        picture.setTags(JSONUtil.toJsonStr(editDto.getTagList()));
        // 校验图片
        this.validPicture(picture);
        // 判断图片是否存在
        Picture dbPicture = this.getById(editDto.getPicId());
        ThrowUtils.throwIf(dbPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 判断是否有权限，只有创建用户本人或管理员才能更改
        UserVo loginUser = userService.getLoginUser();
        Long userId = dbPicture.getUserId();
        Long loginUserId = loginUser.getUserId();
        if (!userId.equals(loginUserId) && !UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 填充审核信息
        this.fillReviewParams(picture, loginUser);
        // 更新图片信息
        boolean updated = this.updateById(picture);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR);
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
}




