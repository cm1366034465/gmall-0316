package com.atguigu.gmall.order.feign;

import com.atguigu.gmall.sms.api.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/07/22:28
 * @Description: TODO
 */
@FeignClient("sms-service")
public interface GmallSmsClient extends GmallSmsApi {
}
