package com.by.cloud.common.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * 分表算法类
 *
 * @author lzh
 */
public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {
    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<Long> preciseShardingValue) {
        // 获取 ShardingSphere 拦截到的 spaceId
        Long spaceId = preciseShardingValue.getValue();
        String logicTableName = preciseShardingValue.getLogicTableName();
        // 公共图库或者未指定空间ID
        if (spaceId == null) {
            return logicTableName;
        }
        // 根据 spaceId 动态生成表名
        String realTableName = String.format(logicTableName + "_%s", spaceId);
        // 如果可用的分表中包含指定的表，则直接返回分表，否则查询逻辑表，即全表
        if (collection.contains(realTableName)) {
            return realTableName;
        }
        return logicTableName;
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return List.of();
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties properties) {

    }
}
