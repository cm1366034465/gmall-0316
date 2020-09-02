package com.atguigu.gmall.index.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/01/16:06
 * @Description: TODO
 */
@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {
}
