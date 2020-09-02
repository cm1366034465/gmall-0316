package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.search.service.SearchService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Auther: cfy
 * @Date: 2020/09/01/0:18
 * @Description: TODO
 */
@Component
public class SpuListener {
    @Autowired
    private SearchService searchService;

    /**
     * 处理insert的消息
     *
     * @param id
     * @throws Exception
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "item_spu_queue", durable = "true"),
            exchange = @Exchange(
                    value = "item_exchange",
                    ignoreDeclarationExceptions = "true",
                    type = ExchangeTypes.TOPIC),
            key = {"item.insert"}))
    public void listenCreate(Long id, Channel channel, Message message) throws Exception {
        if (id == null) {
            return;
        }
        try {
            // 创建索引 消费消息
            this.searchService.createIndex(id);

            // 消费确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                // 比如 放入数据库
                // 一条消息被拒绝，该队列绑定了死信队列的情况下，该消息会进入死信队列
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }
}
