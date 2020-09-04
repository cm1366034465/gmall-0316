package com.atguigu.gmall.ums.service.impl;

import org.apache.catalina.User;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        // 1、用户名；2、手机；3、邮箱
        switch (type) {
            case 1:
                wrapper.eq("username", data);
                break;
            case 2:
                wrapper.eq("phone", data);
                break;
            case 3:
                wrapper.eq("email", data);
                break;
            default:
                return null;
        }


        return this.count(wrapper) == 0;
    }

    @Override
    public void register(UserEntity userEntity, String code) {
        // 校验短信验证码 todo

        // 生成盐
        String salt = StringUtils.replace(UUID.randomUUID().toString(), "-", "");
        userEntity.setSalt(salt);

        // 对密码加密
        userEntity.setPassword(DigestUtils.md5Hex(salt + DigestUtils.md5Hex(userEntity.getPassword())));

        // 设置创建时间等
        userEntity.setCreateTime(new Date());
        userEntity.setLevelId(1l);
        userEntity.setStatus(1);
        userEntity.setIntegration(0);
        userEntity.setGrowth(0);
        userEntity.setNickname(userEntity.getUsername() + "=name");

        // 添加到数据库
        boolean b = this.save(userEntity);

        // 删除短信验证码 todo
    }

    @Override
    public UserEntity queryUser(String loginName, String password) {
        // 1.根据登录名查询用户信息
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", loginName)
                .or().eq("phone", loginName)
                .or().eq("email", loginName);
        UserEntity userEntity = this.getOne(queryWrapper);

        // 2.判空
        if (userEntity == null) {
            // throw new  IllegalArgumentException("用户名或密码不合法");
            return userEntity;
        }

        // 3.获取用户信息中的盐，并对用户输入的明文密码加盐加密
        String newPassword = DigestUtils.md5Hex(userEntity.getSalt() + DigestUtils.md5Hex(password));

        // 4.比较数据库中保存的密码和用户输入的密码(加密后的)
        if (!StringUtils.equals(newPassword, userEntity.getPassword())) {
            // throw new  IllegalArgumentException("用户名或密码不合法");
            return null;
        }
        return userEntity;
    }

}