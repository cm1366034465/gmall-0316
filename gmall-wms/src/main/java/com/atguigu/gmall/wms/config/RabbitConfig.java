package com.atguigu.gmall.wms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * @Auther: cfy
 * @Date: 2020/09/08/20:03
 * @Description: TODO
 */
@Configuration
@Slf4j
public class RabbitConfig {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("消息没有到达交换机" + cause);
            }
        });

        this.rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.error("消费没有到达队列：交换机{};路由键{};消息内容{}", exchange, routingKey, new String(message.getBody()));
        });
    }

    // 声明延时交换机：借用order-exchange

    // 声明延时队列
    @Bean
    public Queue ttlQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-message-ttl", 100000);
        arguments.put("x-dead-letter-exchange", "ORDER-EXCHANGE");
        arguments.put("x-dead-letter-routing-key", "stock.unlock");
        return new Queue("STOCK-TTL-QUEUE", true, false, false, arguments);
    }

    // 把延时队列绑定到交换机
    @Bean
    public Binding ttlBinding() {

        return new Binding("STOCK-TTL-QUEUE", Binding.DestinationType.QUEUE, "ORDER-EXCHANGE", "stock.ttl", null);
    }

    // 声明死信交换机：借用order-exchange

    // 声明死信队列：借用order-stock-queue

    // 把死信队列绑定到死信交换机：注解中已绑定
}
