package com.atguigu.gmall.oms.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/08/16:53
 * @Description: TODO
 */
@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {
}
