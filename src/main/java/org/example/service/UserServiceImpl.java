package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.model.User;
import org.example.repository.UserMapper;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.util.Assert;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 新增用户：直接保存密码（不加密）
     */
    @Override
    public User addUser(User user) {
        // 参数校验：密码不能为空
        Assert.hasText(user.getPassword(), "密码不能为空");
        // 检查用户名是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername()); // 使用数据库实际字段名
        User existingUser = this.getBaseMapper().selectOne(queryWrapper);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        // 直接保存用户信息（不加密密码）
        this.save(user);
        return user;
    }

    /**
     * 根据ID查询用户
     */
    @Override
    public User getUserById(Integer userId) {
        return this.getById(userId);
    }

    /**
     * 更新密码：直接更新
     */
    @Override
    public boolean updatePassword(Integer userId, String newPassword) {
        // 参数校验
        Assert.notNull(userId, "用户ID不能为空");
        Assert.hasText(newPassword, "新密码不能为空");
        // 查询用户是否存在
        User user = this.getById(userId);
        if (user == null) {
            return false;
        }
        // 直接更新密码（不加密）
        user.setPassword(newPassword);
        return this.updateById(user);
    }

    @Override
    public User login(String username, String password) {
        // 查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username); // 使用数据库实际字段名
        User user = this.getBaseMapper().selectOne(queryWrapper);
        
        // 直接比较密码（不使用加密验证）
        if (user != null && password.equals(user.getPassword())) {
            return user;
        }
        return null;
    }
}