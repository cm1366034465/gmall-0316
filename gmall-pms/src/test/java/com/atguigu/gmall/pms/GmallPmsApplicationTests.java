package com.atguigu.gmall.pms;

import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class GmallPmsApplicationTests {

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Test
    void contextLoads() {
        String s = this.skuAttrValueService.querySaleAttrMappingSkuIdBySpuId(31L);
        System.out.println(s);
    }

}
