package online.yueyun.dbswitch.mq.consumer.impl;

import lombok.extern.slf4j.Slf4j;
import online.yueyun.dbswitch.message.DBOperationMessage;
import online.yueyun.dbswitch.mq.consumer.AbstractMQConsumer;
import online.yueyun.dbswitch.mq.producer.MQProducer;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * RocketMQ消费者实现
 */
@Slf4j
@Component
@ConditionalOnClass(RocketMQTemplate.class)
@ConditionalOnBean(name = "rocketMQProducer")
@RocketMQMessageListener(
        topic = "${dbswitch.rocketmq.topic:dbswitch-topic}",
        consumerGroup = "${dbswitch.rocketmq.consumer-group:dbswitch-consumer-group}"
)
public class RocketMQConsumer extends AbstractMQConsumer implements RocketMQListener<DBOperationMessage> {

    @Autowired
    @Qualifier("rocketMQProducer")
    private MQProducer mqProducer;

    /**
     * RocketMQ消息监听处理
     */
    @Override
    public void onMessage(DBOperationMessage message) {
        processMessage(message);
    }

    /**
     * 获取RocketMQ生产者
     */
    @Override
    protected MQProducer getMQProducer() {
        return mqProducer;
    }
} 