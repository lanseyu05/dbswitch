package online.yueyun.dbswitch.mq.consumer;

import lombok.extern.slf4j.Slf4j;
import online.yueyun.dbswitch.message.DBOperationMessage;
import online.yueyun.dbswitch.mq.producer.MQProducer;
import online.yueyun.dbswitch.service.IdempotentService;
import online.yueyun.dbswitch.service.MapperInvokeService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MQ消费者抽象基类
 */
@Slf4j
public abstract class AbstractMQConsumer implements MQConsumer {

    @Autowired
    protected IdempotentService idempotentService;

    @Autowired
    protected MapperInvokeService mapperInvokeService;

    /**
     * 处理消息的共用方法
     * 
     * @param message 接收到的消息
     */
    protected void processMessage(DBOperationMessage message) {
        try {
            log.info("接收到MQ消息: {}", message);
            boolean success = consumeMessage(message);
            if (!success) {
                handleRetry(message);
            }
        } catch (Exception e) {
            log.error("处理MQ消息失败: {}", message, e);
            handleRetry(message);
        }
    }

    @Override
    public boolean consumeMessage(DBOperationMessage message) {
        // 幂等性检查
        if (idempotentService.isProcessed(message.getMessageId())) {
            log.info("消息已处理，忽略: {}", message);
            return true;
        }

        try {
            // 执行数据库操作
            Object result = mapperInvokeService.invoke(
                    message.getMapperClassName(),
                    message.getMethodName(),
                    message.getParameterTypes(),
                    message.getArgs(),
                    message.getUseSecondMaster()  // 使用消息中指定的数据源
            );

            // 标记消息已处理
            idempotentService.markAsProcessed(message.getMessageId());
            
            log.info("数据库操作执行成功: {}, 结果: {}", message, result);
            message.setSuccess(true);
            return true;
        } catch (Exception e) {
            log.error("数据库操作执行失败: {}", message, e);
            message.setSuccess(false);
            return false;
        }
    }

    /**
     * 处理重试逻辑
     * 
     * @param message 需要重试的消息
     */
    protected void handleRetry(DBOperationMessage message) {
        // 如果重试次数为空，初始化为0
        if (message.getRetryCount() == null) {
            message.setRetryCount(0);
        }

        // 最大重试次数
        int maxRetries = getMaxRetries();
        
        if (message.getRetryCount() < maxRetries) {
            message.setRetryCount(message.getRetryCount() + 1);
            
            // 延迟级别根据重试次数递增
            int delayLevel = message.getRetryCount();
            
            // 发送延迟消息进行重试
            getMQProducer().sendDelayMessage(message, delayLevel);
            log.info("消息进入重试队列，当前重试次数: {}", message.getRetryCount());
        } else {
            log.error("消息重试次数已达上限，放弃处理: {}", message);
            // 可以考虑将消息保存到数据库或发送到死信队列
            handleMaxRetriesExceeded(message);
        }
    }
    
    /**
     * 获取最大重试次数
     * 
     * @return 最大重试次数
     */
    protected int getMaxRetries() {
        return 3; // 默认最大重试3次
    }
    
    /**
     * 获取当前使用的MQ生产者
     * 
     * @return MQ生产者
     */
    protected abstract MQProducer getMQProducer();
    
    /**
     * 处理超过最大重试次数的消息
     * 子类可以重写此方法实现自定义处理
     * 
     * @param message 超过重试次数的消息
     */
    protected void handleMaxRetriesExceeded(DBOperationMessage message) {
        // 默认实现只记录日志，子类可以覆盖此方法
        log.error("消息 {} 超过最大重试次数", message.getMessageId());
    }
} 