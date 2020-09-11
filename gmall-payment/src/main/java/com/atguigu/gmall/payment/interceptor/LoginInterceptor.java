package com.atguigu.gmall.payment.interceptor;

import com.atguigu.gmall.cart.bean.UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @Auther: cfy
 * @Date: 2020/09/05/13:47
 * @Description: TODO
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userIdStr = request.getHeader("userId"); // 20200907 23:52 获取为null 网关过滤器导致
        if (StringUtils.isNotBlank(userIdStr)) {
            UserInfo userInfo = new UserInfo();
            // 获取请求头信息
            Long userId = Long.valueOf(userIdStr);
            userInfo.setUserId(userId);
            // 传递给后续业务
            THREAD_LOCAL.set(userInfo);
        }
        // 同步跳转到成功界面，防止被拦截
//        else {
//            System.out.println("userIdStr为null");
//            response.sendRedirect("http://sso.gmall.com/toLogin?returnUrl=" + request.getRequestURL());
//        }
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
