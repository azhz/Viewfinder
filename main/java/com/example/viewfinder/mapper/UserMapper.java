package com.example.viewfinder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.viewfinder.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    @Select("SELECT COUNT(*) FROM user WHERE username = #{username}")
    int countByUsername(@Param("username") String username);
    
    @Select("SELECT COUNT(*) FROM user WHERE email = #{email}")
    int countByEmail(@Param("email") String email);
    
    @Select("SELECT COUNT(*) FROM user WHERE phone = #{phone}")
    int countByPhone(@Param("phone") String phone);
    
    @Select("SELECT * FROM user WHERE username = #{username} AND status = 1")
    User selectByUsername(@Param("username") String username);
}