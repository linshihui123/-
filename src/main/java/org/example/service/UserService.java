package org.example.service;

import org.example.model.User;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户Service接口
 * 封装业务逻辑
 */
public interface UserService extends IService<User> {

    /**
     * 新增用户（密码加密）
     * @param user 用户信息（密码为明文）
     * @return 新增后的用户
     */
    User addUser(User user);

    /**
     * 根据ID查询用户
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Integer userId);

    /**
     * 更新用户密码
     * @param userId 用户ID
     * @param newPassword 新密码（明文）
     * @return 是否更新成功
     */
    boolean updatePassword(Integer userId, String newPassword);
    /**
     * 登录用户
     */
    User login(String username, String password);
}