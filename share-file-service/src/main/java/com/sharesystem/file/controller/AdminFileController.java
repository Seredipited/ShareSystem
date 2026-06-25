package com.sharesystem.file.controller;

import com.sharesystem.common.dto.R;
import com.sharesystem.common.dto.Result;
import com.sharesystem.common.dto.UserDTO;
import com.sharesystem.common.entity.FileItem;
import com.sharesystem.common.entity.OperationLog;
import com.sharesystem.common.entity.Share;
import com.sharesystem.common.util.JwtUtil;
import com.sharesystem.file.feign.UserFeignClient;
import com.sharesystem.file.mapper.FileItemMapper;
import com.sharesystem.file.mapper.OperationLogMapper;
import com.sharesystem.file.service.FileService;
import com.sharesystem.file.service.ShareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器 (文件/分享管理相关，运行在 file-service)
 */
@RestController
@RequestMapping("/api/admin")
public class AdminFileController {

    private static final Logger log = LoggerFactory.getLogger(AdminFileController.class);

    @Autowired
    private FileService fileService;
    @Autowired
    private ShareService shareService;
    @Autowired
    private FileItemMapper fileItemMapper;
    @Autowired
    private OperationLogMapper operationLogMapper;
    @Autowired
    private UserFeignClient userFeignClient;

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
            if (user != null && user.getRole() != null && user.getRole() == 1) return user;
        }
        return null;
    }

    @GetMapping("/files")
    public Result<List<FileItem>> getAllFiles(HttpServletRequest request) {
        if (checkAdmin(request) == null) return Result.error(403, "无权限");
        return Result.success(fileService.getAllFiles());
    }

    @GetMapping("/files/user/{userId}")
    public Result<List<FileItem>> getFilesByUser(@PathVariable Long userId, HttpServletRequest request) {
        if (checkAdmin(request) == null) return Result.error(403, "无权限");
        return Result.success(fileService.getFilesByUserId(userId));
    }

    @DeleteMapping("/files/{fileId}")
    public Result<?> deleteFile(@PathVariable Long fileId, HttpServletRequest request) {
        if (checkAdmin(request) == null) return Result.error(403, "无权限");
        return fileService.adminDeleteFile(fileId);
    }

    @GetMapping("/shares")
    public Result<?> getAllShares(HttpServletRequest request) {
        if (checkAdmin(request) == null) return Result.error(403, "无权限");
        List<Share> shares = shareService.getAllShares();
        return Result.success(shares);
    }

    @DeleteMapping("/shares/{shareId}")
    public Result<?> deleteShare(@PathVariable Long shareId, HttpServletRequest request) {
        if (checkAdmin(request) == null) return Result.error(403, "无权限");
        return shareService.adminDeleteShare(shareId);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(HttpServletRequest request) {
        if (checkAdmin(request) == null) return Result.error(403, "无权限");
        Map<String, Object> stats = new HashMap<>();

        // 用户统计
        try {
            R<Integer> countResult = userFeignClient.getUserCount();
            Integer userCount = countResult != null ? countResult.getData() : null;
            stats.put("totalUsers", userCount != null ? userCount : 0);
        } catch (Exception e) {
            log.warn("获取用户数失败", e);
            stats.put("totalUsers", 0);
        }

        // 文件统计
        List<FileItem> allFiles = fileItemMapper.selectAllFiles();
        long totalFiles = allFiles.size();
        long totalSize = 0;
        for (FileItem f : allFiles) {
            if (f.getFileSize() != null) totalSize += f.getFileSize();
        }
        stats.put("totalFiles", totalFiles);
        stats.put("totalSize", totalSize);

        // 分享统计
        stats.put("totalShares", shareService.getAllShares().size());

        return Result.success(stats);
    }

    @GetMapping("/logs")
    public Result<List<OperationLog>> getLogs(HttpServletRequest request) {
        if (checkAdmin(request) == null) return Result.error(403, "无权限");
        return Result.success(operationLogMapper.selectAll());
    }
}