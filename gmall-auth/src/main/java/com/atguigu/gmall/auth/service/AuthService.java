package com.atguigu.gmall.auth.service;

import com.alibaba.nacos.client.utils.IPUtil;
import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @Auther: cfy
 * @Date: 2020/09/04/20:00
 * @Description: TODO
 */
@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private JwtProperties jwtProperties;

    public void accredit(String loginName, String password, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 校验用户名和密码是否正确
        // 1.远程调用接口
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUser(loginName, password);
        UserEntity userEntity = userEntityResponseVo.getData();

        // 2.判断用户是否为空
        if (userEntity == null) {
            // 自定义异常
            throw new UserException("用户名或者密码有误!");
        }

        // 3.生成jwt
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userEntity.getId());
        map.put("username", userEntity.getUsername());
        map.put("ip", IpUtil.getIpAddressAtService(request));

        String token = JwtUtils.generateToken(map, this.jwtProperties.getPrivateKey(), jwtProperties.getExpire());

        // 4.jwt类型的token放入cookie中
        CookieUtils.setCookie(request, response, this.jwtProperties.getCookieName(), token, this.jwtProperties.getExpire() * 60);
        // 增加用户昵称
        CookieUtils.setCookie(request, response, this.jwtProperties.getUnick(), userEntity.getNickname(), this.jwtProperties.getExpire() * 60);
    }
}
