package com.atguigu.gmall.oms.listener;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.service.OrderService;
import com.atguigu.gmall.oms.vo.UserBoundVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @Auther: cfy
 * @Date: 2020/09/08/21:06
 * @Description: TODO
 */
@Component
@Slf4j
public class OrderListener {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderService orderService;

    /**
     * 更改订单为无效状态
     *
     * @param orderToken
     * @param channel
     * @param message
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "ORDER-FAIL-QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.fail"}
    ))
    public void failOrder(String orderToken, Channel channel, Message message) throws IOException {
        try {
            OrderEntity orderEntity = this.orderMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderToken));
            orderEntity.setStatus(5);
            this.orderMapper.update(orderEntity, new QueryWrapper<OrderEntity>().eq("id", orderEntity.getId()).eq("status", 0));
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                log.error("标记未无效订单失败！订单编号{}", orderToken);
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }

    /**
     * 定时关单 并且解锁库存
     */
    @RabbitListener(queues = "ORDER-DEAD-QUEUE")
    public void closeOrder(String orderToken, Channel channel, Message message) throws IOException {
        try {
            OrderEntity orderEntity = this.orderMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderToken));
            orderEntity.setStatus(4);
            if (this.orderMapper.update(orderEntity, new QueryWrapper<OrderEntity>().eq("id", orderEntity.getId()).eq("status", 0)) == 1) {
                // 解锁库存
                this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE", "stock.unlock", orderToken);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                log.error("标记未无效订单失败！订单编号{}", orderToken);
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "ORDER-SUCCESS-QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE", ignoreDeclarationExceptions = "true"),
            key = {"order.success"}
    ))
    public void successOrder(String orderToken, Channel channel, Message message) throws IOException {
        // 更新订单状态
        int flag = this.orderMapper.successOrder(orderToken);
        if (flag == 1) {
            // 发送消息给wms减库存 真正
            this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE", "stock.minus", orderToken);

            // 发送消息给ums加积分
            OrderEntity orderEntity = this.orderService.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderToken));
            UserBoundVo userBoundVO = new UserBoundVo();
            userBoundVO.setUserId(orderEntity.getUserId());
            userBoundVO.setIntegration(orderEntity.getIntegration());
            userBoundVO.setGrowth(orderEntity.getGrowth());
            this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE", "bound.plus", userBoundVO);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
