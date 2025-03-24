package online.yueyun.dbswitch.mq.producer;

import online.yueyun.dbswitch.message.DBOperationMessage;

/**
 * 消息队列生产者接口
 */
public interface MQProducer {

    /**
     * 发送消息
     *
     * @param message 数据库操作消息
     * @return 是否发送成功
     */
    boolean sendMessage(DBOperationMessage message);

    /**
     * 发送延迟消息
     *
     * @param message     数据库操作消息
     * @param delayLevel  延迟级别
     * @return 是否发送成功
     */
    boolean sendDelayMessage(DBOperationMessage message, int delayLevel);
} 