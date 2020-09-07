package com.atguigu.gmall.cart.interceptor;

import com.atguigu.gmall.cart.bean.UserInfo;
import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

/**
 * @Auther: cfy
 * @Date: 2020/09/05/13:47
 * @Description: TODO
 */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class LoginInterceptor implements HandlerInterceptor {

    // 声明线程的局部变量
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 前置方法，再handler方法执行之前执行
     * 返回true-放行，false-被拦截
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取cookie中的token信息以及userKey信息
        String token = CookieUtils.getCookieValue(request, jwtProperties.getCookieName());
        String userKeyValue = CookieUtils.getCookieValue(request, jwtProperties.getUserKeyName()); // k user_key v uuid userKeyValue

        // userKeyValue为空生成一个放入cookie
        if (StringUtils.isBlank(userKeyValue)) {
            userKeyValue = UUID.randomUUID().toString();
            CookieUtils.setCookie(request, response, jwtProperties.getUserKeyName(), userKeyValue, jwtProperties.getExpire() * 24 * 3600);
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserKey(userKeyValue);

        // 如果token不为空解析出用户信息
        if (StringUtils.isNotBlank(token)) {
            try {
                // 解析jwt
                Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
                Long userId = Long.valueOf(map.get("userId").toString());
                userInfo.setUserId(userId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 怎么把登录信息传递给后续业务
        // 把信息放入线程的局部变量
        THREAD_LOCAL.set(userInfo);
        return true;
    }

    /**
     * 封装了一个获取线程局部变量值的静态方法
     *
     * @return
     */
    public static UserInfo getUserInfo() {
        return THREAD_LOCAL.get();
    }

    /**
     * 在视图渲染完成之后执行，经常在完成方法中释放资源
     *
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        // 调用删除方法，是必须选项。因为使用的是tomcat线程池，请求结束后，线程不会结束。
        // 如果不手动删除线程变量，可能会导致内存泄漏
        THREAD_LOCAL.remove();
    }
}
