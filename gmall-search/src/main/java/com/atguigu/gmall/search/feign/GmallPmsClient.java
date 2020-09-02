package com.atguigu.gmall.search.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/08/28/20:58
 * @Description: TODO
 */
@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {
}
