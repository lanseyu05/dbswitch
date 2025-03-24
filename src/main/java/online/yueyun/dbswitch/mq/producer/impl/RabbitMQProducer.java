package online.yueyun.dbswitch.mq.producer.impl;

import lombok.extern.slf4j.Slf4j;
import online.yueyun.dbswitch.message.DBOperationMessage;
import online.yueyun.dbswitch.mq.producer.MQProducer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * RabbitMQ生产者实现
 */
@Slf4j
@Component("rabbitMQProducer")
@ConditionalOnClass(RabbitTemplate.class)
public class RabbitMQProducer implements MQProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${dbswitch.rabbitmq.exchange:dbswitch-exchange}")
    private String exchange;

    @Value("${dbswitch.rabbitmq.routingKey:dbswitch-routingkey}")
    private String routingKey;

    @Value("${dbswitch.rabbitmq.delayExchange:dbswitch-delay-exchange}")
    private String delayExchange;

    @Value("${dbswitch.rabbitmq.delayRoutingKey:dbswitch-delay-routingkey}")
    private String delayRoutingKey;

    @Override
    public boolean sendMessage(DBOperationMessage message) {
        try {
            if (message.getMessageId() == null) {
                message.setMessageId(UUID.randomUUID().toString());
            }
            
            log.info("发送RabbitMQ消息: {}", message);
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            return true;
        } catch (Exception e) {
            log.error("发送RabbitMQ消息失败: {}", message, e);
            return false;
        }
    }

    @Override
    public boolean sendDelayMessage(DBOperationMessage message, int delayLevel) {
        try {
            if (message.getMessageId() == null) {
                message.setMessageId(UUID.randomUUID().toString());
            }
            
            // 将delayLevel转换为毫秒，假设delayLevel为1-18对应不同的延迟时间
            int delayMillis = delayLevelToMillis(delayLevel);
            
            log.info("发送RabbitMQ延迟消息: {}, 延迟时间: {}ms", message, delayMillis);
            rabbitTemplate.convertAndSend(delayExchange, delayRoutingKey, message, msg -> {
                msg.getMessageProperties().setDelay(delayMillis);
                return msg;
            });
            return true;
        } catch (Exception e) {
            log.error("发送RabbitMQ延迟消息失败: {}", message, e);
            return false;
        }
    }
    
    /**
     * 将延迟级别转换为毫秒
     * 参考RocketMQ的延迟级别：1s, 5s, 10s, 30s, 1m, 2m, 3m, 4m, 5m, 6m, 7m, 8m, 9m, 10m, 20m, 30m, 1h, 2h
     */
    private int delayLevelToMillis(int delayLevel) {
        int[] delayMillis = {
            1000, 5000, 10000, 30000, 60000, 120000, 180000, 240000, 300000, 
            360000, 420000, 480000, 540000, 600000, 1200000, 1800000, 3600000, 7200000
        };
        
        if (delayLevel < 1) {
            return 0;
        }
        
        if (delayLevel > delayMillis.length) {
            return delayMillis[delayMillis.length - 1];
        }
        
        return delayMillis[delayLevel - 1];
    }
} 