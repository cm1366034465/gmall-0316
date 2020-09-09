package com.atguigu.gmall.oms.feign;

import com.atguigu.gmall.ums.api.GmallUmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/08/15:11
 * @Description: TODO
 */
@FeignClient("ums-service")
public interface GmallUmsClient extends GmallUmsApi {
}
