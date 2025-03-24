package online.yueyun.dbswitch.aop;

import lombok.extern.slf4j.Slf4j;
import online.yueyun.dbswitch.datasource.DynamicDataSource;
import online.yueyun.dbswitch.datasource.DynamicDataSource.DataSourceType;
import online.yueyun.dbswitch.datasource.DynamicDataSourceSelector;
import online.yueyun.dbswitch.enums.MQType;
import online.yueyun.dbswitch.enums.OperationType;
import online.yueyun.dbswitch.message.DBOperationMessage;
import online.yueyun.dbswitch.mq.producer.MQProducer;
import online.yueyun.dbswitch.util.SqlParserUtil;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Properties;
import java.util.UUID;

/**
 * 数据库双写切换拦截器
 */
@Slf4j
@Component
@Intercepts({
    @Signature(type = org.apache.ibatis.executor.Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
    @Signature(type = org.apache.ibatis.executor.Executor.class, method = "query", args = {MappedStatement.class, Object.class, org.apache.ibatis.session.RowBounds.class, org.apache.ibatis.session.ResultHandler.class})
})
public class DBSwitchInterceptor implements Interceptor {

    @Autowired
    private DynamicDataSourceSelector dataSourceSelector;

    @Autowired(required = false)
    private MQType mqType;

    @Autowired(required = false)
    @Qualifier("rocketMQProducer")
    private MQProducer rocketMQProducer;

    @Autowired(required = false)
    @Qualifier("rabbitMQProducer")
    private MQProducer rabbitMQProducer;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];
        
        // 获取Mapper类名和方法名
        String mapperClassName = ms.getId().substring(0, ms.getId().lastIndexOf("."));
        String methodName = ms.getId().substring(ms.getId().lastIndexOf(".") + 1);
        
        // 解析操作类型
        OperationType operationType = parseOperationType(ms);
        
        // 判断是否需要双写
        boolean needDualWrite = dataSourceSelector.needDualWrite(operationType);
        
        // 执行单库操作（查询或非双写模式下的写操作）
        if (!needDualWrite) {
            return executeSingleOperation(invocation, operationType, mapperClassName, methodName);
        }
        
        // 执行双写操作
        return executeDualWriteOperation(invocation, operationType, parameter, mapperClassName, methodName);
    }

    /**
     * 执行单库操作
     */
    private Object executeSingleOperation(Invocation invocation, OperationType operationType, 
                                         String mapperClassName, String methodName) throws Throwable {
        // 根据操作类型和写入模式，切换到对应的数据源
        boolean useMaster = dataSourceSelector.useMasterDataSource(operationType);
        DynamicDataSource.setDataSource(useMaster ? DataSourceType.MASTER : DataSourceType.SLAVE);
        
        try {
            log.debug("单库操作，类型: {}, 数据源: {}, Mapper: {}, 方法: {}", 
                     operationType, useMaster ? "主库" : "从库", mapperClassName, methodName);
            return invocation.proceed();
        } finally {
            DynamicDataSource.clearDataSource();
        }
    }

    /**
     * 执行双写操作
     */
    private Object executeDualWriteOperation(Invocation invocation, OperationType operationType, 
                                            Object parameter, String mapperClassName, String methodName) throws Throwable {
        // 决定先执行哪个库
        boolean useMaster = dataSourceSelector.useMasterDataSource(operationType);
        
        // 切换到第一个数据源
        DynamicDataSource.setDataSource(useMaster ? DataSourceType.MASTER : DataSourceType.SLAVE);
        
        // 执行第一个库的操作
        Object result = null;
        try {
            log.debug("双写操作，第一个数据源: {}, Mapper: {}, 方法: {}", 
                     useMaster ? "主库" : "从库", mapperClassName, methodName);
            result = invocation.proceed();
        } finally {
            DynamicDataSource.clearDataSource();
        }

        // 检查是否配置了MQ，如果没有则直接返回结果
        if (mqType == null) {
            log.warn("未配置消息队列类型或未找到消息队列依赖，跳过双写操作");
            return result;
        }

        // 通过消息队列异步执行第二个库的操作
        try {
            // 获取第二个数据源信息
            boolean secondMaster = dataSourceSelector.secondWriteInMaster();
            
            log.debug("双写操作，发送消息队列，第二个数据源: {}, Mapper: {}, 方法: {}", 
                     secondMaster ? "主库" : "从库", mapperClassName, methodName);
            
            // 构建消息，添加第二个数据源信息
            DBOperationMessage message = buildMessage(mapperClassName, methodName, parameter, operationType, secondMaster);
            
            // 发送消息
            sendMessage(message);
        } catch (Exception e) {
            log.error("发送双写消息失败: ", e);
        }
        
        return result;
    }

    /**
     * 构建数据库操作消息
     */
    private DBOperationMessage buildMessage(String mapperClassName, String methodName, Object parameter, 
                                          OperationType operationType, boolean useSecondMaster) {
        return DBOperationMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .mapperClassName(mapperClassName)
                .methodName(methodName)
                .parameterTypes(getParameterTypes(parameter))
                .args(new Object[]{parameter})
                .operationType(operationType)
                .createTime(new Date())
                .retryCount(0)
                .success(false)
                .useSecondMaster(useSecondMaster)  // 添加第二个数据源信息
                .build();
    }

    /**
     * 发送消息到消息队列
     */
    private void sendMessage(DBOperationMessage message) {
        if (mqType == null) {
            log.error("未配置消息队列类型，无法发送消息");
            return;
        }

        MQProducer producer = null;
        if (mqType == MQType.ROCKET_MQ && rocketMQProducer != null) {
            producer = rocketMQProducer;
        } else if (mqType == MQType.RABBIT_MQ && rabbitMQProducer != null) {
            producer = rabbitMQProducer;
        }

        if (producer == null) {
            log.error("无法找到对应的消息队列生产者: {}", mqType);
            return;
        }

        producer.sendMessage(message);
    }

    /**
     * 获取参数类型
     */
    private String[] getParameterTypes(Object parameter) {
        if (parameter == null) {
            return new String[0];
        }
        return new String[]{parameter.getClass().getName()};
    }

    /**
     * 解析操作类型
     */
    private OperationType parseOperationType(MappedStatement ms) {
        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        String methodName = ms.getId().substring(ms.getId().lastIndexOf(".") + 1);
        
        switch (sqlCommandType) {
            case SELECT:
                return OperationType.SELECT;
            case INSERT:
                return OperationType.INSERT;
            case UPDATE:
                return OperationType.UPDATE;
            case DELETE:
                return OperationType.DELETE;
            default:
                return SqlParserUtil.parseOperationTypeByMethodName(methodName);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Interceptor.super.plugin(target);
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }
} 