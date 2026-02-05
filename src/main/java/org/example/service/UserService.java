package org.example.service;

import org.example.model.UserNode;

/**
 * 用户Service接口
 * 封装业务逻辑
 */
public interface UserService {

    /**
     * 新增用户
     * @param user 用户信息
     * @return 新增后的用户
     */
    UserNode addUser(UserNode user);

    /**
     * 根据ID查询用户
     * @param userId 用户ID
     * @return 用户信息
     */
    UserNode getUserById(Long userId);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    UserNode getUserByUsername(String username);

    /**
     * 更新用户密码
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 是否更新成功
     */
    boolean updatePassword(Long userId, String newPassword);

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录成功的用户
     */
    UserNode login(String username, String password);

    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean checkUsernameExists(String username);
}