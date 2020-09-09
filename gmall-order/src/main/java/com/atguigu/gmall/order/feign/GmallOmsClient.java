package com.atguigu.gmall.order.feign;

import com.atguigu.gmall.oms.api.GmallOmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/08/19:55
 * @Description: TODO
 */
@FeignClient("oms-service")
public interface GmallOmsClient extends GmallOmsApi {
}
