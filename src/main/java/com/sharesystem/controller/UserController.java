package com.sharesystem.controller;

import com.sharesystem.dto.LoginDTO;
import com.sharesystem.dto.RegisterDTO;
import com.sharesystem.dto.Result;
import com.sharesystem.entity.User;
import com.sharesystem.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterDTO registerDTO) {
        log.info("用户注册请求: {}", registerDTO.getUsername());
        return userService.register(registerDTO);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<User> login(@RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        log.info("用户登录请求: {}", loginDTO.getUsername());
        return userService.login(loginDTO, request);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<?> logout(HttpSession session) {
        return userService.logout(session);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public Result<User> getCurrentUser(HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) {
            return Result.error(401, "未登录");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 获取QQ登录URL
     */
    @GetMapping("/qq/login")
    public Result<String> getQQLoginUrl() {
        return userService.getQQLoginUrl();
    }

    /**
     * QQ登录回调
     */
    @GetMapping("/qq/callback")
    public void qqCallback(@RequestParam("code") String code,
                           @RequestParam(value = "state", required = false) String state,
                           HttpServletRequest request, HttpServletResponse response) throws IOException {
        Result<User> result = userService.qqLoginCallback(code, state, request);
        if (result.isSuccess()) {
            response.sendRedirect("/ShareSystem/pages/main.html");
        } else {
            response.sendRedirect("/ShareSystem/pages/login.html?error=" +
                    java.net.URLEncoder.encode(result.getMessage(), "UTF-8"));
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/changePassword")
    public Result<?> changePassword(@RequestParam String oldPassword,
                                     @RequestParam String newPassword,
                                     HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        return userService.changePassword(user.getId(), oldPassword, newPassword);
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody User user, HttpSession session) {
        User current = userService.getCurrentUser(session);
        if (current == null) {
            return Result.error(401, "请先登录");
        }
        user.setId(current.getId());
        return userService.updateProfile(user);
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                        HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        try {
            return userService.uploadAvatar(user.getId(), file.getBytes(), file.getOriginalFilename());
        } catch (IOException e) {
            return Result.error("上传失败");
        }
    }

    /**
     * 获取用户存储空间信息
     */
    @GetMapping("/storage")
    public Result<?> getStorageInfo(HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        // 需要注入FileService，暂时返回用户基本信息
        return Result.success(new java.util.HashMap<String, Object>() {{
            put("used", user.getStorageUsed());
            put("max", user.getStorageMax());
        }});
    }
}
