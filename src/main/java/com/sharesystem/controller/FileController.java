package com.sharesystem.controller;

import com.sharesystem.dto.Result;
import com.sharesystem.entity.FileItem;
import com.sharesystem.entity.User;
import com.sharesystem.service.FileService;
import com.sharesystem.service.UserService;
import com.sharesystem.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 文件控制器
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;

    @Autowired
    private UserService userService;

    /**
     * 获取文件列表
     */
    @GetMapping("/list")
    public Result<List<FileItem>> getFileList(@RequestParam(defaultValue = "0") Long parentId,
                                               HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        return fileService.getFileList(user.getId(), parentId);
    }

    /**
     * 创建文件夹
     */
    @PostMapping("/folder")
    public Result<FileItem> createFolder(@RequestParam String folderName,
                                          @RequestParam(defaultValue = "0") Long parentId,
                                          HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        return fileService.createFolder(user.getId(), parentId, folderName);
    }

    /**
     * 文件上传（支持秒传）
     */
    @PostMapping("/upload")
    public Result<FileItem> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam(defaultValue = "0") Long parentId,
                                        @RequestParam(required = false) String fileMd5,
                                        HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        try {
            return fileService.uploadFile(user.getId(), parentId,
                    file.getOriginalFilename(), file.getBytes(), fileMd5);
        } catch (IOException e) {
            return Result.error("上传失败");
        }
    }

    /**
     * 秒传检测
     */
    @PostMapping("/checkInstant")
    public Result<FileItem> checkInstantUpload(@RequestParam String fileName,
                                                @RequestParam String fileMd5,
                                                @RequestParam(defaultValue = "0") Long parentId,
                                                HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        return fileService.checkInstantUpload(user.getId(), parentId, fileName, fileMd5);
    }

    /**
     * 分片上传
     */
    @PostMapping("/chunk")
    public Result<?> uploadChunk(@RequestParam("file") MultipartFile chunk,
                                  @RequestParam String fileMd5,
                                  @RequestParam int chunkIndex,
                                  @RequestParam int totalChunks,
                                  @RequestParam String fileName,
                                  HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        try {
            return fileService.uploadChunk(user.getId(), fileMd5,
                    chunkIndex, totalChunks, fileName, chunk.getBytes());
        } catch (IOException e) {
            return Result.error("分片上传失败");
        }
    }

    /**
     * 检查分片上传进度（断点续传）
     */
    @GetMapping("/chunkProgress")
    public Result<List<Integer>> checkChunkProgress(@RequestParam String fileMd5) {
        return fileService.checkChunkProgress(fileMd5);
    }

    /**
     * 合并分片
     */
    @PostMapping("/merge")
    public Result<FileItem> mergeChunks(@RequestParam String fileMd5,
                                         @RequestParam String fileName,
                                         @RequestParam(defaultValue = "0") Long parentId,
                                         HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        return fileService.mergeChunks(user.getId(), parentId, fileMd5, fileName);
    }

    /**
     * 文件重命名
     */
    @PutMapping("/rename/{fileId}")
    public Result<?> renameFile(@PathVariable Long fileId,
                                 @RequestParam String newName) {
        return fileService.renameFile(fileId, newName);
    }

    /**
     * 文件移动
     */
    @PutMapping("/move/{fileId}")
    public Result<?> moveFile(@PathVariable Long fileId,
                               @RequestParam Long targetParentId) {
        return fileService.moveFile(fileId, targetParentId);
    }

    /**
     * 删除文件（移入回收站）
     */
    @DeleteMapping("/{fileId}")
    public Result<?> deleteFile(@PathVariable Long fileId) {
        return fileService.deleteFile(fileId);
    }

    /**
     * 批量删除
     */
    @PostMapping("/batchDelete")
    public Result<?> batchDeleteFiles(@RequestBody List<Long> fileIds) {
        return fileService.batchDeleteFiles(fileIds);
    }

    /**
     * 还原文件
     */
    @PutMapping("/restore/{fileId}")
    public Result<?> restoreFile(@PathVariable Long fileId) {
        return fileService.restoreFile(fileId);
    }

    /**
     * 彻底删除
     */
    @DeleteMapping("/permanent/{fileId}")
    public Result<?> permanentDelete(@PathVariable Long fileId) {
        return fileService.permanentDelete(fileId);
    }

    /**
     * 获取回收站列表
     */
    @GetMapping("/recycle")
    public Result<List<FileItem>> getRecycleBin(HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        return fileService.getRecycleBinList(user.getId());
    }

    /**
     * 清空回收站
     */
    @DeleteMapping("/recycle/clear")
    public Result<?> clearRecycleBin(HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        return fileService.clearRecycleBin(user.getId());
    }

    /**
     * 文件下载（支持断点续传）
     */
    @GetMapping("/download/{fileId}")
    public void downloadFile(@PathVariable Long fileId, HttpServletResponse response) {
        fileService.downloadFile(fileId, response);
    }

    /**
     * 获取分享文件列表
     */
    @GetMapping("/shared")
    public Result<List<FileItem>> getSharedFiles(HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        return fileService.getSharedFiles(user.getId());
    }

    /**
     * 搜索文件
     */
    @GetMapping("/search")
    public Result<List<FileItem>> searchFiles(@RequestParam String keyword,
                                               HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        return fileService.searchFiles(user.getId(), keyword);
    }

    /**
     * 文件在线预览
     */
    @GetMapping("/preview/{fileId}")
    public void previewFile(@PathVariable Long fileId,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        try {
            FileItem file = fileService.getFileById(fileId);
            if (file == null || file.isFolder()) {
                response.sendError(404, "文件不存在");
                return;
            }

            File physicalFile = new File(file.getFilePath());
            if (!physicalFile.exists()) {
                response.sendError(404, "物理文件不存在");
                return;
            }

            String ext = file.getFileType();
            String mimeType = file.getMimeType();

            if (ext == null) ext = "";

            // 图片直接返回
            if (ext.matches("(?i)jpg|jpeg|png|gif|bmp|svg|webp|ico")) {
                response.setContentType(mimeType);
                Files.copy(Paths.get(file.getFilePath()), response.getOutputStream());
                return;
            }

            // PDF使用inline展示
            if ("pdf".equalsIgnoreCase(ext)) {
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", "inline; filename=\"" +
                        URLEncoder.encode(file.getFileName(), "UTF-8") + "\"");
                Files.copy(Paths.get(file.getFilePath()), response.getOutputStream());
                return;
            }

            // 文本文件
            if (ext.matches("(?i)txt|html|htm|css|js|json|xml|java|py|c|cpp|sql|md")) {
                response.setContentType("text/plain;charset=UTF-8");
                String content = new String(Files.readAllBytes(Paths.get(file.getFilePath())), "UTF-8");
                response.getWriter().write(content);
                return;
            }

            // 音视频
            if (ext.matches("(?i)mp3|wav|ogg|aac|flac|mp4|webm|avi|mov|mkv")) {
                response.setContentType(mimeType);
                response.setHeader("Accept-Ranges", "bytes");
                response.setHeader("Content-Disposition", "inline; filename=\"" +
                        URLEncoder.encode(file.getFileName(), "UTF-8") + "\"");
                Files.copy(Paths.get(file.getFilePath()), response.getOutputStream());
                return;
            }

            // Office文件 - 提供HTML页面进行前端预览
            if (ext.matches("(?i)doc|docx|xls|xlsx|ppt|pptx")) {
                response.sendRedirect("/ShareSystem/pages/preview.html?fileId=" + fileId +
                        "&fileName=" + URLEncoder.encode(file.getFileName(), "UTF-8") +
                        "&fileType=" + ext);
                return;
            }

            // 不支持的格式
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":500,\"message\":\"该文件格式不支持在线预览\"}");

        } catch (IOException e) {
            log.error("预览文件失败", e);
        }
    }

    /**
     * 获取文件详情
     */
    @GetMapping("/detail/{fileId}")
    public Result<FileItem> getFileDetail(@PathVariable Long fileId) {
        FileItem file = fileService.getFileById(fileId);
        if (file == null) {
            return Result.error("文件不存在");
        }
        return Result.success(file);
    }
}
