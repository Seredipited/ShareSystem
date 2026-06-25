package com.sharesystem.service;

import com.sharesystem.dto.LoginDTO;
import com.sharesystem.dto.RegisterDTO;
import com.sharesystem.dto.Result;
import com.sharesystem.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    Result<?> register(RegisterDTO registerDTO);

    /**
     * 用户登录
     */
    Result<User> login(LoginDTO loginDTO, HttpServletRequest request);

    /**
     * 用户退出
     */
    Result<?> logout(HttpSession session);

    /**
     * QQ登录 - 获取授权URL
     */
    Result<String> getQQLoginUrl();

    /**
     * QQ登录回调处理
     */
    Result<User> qqLoginCallback(String code, String state, HttpServletRequest request);

    /**
     * 获取当前登录用户
     */
    User getCurrentUser(HttpSession session);

    /**
     * 修改密码
     */
    Result<?> changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 更新用户信息
     */
    Result<?> updateProfile(User user);

    /**
     * 上传头像
     */
    Result<String> uploadAvatar(Long userId, byte[] avatarData, String originalName);

    /**
     * 根据ID查询用户
     */
    User getUserById(Long id);

    /**
     * 查询所有普通用户（管理员用）
     */
    List<User> getAllUsers();

    /**
     * 搜索用户
     */
    List<User> searchUsers(String keyword);

    /**
     * 更新用户状态（管理员用）
     */
    Result<?> updateUserStatus(Long userId, Integer status);

    /**
     * 给用户分配空间（管理员用）
     */
    Result<?> allocateStorage(Long userId, Long storageMax);
}
