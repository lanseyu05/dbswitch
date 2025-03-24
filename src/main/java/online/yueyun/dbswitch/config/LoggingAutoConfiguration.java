package online.yueyun.dbswitch.config;

import lombok.extern.slf4j.Slf4j;
import online.yueyun.dbswitch.util.LogUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 日志自动配置类
 * 负责初始化框架日志，解决与应用系统日志冲突问题
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "dbswitch.enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAutoConfiguration {

    /**
     * 在框架初始化时执行日志设置
     */
    @PostConstruct
    public void init() {
        try {
            LogUtils.initializeFrameworkLogger();
            
            if (log.isDebugEnabled()) {
                log.debug("DBSwitch框架日志初始化完成");
                // 仅在调试模式下打印日志详情
                LogUtils.printLoggerInfo();
            }
        } catch (Exception e) {
            // 使用System.err记录日志初始化问题，避免循环依赖
            System.err.println("初始化DBSwitch日志框架失败: " + e.getMessage());
        }
    }
} 