package com.atguigu.gmall.item.feign;

import com.atguigu.gmall.sms.api.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/03/9:30
 * @Description: TODO
 */
@FeignClient("sms-service")
public interface GmallSmsClient extends GmallSmsApi {
}
