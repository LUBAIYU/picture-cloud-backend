package com.by.cloud.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import com.by.cloud.mapper.PictureMapper;
import com.by.cloud.mapper.SpaceMapper;
import com.by.cloud.model.dto.space.analyze.*;
import com.by.cloud.model.entity.Picture;
import com.by.cloud.model.entity.Space;
import com.by.cloud.model.entity.User;
import com.by.cloud.model.vo.space.analyze.*;
import com.by.cloud.service.PictureService;
import com.by.cloud.service.SpaceAnalyzeService;
import com.by.cloud.service.SpaceService;
import com.by.cloud.service.UserService;
import com.by.cloud.utils.ThrowUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public void fillAnalyzeQueryWrapper(SpaceAnalyzeDto spaceAnalyzeDto, QueryWrapper<Picture> queryWrapper) {
        // 查询全空间
        boolean queryAll = spaceAnalyzeDto.isQueryAll();
        if (queryAll) {
            return;
        }
        // 查询公共图库
        boolean queryPublic = spaceAnalyzeDto.isQueryPublic();
        if (queryPublic) {
            queryWrapper.isNull("space_id");
            return;
        }
        // 查询特定空间
        Long spaceId = spaceAnalyzeDto.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("space_id", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定分析范围");
    }

    @Override
    public SpaceUsageAnalyzeVo getSpaceUsageAnalyze(SpaceUsageAnalyzeDto spaceUsageAnalyzeDto, HttpServletRequest request) {
        // 获取当前登录用户ID
        Long loginUserId = userService.getLoginUserId(request);

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
    public List<SpaceCategoryAnalyzeVo> getCategoryAnalyze(SpaceCategoryAnalyzeDto spaceCategoryAnalyzeDto, HttpServletRequest request) {
        // 获取当前登录用户ID
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getUserId();
        // 权限校验
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeDto, loginUserId);
        // 分组查询
        return pictureMapper.getCategoryAnalyze(spaceCategoryAnalyzeDto);
    }

    @Override
    public List<SpaceTagAnalyzeVo> getTagAnalyze(SpaceTagAnalyzeDto spaceTagAnalyzeDto, HttpServletRequest request) {
        // 获取当前登录用户ID
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getUserId();
        // 权限校验
        checkSpaceAnalyzeAuth(spaceTagAnalyzeDto, loginUserId);
        // 连表查询
        return pictureMapper.getTagAnalyze(spaceTagAnalyzeDto);
    }

    @Override
    public List<SpaceSizeAnalyzeVo> getSizeAnalyze(SpaceSizeAnalyzeDto spaceSizeAnalyzeDto, HttpServletRequest request) {
        // 获取当前登录用户ID
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getUserId();
        // 权限校验
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeDto, loginUserId);

        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Picture::getPicSize);
        // 填充查询参数
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeDto, queryWrapper);

        // 获取所有图片的大小列表
        List<Long> picSizeList = pictureMapper.selectObjs(queryWrapper).stream()
                .map(obj -> obj instanceof Long ? (Long) obj : 0L)
                .toList();

        // 定义分段范围，使用有序集合存储
        Map<String, Long> sizeRangeMap = getSizeRangeMap(picSizeList);

        // 封装返回
        return sizeRangeMap.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeVo(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public List<SpaceUserAnalyzeVo> getUserAnalyze(SpaceUserAnalyzeDto spaceUserAnalyzeDto, HttpServletRequest request) {
        // 获取当前登录用户ID
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getUserId();
        // 权限校验
        checkSpaceAnalyzeAuth(spaceUserAnalyzeDto, loginUserId);

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 填充查询参数
        fillAnalyzeQueryWrapper(spaceUserAnalyzeDto, queryWrapper);
        Long userId = spaceUserAnalyzeDto.getUserId();
        queryWrapper.eq(ObjectUtil.isNotNull(userId), "user_id", userId);

        // 根据时间纬度查询
        String timeDimension = spaceUserAnalyzeDto.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(create_time,'%Y-%m-%d') as period", "COUNT(*) as count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(create_time) as period", "COUNT(*) as count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(create_time,'%Y-%m') as period", "COUNT(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间纬度");
        }

        // 分组和排序
        queryWrapper.groupBy("period").orderByAsc("period");

        // 查询返回
        return pictureMapper.selectMaps(queryWrapper)
                .stream()
                .map(map -> {
                    String period = map.get("period").toString();
                    Long count = Long.parseLong(map.get("count").toString());
                    return new SpaceUserAnalyzeVo(period, count);
                }).toList();
    }

    @Override
    public List<Space> getSpaceAnalyze(SpaceRankAnalyzeDto spaceRankAnalyzeDto, HttpServletRequest request) {
        // 获取当前登录用户ID
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getUserId();
        // 仅管理员可分析
        ThrowUtils.throwIf(!userService.isAdmin(loginUserId), ErrorCode.NO_AUTH_ERROR);

        // 构造查询条件
        LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Space::getId, Space::getSpaceName, Space::getUserId, Space::getTotalSize)
                .orderByDesc(Space::getTotalSize)
                .last("limit " + spaceRankAnalyzeDto.getTopN());

        // 查询结果
        return spaceService.list(queryWrapper);
    }

    /**
     * 获取图片大小分段统计
     *
     * @param picSizeList 图片大小列表
     * @return 有序集合
     */
    private static Map<String, Long> getSizeRangeMap(List<Long> picSizeList) {
        long firstSizeRangeCount = 0;
        long secondSizeRangeCount = 0;
        long thirdSizeRangeCount = 0;
        long fourthSizeRangeCount = 0;
        for (Long picSize : picSizeList) {
            // <100KB
            if (picSize < 100 * 1024) {
                firstSizeRangeCount++;
                // >=100KB && <500KB
            } else if (picSize < 500 * 1024) {
                secondSizeRangeCount++;
                // >=500KB && <1MB
            } else if (picSize < 1024 * 1024) {
                thirdSizeRangeCount++;
                // >1MB
            } else {
                fourthSizeRangeCount++;
            }
        }
        Map<String, Long> sizeRangeMap = new LinkedHashMap<>();
        sizeRangeMap.put("<100KB", firstSizeRangeCount);
        sizeRangeMap.put("100KB-500KB", secondSizeRangeCount);
        sizeRangeMap.put("500KB-1MB", thirdSizeRangeCount);
        sizeRangeMap.put(">1MB", fourthSizeRangeCount);
        return sizeRangeMap;
    }
}
