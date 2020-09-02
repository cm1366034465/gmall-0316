package com.atguigu.gmall.search.respository;

import com.atguigu.gmall.search.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @Auther: cfy
 * @Date: 2020/08/28/21:00
 * @Description: TODO
 */
public interface GoodsRespository extends ElasticsearchRepository<Goods, Long> {
}
