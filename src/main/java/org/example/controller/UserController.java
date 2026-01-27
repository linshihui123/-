package org.example.controller;

import org.example.model.User;
import org.example.response.Result;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 用户控制器
 * 提供用户相关HTTP接口
 */
@RestController
@RequestMapping("/users")  // 修改路径，去掉/api前缀，因为代理会处理
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 新增用户
     * POST /users
     * @param user 用户信息（JSON格式）
     * @return 新增后的用户
     */
    @PostMapping
    public Result<User> addUser(@Validated @RequestBody User user) {
        try {
            User newUser = userService.addUser(user);
            return Result.success(newUser);
        } catch (Exception e) {
            return Result.error("用户注册失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID查询用户
     * GET /users/{userId}
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/{userId}")
    public Result<User> getUserById(@PathVariable Integer userId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 更新用户密码
     * PUT /users/{userId}/password
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 是否成功
     */
    @PutMapping("/{userId}/password")
    public Result<Boolean> updatePassword(
            @PathVariable Integer userId,
            @RequestParam String newPassword) {
        boolean success = userService.updatePassword(userId, newPassword);
        return success ? Result.success(true) : Result.error("更新密码失败");
    }
    
    /**
     * 用户登录
     * POST /users/login
     * @param username 用户名
     * @param password 密码
     * @return 登录成功后的用户信息
     */
    @PostMapping("/login")
    public Result<User> login(
            @RequestParam String username,
            @RequestParam String password) {
        User user = userService.login(username, password);
        if (user == null) {
            return Result.error("用户名或密码错误");
        }
        return Result.success(user);
    }
    
    /**
     * 检查用户名是否存在
     * GET /users/check-username/{username}
     * @param username 用户名
     * @return 用户名是否存在
     */
    @GetMapping("/check-username/{username}")
    public Result<Boolean> checkUsernameExists(@PathVariable String username) {
        // 添加输入验证
        if (!StringUtils.hasText(username)) {
            return Result.success(false);
        }
        
        try {
            User existingUser = userService.getBaseMapper().selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                    .eq("username", username)
            );
            return Result.success(existingUser != null);
        } catch (Exception e) {
            // 记录错误日志并返回false而不是抛出500错误
            e.printStackTrace(); // 在生产环境中应该使用合适的日志记录器
            return Result.success(false); // 即使出错也返回false，而不是500错误
        }
    }
}