package com.by.cloud.common.sharding;

import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import com.by.cloud.enums.SpaceLevelEnum;
import com.by.cloud.enums.SpaceTypeEnum;
import com.by.cloud.model.entity.Space;
import com.by.cloud.service.SpaceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.infra.metadata.database.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分表管理器
 *
 * @author lzh
 */
//@Component
@Slf4j
public class DynamicShardingManager {

    @Resource
    private DataSource dataSource;

    @Resource
    private SpaceService spaceService;

    /**
     * 逻辑表名
     */
    public static final String LOGIC_TABLE_NAME = "picture";

    /**
     * 数据库名
     */
    public static final String DATABASE_NAME = "logic_db";

    @PostConstruct
    public void initialize() {
        log.info("初始化动态分表配置...");
        updateShardingTableNodes();
    }

    /**
     * 动态创建分表
     * 仅为旗舰版团队空间创建分表
     *
     * @param space 空间
     */
    public void createSpacePictureTable(Space space) {
        if (space.getSpaceType() == SpaceTypeEnum.TEAM.getValue() && space.getSpaceLevel() == SpaceLevelEnum.FLAGSHIP.getValue()) {
            Long spaceId = space.getId();
            String tableName = LOGIC_TABLE_NAME + "_" + spaceId;
            // 创建新表
            String createTableSql = "CREATE TABLE " + tableName + " LIKE picture";
            try {
                SqlRunner.db().update(createTableSql);
                // 更新分表
                updateShardingTableNodes();
            } catch (Exception e) {
                log.error("创建图片空间分表失败，空间 id = {}", space.getId());
            }
        }
    }


    /**
     * 获取所有分表名，包括初始表 picture 和分表 picture_{space_id}
     *
     * @return 所有分表名
     */
    private Set<String> fetchAllPictureTableNames() {
        // 只对旗舰版空间进行分表
        // 取出所有旗舰版空间的ID
        Set<Long> spaceIdSet = spaceService.lambdaQuery()
                .eq(Space::getSpaceLevel, SpaceLevelEnum.FLAGSHIP.getValue())
                .list()
                .stream()
                .map(Space::getId)
                .collect(Collectors.toSet());
        // 获取所有旗舰版空间的分表名
        Set<String> tableNames = spaceIdSet.stream()
                .map(spaceId -> LOGIC_TABLE_NAME + "_" + spaceId)
                .collect(Collectors.toSet());
        tableNames.add(LOGIC_TABLE_NAME);
        return tableNames;
    }

    /**
     * 更新 ShardingSphere 的 actual-data-nodes 动态表名配置
     */
    private void updateShardingTableNodes() {
        Set<String> tableNames = fetchAllPictureTableNames();
        // 重新配置 actual-data-nodes
        String newActualDataNodes = tableNames.stream()
                .map(tableName -> "picture_cloud." + tableName)
                .collect(Collectors.joining(","));
        log.info("动态分表 actual-data-nodes 配置：{}", newActualDataNodes);

        ContextManager contextManager = getContextManager();
        ShardingSphereRuleMetaData ruleMetaData = contextManager.getMetaDataContexts()
                .getMetaData()
                .getDatabases()
                .get(DATABASE_NAME)
                .getRuleMetaData();

        Optional<ShardingRule> shardingRule = ruleMetaData.findSingleRule(ShardingRule.class);
        if (shardingRule.isPresent()) {
            ShardingRuleConfiguration ruleConfig = (ShardingRuleConfiguration) shardingRule.get().getConfiguration();
            List<ShardingTableRuleConfiguration> updateRules = ruleConfig.getTables()
                    .stream()
                    .map(oldTableRule -> {
                        if (LOGIC_TABLE_NAME.equals(oldTableRule.getLogicTable())) {
                            ShardingTableRuleConfiguration newTableRuleConfig = new ShardingTableRuleConfiguration(LOGIC_TABLE_NAME, newActualDataNodes);
                            newTableRuleConfig.setDatabaseShardingStrategy(oldTableRule.getDatabaseShardingStrategy());
                            newTableRuleConfig.setTableShardingStrategy(oldTableRule.getTableShardingStrategy());
                            newTableRuleConfig.setKeyGenerateStrategy(oldTableRule.getKeyGenerateStrategy());
                            newTableRuleConfig.setAuditStrategy(oldTableRule.getAuditStrategy());
                            return newTableRuleConfig;
                        }
                        return oldTableRule;
                    }).toList();
            ruleConfig.setTables(updateRules);
            contextManager.alterRuleConfiguration(DATABASE_NAME, Collections.singleton(ruleConfig));
            contextManager.reloadDatabase(DATABASE_NAME);
            log.info("动态分表规则更新成功！");
        } else {
            log.error("未找到 ShardingSphere 的分片规则配置，动态分表更新失败。");
        }
    }

    /**
     * 获取 ShardingSphere 上下文管理器
     *
     * @return ContextManager
     */
    private ContextManager getContextManager() {
        try (ShardingSphereConnection connection = dataSource.getConnection().unwrap(ShardingSphereConnection.class)) {
            return connection.getContextManager();
        } catch (SQLException e) {
            throw new RuntimeException("获取 ShardingSphere ContextManage 失败", e);
        }
    }
}
