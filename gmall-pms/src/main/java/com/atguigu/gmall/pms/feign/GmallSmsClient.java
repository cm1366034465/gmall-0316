package com.atguigu.gmall.pms.feign;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.sms.api.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Auther: cfy
 * @Date: 2020/08/24/23:28
 * @Description: TODO
 */
@FeignClient("sms-service")
public interface GmallSmsClient extends GmallSmsApi {

}
