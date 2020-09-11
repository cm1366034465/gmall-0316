package com.atguigu.gmall.payment.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.bean.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.entity.PaymentInfoEntity;
import com.atguigu.gmall.payment.feign.GmallOmsClient;
import com.atguigu.gmall.payment.interceptor.LoginInterceptor;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @Auther: cfy
 * @Date: 2020/09/09/13:31
 * @Description: TODO
 */
@Service
public class PaymentService {
    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    public OrderEntity queryOrderByOrderToken(String orderToken) {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        // 20200909 15:22 如果用户未登录？ 拦截器已经完善，必然是登录的用户
        ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.queryOrderByToken(orderToken, userInfo.getUserId());
        OrderEntity orderEntity = orderEntityResponseVo.getData();
        return orderEntity;
    }

    public Long savePayment(OrderEntity orderEntity) {
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();

        paymentInfoEntity.setPaymentStatus(0);
        paymentInfoEntity.setCreateTime(new Date());
        paymentInfoEntity.setTotalAmount(orderEntity.getPayAmount());
        paymentInfoEntity.setSubject("谷粒商城支付平台");
        paymentInfoEntity.setPaymentType(1);
        paymentInfoEntity.setOutTradeNo(orderEntity.getOrderSn());

        this.paymentInfoMapper.insert(paymentInfoEntity);

        return paymentInfoEntity.getId();
    }

    public PaymentInfoEntity queryPaymentById(String paymentId) {
        return this.paymentInfoMapper.selectById(paymentId);
    }

    public void updatePayment(PayAsyncVo payAsyncVo) {
        PaymentInfoEntity paymentInfoEntity = this.paymentInfoMapper.selectById(payAsyncVo.getPassback_params());

        paymentInfoEntity.setPaymentStatus(1);
        paymentInfoEntity.setCallbackTime(new Date());
        paymentInfoEntity.setCallbackContent(JSON.toJSONString(payAsyncVo));
        paymentInfoEntity.setTradeNo(payAsyncVo.getTrade_no());

        this.paymentInfoMapper.updateById(paymentInfoEntity);
    }
}
