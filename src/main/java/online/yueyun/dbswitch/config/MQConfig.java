package online.yueyun.dbswitch.config;

import lombok.extern.slf4j.Slf4j;
import online.yueyun.dbswitch.enums.MQType;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息队列配置
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "dbswitch.enabled", havingValue = "true", matchIfMissing = true)
public class MQConfig {

    @Value("${dbswitch.mq.type:#{null}}")
    private String mqTypeProperty;

    @Value("${dbswitch.rabbitmq.exchange:dbswitch-exchange}")
    private String exchange;

    @Value("${dbswitch.rabbitmq.queue:dbswitch-queue}")
    private String queue;

    @Value("${dbswitch.rabbitmq.routingKey:dbswitch-routingkey}")
    private String routingKey;

    @Value("${dbswitch.rabbitmq.delayExchange:dbswitch-delay-exchange}")
    private String delayExchange;

    @Value("${dbswitch.rabbitmq.delayQueue:dbswitch-delay-queue}")
    private String delayQueue;

    @Value("${dbswitch.rabbitmq.delayRoutingKey:dbswitch-delay-routingkey}")
    private String delayRoutingKey;

    /**
     * 获取当前消息队列类型
     * 优先级：配置属性 > RocketMQ类存在 > RabbitMQ类存在
     */
    @Bean
    @ConditionalOnClass(name = "org.apache.rocketmq.spring.core.RocketMQTemplate")
    public MQType rocketMQTypeBean() {
        if (mqTypeProperty != null && !mqTypeProperty.isEmpty()) {
            try {
                MQType configuredType = MQType.valueOf(mqTypeProperty);
                log.info("使用配置的消息队列类型: {}", configuredType);
                return configuredType;
            } catch (IllegalArgumentException e) {
                log.warn("不支持的消息队列类型配置: {}, 将使用自动检测", mqTypeProperty);
            }
        }
        log.info("检测到RocketMQ依赖，将使用RocketMQ");
        return MQType.ROCKET_MQ;
    }

    /**
     * 当RocketMQ不存在时，尝试使用RabbitMQ
     */
    @Bean
    @ConditionalOnMissingClass("org.apache.rocketmq.spring.core.RocketMQTemplate")
    @ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
    public MQType rabbitMQTypeBean() {
        if (mqTypeProperty != null && !mqTypeProperty.isEmpty()) {
            try {
                MQType configuredType = MQType.valueOf(mqTypeProperty);
                log.info("使用配置的消息队列类型: {}", configuredType);
                return configuredType;
            } catch (IllegalArgumentException e) {
                log.warn("不支持的消息队列类型配置: {}, 将使用自动检测", mqTypeProperty);
            }
        }
        log.info("未检测到RocketMQ依赖，但检测到RabbitMQ依赖，将使用RabbitMQ");
        return MQType.RABBIT_MQ;
    }

    /**
     * 当两者都不存在时的默认配置
     */
    @Bean
    @ConditionalOnMissingClass({"org.apache.rocketmq.spring.core.RocketMQTemplate", "org.springframework.amqp.rabbit.core.RabbitTemplate"})
    public MQType defaultMQTypeBean() {
        log.warn("未检测到任何消息队列依赖，双写功能将不可用");
        if (mqTypeProperty != null && !mqTypeProperty.isEmpty()) {
            try {
                return MQType.valueOf(mqTypeProperty);
            } catch (IllegalArgumentException e) {
                log.error("配置的消息队列类型无效: {}", mqTypeProperty);
            }
        }
        return null;
    }

    /**
     * RabbitMQ交换机
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
    public DirectExchange directExchange() {
        return new DirectExchange(exchange);
    }

    /**
     * RabbitMQ队列
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
    public Queue queue() {
        return new Queue(queue);
    }

    /**
     * RabbitMQ绑定
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
    public Binding binding() {
        return BindingBuilder.bind(queue()).to(directExchange()).with(routingKey);
    }

    /**
     * RabbitMQ延迟交换机
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
    public CustomExchange delayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(delayExchange, "x-delayed-message", true, false, args);
    }

    /**
     * RabbitMQ延迟队列
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
    public Queue delayQueue() {
        return new Queue(delayQueue);
    }

    /**
     * RabbitMQ延迟绑定
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(delayRoutingKey).noargs();
    }
} 