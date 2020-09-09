package com.atguigu.gmall.order.feign;

import com.atguigu.gmall.cart.api.GmallCartApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/07/22:26
 * @Description: TODO
 */
@FeignClient("cart-service")
public interface GmallCartClient extends GmallCartApi {
}
