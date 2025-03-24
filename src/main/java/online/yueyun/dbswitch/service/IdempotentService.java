package online.yueyun.dbswitch.service;

/**
 * 幂等性服务接口
 */
public interface IdempotentService {

    /**
     * 检查消息是否已处理
     *
     * @param messageId 消息ID
     * @return 是否已处理
     */
    boolean isProcessed(String messageId);

    /**
     * 标记消息已处理
     *
     * @param messageId 消息ID
     */
    void markAsProcessed(String messageId);
} 