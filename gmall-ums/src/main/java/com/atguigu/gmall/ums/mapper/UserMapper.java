package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author cfy
 * @email cfy@qq.com
 * @date 2020-08-21 16:56:28
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
