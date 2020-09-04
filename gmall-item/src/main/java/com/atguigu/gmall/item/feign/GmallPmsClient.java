package com.atguigu.gmall.item.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Auther: cfy
 * @Date: 2020/09/03/9:29
 * @Description: TODO
 */
@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {
}
