package com.atguigu.gmall.auth.feign;

import com.atguigu.gmall.ums.api.GmallUmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/04/20:00
 * @Description: TODO
 */
@FeignClient("ums-service")
public interface GmallUmsClient extends GmallUmsApi {
}
