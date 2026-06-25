package com.sharesystem.user.controller;

import com.sharesystem.common.dto.Result;
import com.sharesystem.common.dto.R;
import com.sharesystem.common.dto.UserDTO;
import com.sharesystem.common.entity.User;
import com.sharesystem.common.util.JwtUtil;
import com.sharesystem.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器 (用户管理相关，运行在 user-service)
 */
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);
    @Autowired
    private UserService userService;

    private UserDTO checkAdmin(HttpServletRequest request) {
        String token = request.getHeader("X-User-Token");
        if (token == null || token.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }
        if (token != null && !token.isEmpty()) {
            UserDTO user = JwtUtil.getUserFromToken(token);
            if (user != null && user.getRole() != null && user.getRole() == 1) {
                return user;
            }
        }
        return null;
    }

    @GetMapping("/users")
    public Result<?> getAllUsers(HttpServletRequest request) {
        if (checkAdmin(request) == null) return Result.error(403, "无权限");
        List<User> users = userService.getAllUsers();
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    @GetMapping("/users/search")
    public Result<?> searchUsers(@RequestParam String keyword, HttpServletRequest request) {
        if (checkAdmin(request) == null) return Result.error(403, "无权限");
        List<User> users = userService.searchUsers(keyword);
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    @GetMapping("/users/{userId}")
    public Result<?> getUserDetail(@PathVariable Long userId, HttpServletRequest request) {
        if (checkAdmin(request) == null) return Result.error(403, "无权限");
        User user = userService.getUserById(userId);
        if (user == null) return Result.error("用户不存在");
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/users/{userId}/status")
    public Result<?> updateUserStatus(@PathVariable Long userId,
                                       @RequestParam Integer status,
                                       HttpServletRequest request) {
        if (checkAdmin(request) == null) return Result.error(403, "无权限");
        return userService.updateUserStatus(userId, status);
    }

    @PutMapping("/users/{userId}/storage")
    public Result<?> allocateStorage(@PathVariable Long userId,
                                      @RequestParam Long storageMax,
                                      HttpServletRequest request) {
        if (checkAdmin(request) == null) return Result.error(403, "无权限");
        return userService.allocateStorage(userId, storageMax);
    }

    @DeleteMapping("/users/{userId}")
    public Result<?> deleteUser(@PathVariable Long userId, HttpServletRequest request) {
        UserDTO admin = checkAdmin(request);
        if (admin == null) return Result.error(403, "无权限");
        if (admin.getId().equals(userId)) return Result.error("不能删除自己");
        return userService.updateUserStatus(userId, 0);
    }

    @GetMapping("/stats")
    public Result<?> getStats(HttpServletRequest request) {
        if (checkAdmin(request) == null) return Result.error(403, "无权限");
        List<User> users = userService.getAllUsers();
        return Result.success(new java.util.HashMap<String, Object>() {{
            put("totalUsers", (long) users.size());
        }});
    }
}
