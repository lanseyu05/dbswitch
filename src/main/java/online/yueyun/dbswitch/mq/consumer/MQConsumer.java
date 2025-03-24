package online.yueyun.dbswitch.mq.consumer;

import online.yueyun.dbswitch.message.DBOperationMessage;

/**
 * 消息队列消费者接口
 */
public interface MQConsumer {

    /**
     * 消费消息
     *
     * @param message 数据库操作消息
     * @return 是否消费成功
     */
    boolean consumeMessage(DBOperationMessage message);
} 