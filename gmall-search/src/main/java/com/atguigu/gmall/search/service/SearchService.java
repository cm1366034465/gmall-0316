package com.atguigu.gmall.search.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.atguigu.gmall.search.respository.GoodsRespository;
import com.atguigu.gmall.search.vo.SearchAttrValueVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.org.apache.regexp.internal.RE;
import jdk.internal.util.xml.impl.Attrs;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Mapper;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Attr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Auther: cfy
 * @Date: 2020/08/29/11:53
 * @Description: TODO
 * search.gmall.com/search?keyword=手机&brandId=2,3,4&categoryId=225,250&props=4:6G-8G-12G&props=5:128G&sort=1&store=true&priceFrom=1000&priceTo=6000&pageNum=1
 */
@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public SearchResponseVo search(SearchParamVo paramVo) {
        try {
            SearchSourceBuilder sourceBuilder = buildDSL(paramVo);
            SearchResponse response = this.restHighLevelClient.search(new SearchRequest(new String[]{"goods"}, sourceBuilder), RequestOptions.DEFAULT);
            SearchResponseVo responseVo = this.parseSearchResult(response);
            // 通过查询条件获取分页数据
            responseVo.setPageNum(paramVo.getPageNum());
            responseVo.setPageSize(paramVo.getPageSize());
            return responseVo;
        } catch (IOException e) {
            // 显示默认信息，打广告 TODO
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解析搜索数据
     */
    private SearchResponseVo parseSearchResult(SearchResponse response) {
        SearchResponseVo responseVo = new SearchResponseVo();
        // 完成转换
        // 解析搜索数据中的hits
        SearchHits hits = response.getHits();
        // 总记录数
        responseVo.setTotal(hits.getTotalHits());

        // 当前页的数据
        SearchHit[] hitsHits = hits.getHits();
        List<Goods> goodsList = Arrays.stream(hitsHits).map(hitsHit -> {
            try {
                // 获取hhit中的_source
                String json = hitsHit.getSourceAsString();
                System.out.println("json = " + json);
                // 反序列化，获取Goods对象
                Goods goods = MAPPER.readValue(json, Goods.class);
                // 高亮标题替换普通title
                HighlightField title = hitsHit.getHighlightFields().get("title");
                Text[] fragments = title.getFragments();
                goods.setTitle(fragments[0].toString());
                return goods;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList);

        // 解析搜索数据中的聚合数据
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();

        // 获取品牌聚合，解析出品牌集合
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> brandBuckets = brandIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(brandBuckets)) {
            List<BrandEntity> brandEntities = brandBuckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                // 获取桶中的key,设置给品牌id
                brandEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());

                // 获取桶中的所有子聚合
                Map<String, Aggregation> brandAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();

                // 获取品牌名称的子聚合
                ParsedStringTerms brandNameAgg = (ParsedStringTerms) brandAggregationMap.get("brandNameAgg");

                // 获取品牌名称的子聚合中的桶
                List<? extends Terms.Bucket> nameBuckets = brandNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameBuckets)) {
                    brandEntity.setName(nameBuckets.get(0).getKeyAsString());
                }

                // 获取logo子聚合
                ParsedStringTerms logoAgg = (ParsedStringTerms) brandAggregationMap.get("logoAgg");
                List<? extends Terms.Bucket> logoBuckets = logoAgg.getBuckets();
                // 获取logo子聚合的桶,获取桶中的key
                if (!CollectionUtils.isEmpty(logoBuckets)) {
                    brandEntity.setLogo(logoBuckets.get(0).getKeyAsString());
                }

                return brandEntity;
            }).collect(Collectors.toList());
            responseVo.setBrands(brandEntities);
        }

        // 获取分类id的聚合
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(categoryBuckets)) {
            List<CategoryEntity> categoryEntities = categoryBuckets.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                // 设置id
                categoryEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());

                // 获取分类名称的子聚合
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms) ((Terms.Bucket) bucket).getAggregations().get("categoryNameAgg");
                List<? extends Terms.Bucket> nameBuckets = categoryNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameBuckets)) {
                    categoryEntity.setName(nameBuckets.get(0).getKeyAsString());
                }
                return categoryEntity;
            }).collect(Collectors.toList());
            responseVo.setCategories(categoryEntities);
        }

        // 获取规格参数的嵌套聚合
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        // 获取嵌套集合中的attrIdAgg子聚合
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrAgg.getAggregations().get("attrIdAgg");
        // 获取子聚合中的所有桶
        List<? extends Terms.Bucket> idAggBuckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(idAggBuckets)) {
            List<SearchResponseAttrVo> attrVos = idAggBuckets.stream().map(bucket -> {
                SearchResponseAttrVo responseAttrVo = new SearchResponseAttrVo();
                // 桶中key就是规格参数的id
                responseAttrVo.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());

                // 获取桶中的子聚合
                Map<String, Aggregation> attrAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
                ParsedStringTerms attrNameAgg = (ParsedStringTerms) attrAggregationMap.get("attrNameAgg");
                List<? extends Terms.Bucket> nameBuckets = attrNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameBuckets)) {
                    responseAttrVo.setAttrName(nameBuckets.get(0).getKeyAsString());
                }

                // 获取规格参数值的子聚合
                ParsedStringTerms attrValueAgg = (ParsedStringTerms) attrAggregationMap.get("attrValueAgg");
                List<? extends Terms.Bucket> valueBuckets = attrValueAgg.getBuckets();
                if (!CollectionUtils.isEmpty(valueBuckets)) {
                    List<String> attrValues = valueBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                    responseAttrVo.setAttrValues(attrValues);
                }
                return responseAttrVo;
            }).collect(Collectors.toList());
            responseVo.setFilters(attrVos);
        }

        return responseVo;
    }

    /**
     * 构建查询DSL语句
     *
     * @return
     */
    private SearchSourceBuilder buildDSL(SearchParamVo paramVo) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        String keyword = paramVo.getKeyword();
        if (StringUtils.isBlank(keyword)) {
            // TODO 打广告
            // 关键字为空的时，直接返回sourceBuilder对象
            return sourceBuilder;
        }
        // 1 构建查询条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);

        // 1.1 匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));

        // 1.2 品牌过滤
        List<Long> brandId = paramVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandId));
        }

        // 1.3 分类过滤
        List<Long> categoryId = paramVo.getCategoryId();
        if (!CollectionUtils.isEmpty(categoryId)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", categoryId));
        }

        // 1.4 规格参数过滤(相对麻烦，可能有多个参数)
        // &props=4:6G-8G-12G&props=5:128G
        List<String> props = paramVo.getProps();
        if (!CollectionUtils.isEmpty(props)) {
            props.forEach(prop -> {
                // 4:6G-8G-12G split 分割
                String[] attr = StringUtils.split(prop, ":");
                if (attr != null && attr.length == 2) {
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attr[0]));
                    // 6G-8G-12G
                    String[] attrValues = StringUtils.split(attr[1], "-");
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", attrValues));
                    NestedQueryBuilder searchAttrs = QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None);
                    boolQueryBuilder.filter(searchAttrs);
                }
            });
        }

        // 1.5 价格区间过滤
        Double priceFrom = paramVo.getPriceFrom();
        Double priceTo = paramVo.getPriceTo();
        if (priceFrom != null || priceTo != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if (priceFrom != null) {
                rangeQuery.gte(priceFrom);
            }
            if (priceTo != null) {
                rangeQuery.lte(priceTo);
            }
            boolQueryBuilder.filter(rangeQuery);
        }

        // 1.6 是否有货
        Boolean store = paramVo.getStore();
        if (store != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("store", store));
        }

        // 2 排序 0得分排序，1价格升序 2价格降序 3新品降序 4销量降序
        // switch的default处理
        Integer sort = paramVo.getSort();
        if (sort == null) {
            sort = 0;
        }
        switch (sort) {
            case 1:
                sourceBuilder.sort("price", SortOrder.DESC);
                break;
            case 2:
                sourceBuilder.sort("price", SortOrder.ASC);
                break;
            case 3:
                sourceBuilder.sort("createTime", SortOrder.DESC);
                break;
            case 4:
                sourceBuilder.sort("sales", SortOrder.DESC);
                break;
            default:
                sourceBuilder.sort("_score", SortOrder.DESC);
                break;
        }

        // 3 分页 类中设置默认值
        Integer pageNum = paramVo.getPageNum();
        Integer pageSize = paramVo.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);

        // 4 高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<font style = 'color:red;'>").postTags("</font>"));

        // 5 构建聚合
        // 5.1 品牌
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo"))

        );

        // 5.2 分类
        sourceBuilder.aggregation(
                AggregationBuilders.terms("categoryIdAgg").field("categoryId").subAggregation(
                        AggregationBuilders.terms("categoryNameAgg").field("categoryName")
                )
        );

        // 5.3 规格参数聚合
        sourceBuilder.aggregation(
                AggregationBuilders.nested("attrAgg", "searchAttrs")
                        .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                                .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                                .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))
                        ));

        // 6.添加结果集过滤，过滤出goods中所需要的5个字段
        sourceBuilder.fetchSource(new String[]{"skuId", "defaultImage", "price", "title", "subTitle"}, null);

        System.out.println(sourceBuilder.toString());
        return sourceBuilder;
    }

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GoodsRespository goodsRepository;

    public void createIndex(Long spuId) {
        ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(spuId);
        SpuEntity spuEntity = spuEntityResponseVo.getData();

        ResponseVo<List<SkuEntity>> skuResp = this.pmsClient.querySkusBySpuId(spuId);
        List<SkuEntity> skuEntities = skuResp.getData();
        if (!org.springframework.util.CollectionUtils.isEmpty(skuEntities)) {
            // 把sku转化成goods对象
            List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                Goods goods = new Goods();

                // 查询spu搜索属性及值
                ResponseVo<List<SpuAttrValueEntity>> attrValueResp = this.pmsClient.querySearchAttrValueBySpuId(spuId);
                List<SpuAttrValueEntity> attrValueEntities = attrValueResp.getData();
                List<SearchAttrValueVo> searchAttrValues = new ArrayList<>();
                if (!org.springframework.util.CollectionUtils.isEmpty(attrValueEntities)) {
                    searchAttrValues = attrValueEntities.stream().map(spuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValue = new SearchAttrValueVo();
                        searchAttrValue.setAttrId(spuAttrValueEntity.getAttrId());
                        searchAttrValue.setAttrName(spuAttrValueEntity.getAttrName());
                        searchAttrValue.setAttrValue(spuAttrValueEntity.getAttrValue());
                        return searchAttrValue;
                    }).collect(Collectors.toList());
                }
                // 查询sku搜索属性及值
                ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResp = this.pmsClient.querySearchAttrValueBySkuId(spuId);
                List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResp.getData();
                List<SearchAttrValueVo> searchSkuAttrValues = new ArrayList<>();
                if (!org.springframework.util.CollectionUtils.isEmpty(skuAttrValueEntities)) {
                    searchSkuAttrValues = skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValue = new SearchAttrValueVo();
                        searchAttrValue.setAttrId(skuAttrValueEntity.getAttrId());
                        searchAttrValue.setAttrName(skuAttrValueEntity.getAttrName());
                        searchAttrValue.setAttrValue(skuAttrValueEntity.getAttrValue());
                        return searchAttrValue;
                    }).collect(Collectors.toList());
                }
                searchAttrValues.addAll(searchSkuAttrValues);
                goods.setSearchAttrs(searchAttrValues);

                // 查询品牌
                ResponseVo<BrandEntity> brandEntityResp = this.pmsClient.queryBrandById(skuEntity.getBrandId());
                BrandEntity brandEntity = brandEntityResp.getData();
                if (brandEntity != null) {
                    goods.setBrandId(skuEntity.getBrandId());
                    goods.setBrandName(brandEntity.getName());
                    goods.setLogo(brandEntity.getLogo());
                }

                // 查询分类
                ResponseVo<CategoryEntity> categoryEntityResp = this.pmsClient.queryCategoryById(skuEntity.getCatagoryId());
                CategoryEntity categoryEntity = categoryEntityResp.getData();
                if (categoryEntity != null) {
                    goods.setCategoryId(skuEntity.getCatagoryId());
                    goods.setCategoryName(categoryEntity.getName());
                }

                goods.setCreateTime(spuEntity.getCreateTime());
                goods.setDefaultImage(skuEntity.getDefaultImage());
                goods.setPrice(skuEntity.getPrice().doubleValue());
                goods.setSales(0l);
                goods.setSkuId(skuEntity.getId());

                // 查询库存信息
                ResponseVo<List<WareSkuEntity>> listResp = this.wmsClient.queryWareSkuBySkuId(skuEntity.getId());
                List<WareSkuEntity> wareSkuEntities = listResp.getData();
                if (!org.springframework.util.CollectionUtils.isEmpty(wareSkuEntities)) {
                    boolean flag = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
                    goods.setStore(flag);
                    // 设置销量
                    goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get());
                }
                goods.setTitle(skuEntity.getTitle());
                goods.setSubTitle(skuEntity.getSubtitle());
                return goods;
            }).collect(Collectors.toList());

            // 导入索引库
            this.goodsRepository.saveAll(goodsList);
        }
    }
}
