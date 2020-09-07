package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
    private static final String pubKeyPath = "D:\\developer_tools\\workspaceteacher0316\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\developer_tools\\workspaceteacher0316\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "123456!@#$%^qwer");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE1OTkyMjAyMjh9.MhXdW79FlGXxuWWWWbwcrPhom-Aajc_ClWAKTQP_06Ui1IQ7Lpx-wzD0NboGMvQOdXs48kOBFUCX8uf-X7gFL0s3Zn5Kipp0xetbyN2hED2XRMudgTa4AvLvvildF7IeVYY7iaH3BtUY1jAGvlpVX2KCAu6mX-eIMg7HpG4YEEFtBUKZs7MxtUyMz1Gpy7rkidOlk1lq2qCpBOWb1fID0jvrlDYAhLa5r23fGRgeEoYKq8M8QGD_YEvnb9k3b2EpRQ0Vrd3TnlFpTPTlLQJFeF0OVoqPdPb9LG1WfQxdeIp0EtLkZaSfj17lBIch89yO0-lIOKoa9c4r4lszvtj0ZA";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}