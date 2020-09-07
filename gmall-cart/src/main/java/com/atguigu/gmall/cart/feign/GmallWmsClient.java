package com.atguigu.gmall.cart.feign;

import com.atguigu.gmall.wms.api.GmallWmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/06/1:42
 * @Description: TODO
 */
@FeignClient("wms-service")
public interface GmallWmsClient extends GmallWmsApi {
}
