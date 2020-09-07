package com.atguigu.gmall.gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @Auther: cfy
 * @Date: 2020/09/04/23:02
 * @Description: TODO
 */
@Component
@Order(1)
public class MyGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("无需配置，拦截所有经过网关的请求！！");
        // 放行
        return chain.filter(exchange);
    }

    /**
     * 通过实现Orderer接口的getOrder方法控制全局过滤器的执行顺序
     *
     * @return
     */
    @Override
    public int getOrder() {
        return 10;
    }
}
