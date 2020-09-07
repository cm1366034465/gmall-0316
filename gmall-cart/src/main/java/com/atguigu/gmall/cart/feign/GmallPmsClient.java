package com.atguigu.gmall.cart.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/06/1:35
 * @Description: TODO
 */
@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {
}
