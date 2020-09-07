package com.atguigu.gmall.scheduled.feign;

import com.atguigu.gmall.cart.api.GmallCartApi;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

/**
 * @Auther: cfy
 * @Date: 2020/09/07/15:22
 * @Description: TODO
 */
@FeignClient("cart-service")
public interface GmallCartClient extends GmallCartApi {
}
