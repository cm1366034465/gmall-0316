package com.atguigu.gmall.order.feign;

import com.atguigu.gmall.ums.api.GmallUmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/07/22:28
 * @Description: TODO
 */
@FeignClient("ums-service")
public interface GmallUmsClient extends GmallUmsApi {
}
