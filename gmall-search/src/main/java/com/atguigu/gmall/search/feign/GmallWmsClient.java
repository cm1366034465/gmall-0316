package com.atguigu.gmall.search.feign;

import com.atguigu.gmall.wms.api.GmallWmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/08/28/20:58
 * @Description: TODO
 */
@FeignClient("wms-service")
public interface GmallWmsClient extends GmallWmsApi {
}
