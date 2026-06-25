package com.sharesystem.controller;

import com.sharesystem.dto.Result;
import com.sharesystem.entity.FileItem;
import com.sharesystem.entity.Share;
import com.sharesystem.entity.User;
import com.sharesystem.service.FileService;
import com.sharesystem.service.ShareService;
import com.sharesystem.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 管理员控制器
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Autowired
    private ShareService shareService;

    /**
     * 验证管理员身份
     */
    private User checkAdmin(HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null || !user.isAdmin()) {
            return null;
        }
        return user;
    }

    // ==================== 用户管理 ====================

    /**
     * 获取所有用户
     */
    @GetMapping("/users")
    public Result<List<User>> getAllUsers(HttpSession session) {
        if (checkAdmin(session) == null) return Result.error(403, "无权限");
        List<User> users = userService.getAllUsers();
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    /**
     * 搜索用户
     */
    @GetMapping("/users/search")
    public Result<List<User>> searchUsers(@RequestParam String keyword, HttpSession session) {
        if (checkAdmin(session) == null) return Result.error(403, "无权限");
        List<User> users = userService.searchUsers(keyword);
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/users/{userId}")
    public Result<User> getUserDetail(@PathVariable Long userId, HttpSession session) {
        if (checkAdmin(session) == null) return Result.error(403, "无权限");
        User user = userService.getUserById(userId);
        if (user == null) return Result.error("用户不存在");
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 更新用户状态（启用/禁用）
     */
    @PutMapping("/users/{userId}/status")
    public Result<?> updateUserStatus(@PathVariable Long userId,
                                       @RequestParam Integer status,
                                       HttpSession session) {
        if (checkAdmin(session) == null) return Result.error(403, "无权限");
        return userService.updateUserStatus(userId, status);
    }

    /**
     * 给用户分配存储空间
     */
    @PutMapping("/users/{userId}/storage")
    public Result<?> allocateStorage(@PathVariable Long userId,
                                      @RequestParam Long storageMax,
                                      HttpSession session) {
        if (checkAdmin(session) == null) return Result.error(403, "无权限");
        return userService.allocateStorage(userId, storageMax);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{userId}")
    public Result<?> deleteUser(@PathVariable Long userId, HttpSession session) {
        User admin = checkAdmin(session);
        if (admin == null) return Result.error(403, "无权限");
        if (admin.getId().equals(userId)) {
            return Result.error("不能删除自己");
        }
        return userService.updateUserStatus(userId, 0);
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/stats")
    public Result<?> getStats(HttpSession session) {
        if (checkAdmin(session) == null) return Result.error(403, "无权限");
        List<User> users = userService.getAllUsers();
        List<FileItem> files = fileService.getAllFiles();
        long totalUsers = users.size();
        long totalFiles = files.size();
        long totalSize = files.stream().filter(f -> !f.isFolder()).mapToLong(FileItem::getFileSize).sum();
        return Result.success(new java.util.HashMap<String, Object>() {{
            put("totalUsers", totalUsers);
            put("totalFiles", totalFiles);
            put("totalSize", totalSize);
        }});
    }

    // ==================== 文件管理 ====================

    /**
     * 获取所有文件
     */
    @GetMapping("/files")
    public Result<List<FileItem>> getAllFiles(HttpSession session) {
        if (checkAdmin(session) == null) return Result.error(403, "无权限");
        List<FileItem> files = fileService.getAllFiles();
        return Result.success(files);
    }

    /**
     * 获取指定用户的文件
     */
    @GetMapping("/files/user/{userId}")
    public Result<List<FileItem>> getFilesByUser(@PathVariable Long userId, HttpSession session) {
        if (checkAdmin(session) == null) return Result.error(403, "无权限");
        List<FileItem> files = fileService.getFilesByUserId(userId);
        return Result.success(files);
    }

    /**
     * 删除任意文件
     */
    @DeleteMapping("/files/{fileId}")
    public Result<?> deleteFile(@PathVariable Long fileId, HttpSession session) {
        if (checkAdmin(session) == null) return Result.error(403, "无权限");
        return fileService.adminDeleteFile(fileId);
    }

    // ==================== 分享管理 ====================

    /**
     * 获取所有分享
     */
    @GetMapping("/shares")
    public Result<?> getAllShares(HttpSession session) {
        if (checkAdmin(session) == null) return Result.error(403, "无权限");
        List<FileItem> sharedFiles = fileService.getAllSharedFiles();
        return Result.success(sharedFiles);
    }

    /**
     * 删除任意分享
     */
    @DeleteMapping("/shares/{shareId}")
    public Result<?> deleteShare(@PathVariable Long shareId, HttpSession session) {
        if (checkAdmin(session) == null) return Result.error(403, "无权限");
        return shareService.adminDeleteShare(shareId);
    }
}
