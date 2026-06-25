package com.sharesystem.user.service;

import com.sharesystem.common.dto.*;
import com.sharesystem.common.entity.User;

import java.util.List;

public interface UserService {

    Result<?> register(RegisterDTO dto);

    Result<?> login(LoginDTO dto);

    Result<String> getQQLoginUrl();

    Result<?> qqLoginCallback(String code, String state);

    User getUserById(Long id);

    UserDTO getUserDtoById(Long id);

    Result<?> changePassword(Long userId, String oldPassword, String newPassword);

    Result<?> updateProfile(User user);

    List<User> getAllUsers();

    List<User> searchUsers(String keyword);

    Result<?> updateUserStatus(Long userId, Integer status);

    Result<?> allocateStorage(Long userId, Long storageMax);

    int getUserCount();
}
