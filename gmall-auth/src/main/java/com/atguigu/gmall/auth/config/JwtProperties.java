package com.atguigu.gmall.auth.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @Auther: cfy
 * @Date: 2020/09/04/20:20
 * @Description: TODO
 */
@Data
@Slf4j
@ConfigurationProperties(prefix = "auth.jwt")
@Component
public class JwtProperties {
    private String pubKeyPath;
    private String priKeyPath;
    private String secret;
    private String cookieName;
    private Integer expire;
    private String unick;

    private PublicKey publicKey;
    private PrivateKey privateKey;

    @PostConstruct
    public void init() {
        try {
            File pubFile = new File(pubKeyPath);
            File priFile = new File(priKeyPath);
            if (!pubFile.exists() || !priFile.exists()) {
                RsaUtils.generateKey(pubKeyPath, priKeyPath, secret);
            }
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
            this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("生成公钥和私钥失败，失败原因:" + e.getMessage());
        }
    }
}
