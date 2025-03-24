package online.yueyun.dbswitch.mq.consumer.impl;

import lombok.extern.slf4j.Slf4j;
import online.yueyun.dbswitch.message.DBOperationMessage;
import online.yueyun.dbswitch.mq.consumer.AbstractMQConsumer;
import online.yueyun.dbswitch.mq.producer.MQProducer;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ消费者实现
 */
@Slf4j
@Component
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnBean(name = "rabbitMQProducer")
public class RabbitMQConsumer extends AbstractMQConsumer {

    @Autowired
    @Qualifier("rabbitMQProducer")
    private MQProducer mqProducer;

    /**
     * RabbitMQ消息监听处理
     */
    @RabbitListener(queues = "${dbswitch.rabbitmq.queue:dbswitch-queue}")
    public void onMessage(DBOperationMessage message) {
        processMessage(message);
    }

    /**
     * 获取RabbitMQ生产者
     */
    @Override
    protected MQProducer getMQProducer() {
        return mqProducer;
    }
} 