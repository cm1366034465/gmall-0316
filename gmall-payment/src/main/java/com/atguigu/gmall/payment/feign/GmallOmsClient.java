package com.atguigu.gmall.payment.feign;

import com.atguigu.gmall.oms.api.GmallOmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/09/13:32
 * @Description: TODO
 */
@FeignClient("oms-service")
public interface GmallOmsClient extends GmallOmsApi {
}
