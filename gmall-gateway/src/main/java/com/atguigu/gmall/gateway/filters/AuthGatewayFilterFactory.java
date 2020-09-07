package com.atguigu.gmall.gateway.filters;

import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.google.common.net.HttpHeaders;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Auther: cfy
 * @Date: 2020/09/04/23:22
 * @Description: TODO
 */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 一定要重写构造方法
     * 告诉父类，这里使用PathConfig对象接收配置内容
     */
    public AuthGatewayFilterFactory() {
        super(PathConfig.class);
    }

    @Override
    public GatewayFilter apply(PathConfig config) {
        // 实现GatewaFilter接口
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

                // 判断当前路径在不在拦截黑名单中，不在直接放行
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();

                // 获取当前路径
                String curPath = request.getURI().getPath();
                List<String> paths = config.authPaths;

                if (paths.stream().allMatch(path -> curPath.indexOf(path) == -1)) {
                    return chain.filter(exchange);
                }

                // 获取cookie中的token信息
                String token = request.getHeaders().getFirst("token");
                if (StringUtils.isBlank(token)) {
                    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                    if (!CollectionUtils.isEmpty(cookies) && cookies.containsKey(jwtProperties.getCookieName())) {
                        token = cookies.getFirst(jwtProperties.getCookieName()).getValue();
                    }
                }

                // 判断token是否为空 拦截重定向到登录界面
                if (StringUtils.isBlank(token)) {
                    return interceptor(request, response);
                }

                try {
                    // 解析jwt类型的token 获取用户信息
                    Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());

                    // 判断当前用户ip和token中的ip是否一致，防止盗用
                    String ip = map.get("ip").toString();
                    String curIp = IpUtil.getIpAddressAtGateway(request);
                    if (StringUtils.equals(ip, curIp)) {
                        return interceptor(request, response);
                    }
                    // 解析后的用户信息，传递给后续服务
                    String userId = map.get("userId").toString();
                    String username = map.get("username").toString();
                    ServerHttpRequest newRequest = request.mutate().header("userId", userId).header("username", username).build();
                    exchange = exchange.mutate().request(newRequest).build();
                } catch (Exception e) {
                    e.printStackTrace();
                    return interceptor(request, response);
                }
                // 放行
                return chain.filter(exchange);
            }

            private Mono<Void> interceptor(ServerHttpRequest request, ServerHttpResponse response) {
                response.setStatusCode(HttpStatus.SEE_OTHER);
                response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin?returnUrl=" + request.getURI());
                return response.setComplete();
            }
        };
    }

    /**
     * 指定字段顺序
     * 可以通过不同的字段分别读取：/toLogin,/login
     * 通过一个集合字段读取所有的路径
     *
     * @return
     */
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("authPaths");
    }

    /**
     * 指定读取字段的结果集类型
     * 默认通过map的方式，把配置读取到不同字段
     * 例如：/toLogin,/login
     * 由于只指定了一个字段，只能接收/toLogin
     *
     * @return
     */
    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    /**
     * 读取配置的内部类
     */
    @Data
    public static class PathConfig {
        private List<String> authPaths;
    }
}
