package online.yueyun.dbswitch.mq.producer.impl;

import lombok.extern.slf4j.Slf4j;
import online.yueyun.dbswitch.message.DBOperationMessage;
import online.yueyun.dbswitch.mq.producer.MQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * RocketMQ生产者实现
 */
@Slf4j
@Component("rocketMQProducer")
@ConditionalOnClass(RocketMQTemplate.class)
public class RocketMQProducer implements MQProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${dbswitch.rocketmq.topic:dbswitch-topic}")
    private String topic;

    @Override
    public boolean sendMessage(DBOperationMessage message) {
        try {
            if (message.getMessageId() == null) {
                message.setMessageId(UUID.randomUUID().toString());
            }
            
            log.info("发送RocketMQ消息: {}", message);
            rocketMQTemplate.convertAndSend(topic, message);
            return true;
        } catch (Exception e) {
            log.error("发送RocketMQ消息失败: {}", message, e);
            return false;
        }
    }

    @Override
    public boolean sendDelayMessage(DBOperationMessage message, int delayLevel) {
        try {
            if (message.getMessageId() == null) {
                message.setMessageId(UUID.randomUUID().toString());
            }
            
            log.info("发送RocketMQ延迟消息: {}, 延迟级别: {}", message, delayLevel);
            rocketMQTemplate.syncSend(topic, 
                MessageBuilder.withPayload(message).build(), 
                3000, 
                delayLevel);
            return true;
        } catch (Exception e) {
            log.error("发送RocketMQ延迟消息失败: {}", message, e);
            return false;
        }
    }
} 