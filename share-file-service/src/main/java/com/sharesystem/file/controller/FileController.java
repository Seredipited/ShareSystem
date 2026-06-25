package com.sharesystem.file.controller;

import com.sharesystem.common.dto.Result;
import com.sharesystem.common.dto.UserDTO;
import com.sharesystem.common.entity.FileItem;
import com.sharesystem.common.util.FileUtil;
import com.sharesystem.common.util.JwtUtil;
import com.sharesystem.file.config.CurrentUser;
import com.sharesystem.file.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;

    private Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("X-User-Token");
        if (token != null) {
            UserDTO user = JwtUtil.getUserFromToken(token);
            return user != null ? user.getId() : null;
        }
        return null;
    }

    @GetMapping("/list")
    public Result<List<FileItem>> getFileList(@RequestParam(defaultValue = "0") Long parentId,
                                                HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        return fileService.getFileList(userId, parentId);
    }

    @PostMapping("/folder")
    public Result<FileItem> createFolder(@RequestParam String folderName,
                                          @RequestParam(defaultValue = "0") Long parentId,
                                          HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        return fileService.createFolder(userId, parentId, folderName);
    }

    @PostMapping("/upload")
    public Result<FileItem> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam(defaultValue = "0") Long parentId,
                                        @RequestParam(required = false) String fileMd5,
                                        HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        try {
            return fileService.uploadFile(userId, parentId,
                    file.getOriginalFilename(), file.getBytes(), fileMd5);
        } catch (IOException e) { return Result.error("上传失败"); }
    }

    @PostMapping("/checkInstant")
    public Result<FileItem> checkInstantUpload(@RequestParam String fileName,
                                                @RequestParam String fileMd5,
                                                @RequestParam(defaultValue = "0") Long parentId,
                                                HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        return fileService.checkInstantUpload(userId, parentId, fileName, fileMd5);
    }

    @PostMapping("/chunk")
    public Result<?> uploadChunk(@RequestParam("file") MultipartFile chunk,
                                  @RequestParam String fileMd5,
                                  @RequestParam int chunkIndex,
                                  @RequestParam int totalChunks,
                                  @RequestParam String fileName,
                                  HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        try {
            return fileService.uploadChunk(userId, fileMd5,
                    chunkIndex, totalChunks, fileName, chunk.getBytes());
        } catch (IOException e) { return Result.error("分片上传失败"); }
    }

    @GetMapping("/chunkProgress")
    public Result<List<Integer>> checkChunkProgress(@RequestParam String fileMd5) {
        return fileService.checkChunkProgress(fileMd5);
    }

    @PostMapping("/merge")
    public Result<FileItem> mergeChunks(@RequestParam String fileMd5,
                                         @RequestParam String fileName,
                                         @RequestParam(defaultValue = "0") Long parentId,
                                         HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        return fileService.mergeChunks(userId, parentId, fileMd5, fileName);
    }

    @PutMapping("/rename/{fileId}")
    public Result<?> renameFile(@PathVariable Long fileId, @RequestParam String newName) {
        return fileService.renameFile(fileId, newName);
    }

    @PutMapping("/move/{fileId}")
    public Result<?> moveFile(@PathVariable Long fileId, @RequestParam Long targetParentId) {
        return fileService.moveFile(fileId, targetParentId);
    }

    @DeleteMapping("/{fileId}")
    public Result<?> deleteFile(@PathVariable Long fileId) {
        return fileService.deleteFile(fileId);
    }

    @PostMapping("/batchDelete")
    public Result<?> batchDelete(@RequestBody List<Long> fileIds) {
        return fileService.batchDeleteFiles(fileIds);
    }

    @PutMapping("/restore/{fileId}")
    public Result<?> restoreFile(@PathVariable Long fileId) {
        return fileService.restoreFile(fileId);
    }

    @DeleteMapping("/permanent/{fileId}")
    public Result<?> permanentDelete(@PathVariable Long fileId) {
        return fileService.permanentDelete(fileId);
    }

    @GetMapping("/recycle")
    public Result<List<FileItem>> getRecycleBin(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        return fileService.getRecycleBinList(userId);
    }

    @DeleteMapping("/recycle/clear")
    public Result<?> clearRecycleBin(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        return fileService.clearRecycleBin(userId);
    }

    @GetMapping("/download/{fileId}")
    public void downloadFile(@PathVariable Long fileId, HttpServletResponse response) {
        fileService.downloadFile(fileId, response);
    }

    @GetMapping("/shared")
    public Result<List<FileItem>> getSharedFiles(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        return fileService.getSharedFiles(userId);
    }

    @GetMapping("/search")
    public Result<List<FileItem>> searchFiles(@RequestParam String keyword,
                                                HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        return fileService.searchFiles(userId, keyword);
    }

    @GetMapping("/preview/{fileId}")
    public void previewFile(@PathVariable Long fileId,
                             HttpServletRequest request, HttpServletResponse response) {
        try {
            FileItem file = fileService.getFileById(fileId);
            if (file == null || file.isFolder()) { response.sendError(404); return; }
            File physicalFile = new File(file.getFilePath());
            if (!physicalFile.exists()) { response.sendError(404); return; }

            String ext = file.getFileType() != null ? file.getFileType() : "";
            String encodedName = URLEncoder.encode(file.getFileName(), "UTF-8");

            // Always expose file name for the frontend
            response.setHeader("X-File-Name", encodedName);

            if (ext.matches("(?i)jpg|jpeg|png|gif|bmp|svg|webp|ico")) {
                response.setContentType(file.getMimeType());
                Files.copy(Paths.get(file.getFilePath()), response.getOutputStream());
            } else if ("pdf".equalsIgnoreCase(ext)) {
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", "inline; filename=\"" + encodedName + "\"");
                Files.copy(Paths.get(file.getFilePath()), response.getOutputStream());
            } else if (ext.matches("(?i)txt|html|htm|css|js|json|xml|md|sql")) {
                response.setContentType("text/plain;charset=UTF-8");
                String content = new String(Files.readAllBytes(Paths.get(file.getFilePath())), "UTF-8");
                response.getWriter().write(content);
            } else if (ext.matches("(?i)mp3|wav|ogg|aac|flac|mp4|webm|avi|mov|mkv")) {
                response.setContentType(file.getMimeType());
                response.setHeader("Accept-Ranges", "bytes");
                response.setHeader("Content-Disposition", "inline; filename=\"" + encodedName + "\"");
                Files.copy(Paths.get(file.getFilePath()), response.getOutputStream());
            } else if (ext.matches("(?i)doc|docx|xls|xlsx|ppt|pptx")) {
                // Return JSON so the frontend can show appropriate UI
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("X-Preview-Type", "unsupported");
                response.getWriter().write("{\"code\":200,\"previewType\":\"unsupported\",\"message\":\"Office文件不支持在线预览，建议下载后查看\"}");
            } else {
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("X-Preview-Type", "unsupported");
                response.getWriter().write("{\"code\":200,\"previewType\":\"unsupported\",\"message\":\"该文件格式不支持在线预览\"}");
            }
        } catch (IOException e) { log.error("预览失败", e); }
    }

    @GetMapping("/detail/{fileId}")
    public Result<FileItem> getFileDetail(@PathVariable Long fileId) {
        FileItem file = fileService.getFileById(fileId);
        if (file == null) return Result.error("文件不存在");
        return Result.success(file);
    }
}
