package org.example.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

}
