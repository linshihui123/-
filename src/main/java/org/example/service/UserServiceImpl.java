package org.example.service;

import org.example.model.UserNode;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Optional;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 新增用户：直接保存密码（不加密）
     */
    @Override
    public UserNode addUser(UserNode user) {
        // 参数校验：密码不能为空
        Assert.hasText(user.getPassword(), "密码不能为空");
        // 检查用户名是否已存在
        Optional<UserNode> existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser.isPresent()) {
            throw new RuntimeException("用户名已存在");
        }
        // 直接保存用户信息（不加密密码）
        return userRepository.save(user);
    }

    /**
     * 根据ID查询用户
     */
    @Override
    public UserNode getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    /**
     * 根据用户名查询用户
     */
    @Override
    public UserNode getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * 更新密码：直接更新
     */
    @Override
    public boolean updatePassword(Long userId, String newPassword) {
        // 参数校验
        Assert.notNull(userId, "用户ID不能为空");
        Assert.hasText(newPassword, "新密码不能为空");
        // 查询用户是否存在
        Optional<UserNode> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            return false;
        }
        // 直接更新密码（不加密）
        UserNode user = userOptional.get();
        user.setPassword(newPassword);
        userRepository.save(user);
        return true;
    }

    /**
     * 用户登录
     */
    @Override
    public UserNode login(String username, String password) {
        // 查询用户
        Optional<UserNode> userOptional = userRepository.findByUsername(username);
        
        // 直接比较密码（不使用加密验证）
        if (userOptional.isPresent()) {
            UserNode user = userOptional.get();
            if (password.equals(user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    /**
     * 检查用户名是否存在
     */
    @Override
    public boolean checkUsernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }
}