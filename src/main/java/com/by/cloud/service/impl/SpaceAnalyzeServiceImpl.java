package com.by.cloud.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.common.BaseContext;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.mapper.PictureMapper;
import com.by.cloud.mapper.SpaceMapper;
import com.by.cloud.model.dto.space.analyze.SpaceAnalyzeDto;
import com.by.cloud.model.dto.space.analyze.SpaceCategoryAnalyzeDto;
import com.by.cloud.model.dto.space.analyze.SpaceTagAnalyzeDto;
import com.by.cloud.model.dto.space.analyze.SpaceUsageAnalyzeDto;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.entity.Space;
import com.by.cloud.model.vo.space.analyze.SpaceCategoryAnalyzeVo;
import com.by.cloud.model.vo.space.analyze.SpaceTagAnalyzeVo;
import com.by.cloud.model.vo.space.analyze.SpaceUsageAnalyzeVo;
import com.by.cloud.service.PictureService;
import com.by.cloud.service.SpaceAnalyzeService;
import com.by.cloud.service.SpaceService;
import com.by.cloud.service.UserService;
import com.by.cloud.utils.ThrowUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * @author lzh
 */
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceAnalyzeService {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private PictureMapper pictureMapper;


    @Override
    public void checkSpaceAnalyzeAuth(SpaceAnalyzeDto spaceAnalyzeDto, Long loginUserId) {
        boolean queryPublic = spaceAnalyzeDto.isQueryPublic();
        boolean queryAll = spaceAnalyzeDto.isQueryAll();
        // 只能选择查询公共图库或全空间
        if (queryPublic && queryAll) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 查询公共图库或全空间，需要管理员权限
        if (queryPublic || queryAll) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUserId), ErrorCode.NO_AUTH_ERROR);
            return;
        }
        // 查询特定空间，需要创建空间者或者管理员权限
        Long spaceId = spaceAnalyzeDto.getSpaceId();
        if (spaceId == null || spaceId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Space space = Optional.ofNullable(this.getById(spaceId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        spaceService.checkSpaceAuth(space, loginUserId);
    }

    @Override
    public void fillAnalyzeQueryWrapper(SpaceAnalyzeDto spaceAnalyzeDto, LambdaQueryWrapper<Picture> queryWrapper) {
        // 查询全空间
        boolean queryAll = spaceAnalyzeDto.isQueryAll();
        if (queryAll) {
            return;
        }
        // 查询公共图库
        boolean queryPublic = spaceAnalyzeDto.isQueryPublic();
        if (queryPublic) {
            queryWrapper.isNull(Picture::getSpaceId);
            return;
        }
        // 查询特定空间
        Long spaceId = spaceAnalyzeDto.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq(Picture::getSpaceId, spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定分析范围");
    }

    @Override
    public SpaceUsageAnalyzeVo getSpaceUsageAnalyze(SpaceUsageAnalyzeDto spaceUsageAnalyzeDto) {
        // 获取当前登录用户ID
        Long loginUserId = BaseContext.getLoginUserId();

        // 统一校验权限
        checkSpaceAnalyzeAuth(spaceUsageAnalyzeDto, loginUserId);

        // 创建返回结果
        // 公共图库无使用上限，无比例
        SpaceUsageAnalyzeVo spaceUsageAnalyzeVo = new SpaceUsageAnalyzeVo();
        spaceUsageAnalyzeVo.setMaxCount(null);
        spaceUsageAnalyzeVo.setMaxSize(null);
        spaceUsageAnalyzeVo.setCountUsageRatio(null);
        spaceUsageAnalyzeVo.setSizeUsageRatio(null);

        // 查询公共图库或全空间
        if (spaceUsageAnalyzeDto.isQueryAll() || spaceUsageAnalyzeDto.isQueryPublic()) {
            // 查询条件
            LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(Picture::getPicSize);
            // 填充查询参数
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeDto, queryWrapper);
            // 查询
            List<Object> objectList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            // 获取使用数和使用总量
            long usedCount = objectList.size();
            long usedSize = objectList.stream().mapToLong(obj -> obj instanceof Long ? (Long) obj : 0L).sum();
            spaceUsageAnalyzeVo.setUsedCount(usedCount);
            spaceUsageAnalyzeVo.setUsedSize(usedSize);
        } else {
            // 查询特定空间
            Space space = spaceService.getById(spaceUsageAnalyzeDto.getSpaceId());
            spaceUsageAnalyzeVo.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeVo.setMaxCount(space.getMaxCount());
            spaceUsageAnalyzeVo.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeVo.setMaxSize(space.getMaxSize());
            // 计算比例
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            spaceUsageAnalyzeVo.setCountUsageRatio(countUsageRatio);
            spaceUsageAnalyzeVo.setSizeUsageRatio(sizeUsageRatio);
        }
        // 返回结果
        return spaceUsageAnalyzeVo;
    }

    @Override
    public List<SpaceCategoryAnalyzeVo> getCategoryAnalyze(SpaceCategoryAnalyzeDto spaceCategoryAnalyzeDto) {
        // 获取当前登录用户ID
        Long loginUserId = BaseContext.getLoginUserId();
        // 权限校验
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeDto, loginUserId);
        // 分组查询
        return pictureMapper.getCategoryAnalyze(spaceCategoryAnalyzeDto);
    }

    @Override
    public List<SpaceTagAnalyzeVo> getTagAnalyze(SpaceTagAnalyzeDto spaceTagAnalyzeDto) {
        // 获取当前登录用户ID
        Long loginUserId = BaseContext.getLoginUserId();
        // 权限校验
        checkSpaceAnalyzeAuth(spaceTagAnalyzeDto, loginUserId);
        // 连表查询
        return pictureMapper.getTagAnalyze(spaceTagAnalyzeDto);
    }
}
