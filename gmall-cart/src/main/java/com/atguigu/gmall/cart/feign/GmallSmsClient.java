package com.atguigu.gmall.cart.feign;

import com.atguigu.gmall.sms.api.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/06/1:41
 * @Description: TODO
 */
@FeignClient("sms-service")
public interface GmallSmsClient extends GmallSmsApi {
}