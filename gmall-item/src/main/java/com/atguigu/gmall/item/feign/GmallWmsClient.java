package com.atguigu.gmall.item.feign;

import com.atguigu.gmall.wms.api.GmallWmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/03/9:30
 * @Description: TODO
 */
@FeignClient("wms-service")
public interface GmallWmsClient extends GmallWmsApi {
}
