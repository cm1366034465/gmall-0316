package com.atguigu.gmall.order.feign;

import com.atguigu.gmall.wms.api.GmallWmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/07/22:29
 * @Description: TODO
 */
@FeignClient("wms-service")
public interface GmallWmsClient extends GmallWmsApi {
}
