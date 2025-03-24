package online.yueyun.dbswitch.config;

import lombok.extern.slf4j.Slf4j;
import online.yueyun.dbswitch.datasource.DynamicDataSource;
import online.yueyun.dbswitch.datasource.DynamicDataSource.DataSourceType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态数据源配置
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "dbswitch.enabled", havingValue = "true", matchIfMissing = true)
public class DynamicDataSourceConfig {

    /**
     * 主数据源
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        log.info("初始化主数据源");
        return DataSourceBuilder.create().build();
    }

    /**
     * 从数据源
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.slave")
    public DataSource slaveDataSource() {
        log.info("初始化从数据源");
        return DataSourceBuilder.create().build();
    }

    /**
     * 动态数据源
     */
    @Bean
    @Primary
    public DataSource dynamicDataSource(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            @Qualifier("slaveDataSource") DataSource slaveDataSource) {
        log.info("初始化动态数据源");
        
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        
        // 配置数据源映射关系
        Map<Object, Object> dataSourceMap = new HashMap<>(2);
        dataSourceMap.put(DataSourceType.MASTER, masterDataSource);
        dataSourceMap.put(DataSourceType.SLAVE, slaveDataSource);
        
        // 设置默认数据源和所有数据源
        dynamicDataSource.setDefaultTargetDataSource(masterDataSource);
        dynamicDataSource.setTargetDataSources(dataSourceMap);
        
        return dynamicDataSource;
    }
} 