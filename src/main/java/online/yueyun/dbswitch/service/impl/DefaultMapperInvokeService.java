package online.yueyun.dbswitch.service.impl;

import lombok.extern.slf4j.Slf4j;
import online.yueyun.dbswitch.datasource.DynamicDataSource;
import online.yueyun.dbswitch.datasource.DynamicDataSource.DataSourceType;
import online.yueyun.dbswitch.service.MapperInvokeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

/**
 * 默认的Mapper调用服务实现
 */
@Slf4j
@Service
public class DefaultMapperInvokeService implements MapperInvokeService {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public Object invoke(String mapperClassName, String methodName, String[] parameterTypeNames, Object[] args) throws Exception {
        // 默认调用方法，不指定数据源
        return invokeMethod(mapperClassName, methodName, parameterTypeNames, args);
    }
    
    @Override
    public Object invoke(String mapperClassName, String methodName, String[] parameterTypeNames, Object[] args, boolean useMaster) throws Exception {
        // 切换到指定的数据源
        DynamicDataSource.setDataSource(useMaster ? DataSourceType.MASTER : DataSourceType.SLAVE);
        try {
            log.debug("消费消息，使用数据源: {}, Mapper: {}, 方法: {}", 
                     useMaster ? "主库" : "从库", mapperClassName, methodName);
            return invokeMethod(mapperClassName, methodName, parameterTypeNames, args);
        } finally {
            // 清除数据源上下文
            DynamicDataSource.clearDataSource();
        }
    }
    
    /**
     * 实际执行Mapper方法的内部方法
     */
    private Object invokeMethod(String mapperClassName, String methodName, String[] parameterTypeNames, Object[] args) throws Exception {
        try {
            // 加载Mapper类
            Class<?> mapperClass = Class.forName(mapperClassName);
            
            // 获取Mapper Bean
            Object mapperBean = applicationContext.getBean(mapperClass);
            
            // 解析参数类型
            Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.length];
            for (int i = 0; i < parameterTypeNames.length; i++) {
                parameterTypes[i] = Class.forName(parameterTypeNames[i]);
            }
            
            // 获取方法
            Method method = mapperClass.getMethod(methodName, parameterTypes);
            
            // 调用方法
            return method.invoke(mapperBean, args);
        } catch (Exception e) {
            log.error("调用Mapper方法失败: mapperClass={}, method={}", mapperClassName, methodName, e);
            throw e;
        }
    }
} 