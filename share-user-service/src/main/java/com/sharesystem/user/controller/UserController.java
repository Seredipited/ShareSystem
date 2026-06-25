package com.sharesystem.user.controller;

import com.sharesystem.common.dto.*;
import com.sharesystem.common.entity.User;
import com.sharesystem.common.util.JwtUtil;
import com.sharesystem.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /** 从请求头获取当前用户 */
    private UserDTO getCurrentUser(HttpServletRequest request) {
        String token = request.getHeader("X-User-Token");
        if (token != null) {
            return JwtUtil.getUserFromToken(token);
        }
        return null;
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterDTO dto) {
        return userService.register(dto);
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginDTO dto) {
        return userService.login(dto);
    }

    @GetMapping("/current")
    public Result<?> currentUser(HttpServletRequest request) {
        UserDTO user = getCurrentUser(request);
        if (user == null) return Result.error(401, "未登录");
        return Result.success(user);
    }

    @GetMapping("/qq/login")
    public Result<String> getQQLoginUrl() {
        return userService.getQQLoginUrl();
    }

    @GetMapping("/qq/callback")
    public void qqCallback(@RequestParam("code") String code,
                           @RequestParam(value = "state", required = false) String state,
                           HttpServletResponse response) throws IOException {
        Result<?> result = userService.qqLoginCallback(code, state);
        if (result.isSuccess()) {
            // 从data中提取token
            response.sendRedirect("/pages/main.html");
        } else {
            response.sendRedirect("/pages/login.html?error=" +
                    java.net.URLEncoder.encode(result.getMessage(), "UTF-8"));
        }
    }

    @PostMapping("/changePassword")
    public Result<?> changePassword(@RequestParam String oldPassword,
                                     @RequestParam String newPassword,
                                     HttpServletRequest request) {
        UserDTO user = getCurrentUser(request);
        if (user == null) return Result.error(401, "请先登录");
        return userService.changePassword(user.getId(), oldPassword, newPassword);
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody User user, HttpServletRequest request) {
        UserDTO current = getCurrentUser(request);
        if (current == null) return Result.error(401, "请先登录");
        user.setId(current.getId());
        return userService.updateProfile(user);
    }

    @GetMapping("/storage")
    public Result<?> getStorageInfo(HttpServletRequest request) {
        UserDTO user = getCurrentUser(request);
        if (user == null) return Result.error(401, "请先登录");
        return Result.success(new java.util.HashMap<String, Object>() {{
            put("used", user.getStorageUsed());
            put("max", user.getStorageMax());
        }});
    }

    // ==================== 内部 Feign 调用接口 ====================

    @GetMapping("/internal/{userId}")
    public R<UserDTO> getUserById(@PathVariable Long userId) {
        UserDTO dto = userService.getUserDtoById(userId);
        return dto != null ? R.ok(dto) : R.fail("用户不存在");
    }

    @GetMapping("/internal/count")
    public R<Integer> getUserCount() {
        return R.ok(userService.getUserCount());
    }
}
