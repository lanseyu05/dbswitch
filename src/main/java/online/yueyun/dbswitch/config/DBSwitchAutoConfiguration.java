package online.yueyun.dbswitch.config;

import lombok.extern.slf4j.Slf4j;
import online.yueyun.dbswitch.aop.DBSwitchInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 数据库双写自动配置
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "dbswitch.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(MybatisAutoConfiguration.class)
@Import({DynamicDataSourceConfig.class, MQConfig.class, LoggingAutoConfiguration.class})
@ComponentScan(basePackages = "online.yueyun.dbswitch")
public class DBSwitchAutoConfiguration {

    /**
     * 注册Mybatis拦截器
     */
    @Bean
    @ConditionalOnBean(DBSwitchInterceptor.class)
    public Interceptor dbSwitchInterceptor(DBSwitchInterceptor dbSwitchInterceptor) {
        log.info("注册DBSwitch拦截器");
        return dbSwitchInterceptor;
    }
} 