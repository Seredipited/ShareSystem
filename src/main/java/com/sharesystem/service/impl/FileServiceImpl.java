package com.sharesystem.service.impl;

import com.sharesystem.dto.Result;
import com.sharesystem.entity.FileChunk;
import com.sharesystem.entity.FileItem;
import com.sharesystem.entity.User;
import com.sharesystem.mapper.FileChunkMapper;
import com.sharesystem.mapper.FileItemMapper;
import com.sharesystem.mapper.UserMapper;
import com.sharesystem.service.FileService;
import com.sharesystem.util.FileUtil;
import com.sharesystem.util.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文件服务实现
 */
@Service
@Transactional
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    @Autowired
    private FileItemMapper fileItemMapper;

    @Autowired
    private FileChunkMapper chunkMapper;

    @Autowired
    private UserMapper userMapper;

    @Value("${file.upload.basePath}")
    private String uploadBasePath;

    @Value("${file.chunk.size:1048576}")
    private long chunkSize;

    @Override
    public Result<List<FileItem>> getFileList(Long userId, Long parentId) {
        List<FileItem> list = fileItemMapper.selectByUserIdAndParent(userId, parentId);
        return Result.success(list);
    }

    @Override
    public Result<FileItem> createFolder(Long userId, Long parentId, String folderName) {
        if (folderName == null || folderName.trim().isEmpty()) {
            return Result.error("文件夹名称不能为空");
        }
        // 检查同名文件夹
        FileItem exist = fileItemMapper.selectByNameAndParent(userId, parentId, folderName.trim());
        if (exist != null) {
            return Result.error("已存在同名文件夹");
        }

        FileItem folder = FileItem.builder()
                .userId(userId)
                .parentId(parentId)
                .fileName(folderName.trim())
                .fileSize(0L)
                .isFolder(1)
                .isDeleted(0)
                .shareStatus(0)
                .build();

        fileItemMapper.insert(folder);
        log.info("创建文件夹: userId={}, folderName={}", userId, folderName);
        return Result.success("创建成功", folder);
    }

    @Override
    public Result<FileItem> uploadFile(Long userId, Long parentId,
                                        String fileName, byte[] fileData, String fileMd5) {
        try {
            // 1. 检查存储空间
            User user = userMapper.selectById(userId);
            Long used = fileItemMapper.sumFileSizeByUserId(userId);
            if (used == null) used = 0L;
            if (used + fileData.length > user.getStorageMax()) {
                return Result.error("存储空间不足！已用 " + FileUtil.formatSize(used) +
                        " / " + FileUtil.formatSize(user.getStorageMax()));
            }

            // 2. 秒传检测 — 如果文件MD5相同且存在物理文件则秒传
            if (fileMd5 != null && !fileMd5.isEmpty()) {
                FileItem existingByMd5 = fileItemMapper.selectByMd5(fileMd5);
                if (existingByMd5 != null && existingByMd5.getFilePath() != null) {
                    File targetFile = new File(existingByMd5.getFilePath());
                    if (targetFile.exists()) {
                        // 秒传：复用已有文件路径
                        String ext = FileUtil.getExtension(fileName);
                        FileItem newFile = FileItem.builder()
                                .userId(userId)
                                .parentId(parentId)
                                .fileName(fileName)
                                .filePath(existingByMd5.getFilePath()) // 复用路径
                                .fileSize(existingByMd5.getFileSize())
                                .fileType(ext)
                                .fileMd5(fileMd5)
                                .mimeType(FileUtil.getMimeType(ext))
                                .isFolder(0)
                                .isDeleted(0)
                                .shareStatus(0)
                                .build();
                        fileItemMapper.insert(newFile);
                        updateUserStorage(userId);
                        log.info("秒传成功: {}", fileName);
                        return Result.success("秒传成功", newFile);
                    }
                }
            }

            // 3. 正常上传
            String ext = FileUtil.getExtension(fileName);
            String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            String relativePath = userId + "/" + storedName;
            String fullPath = uploadBasePath + relativePath;

            File file = new File(fullPath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(fileData);
                fos.flush();
            }

            String actualMd5 = fileMd5 != null ? fileMd5 : MD5Util.md5(fileData);

            FileItem fileItem = FileItem.builder()
                    .userId(userId)
                    .parentId(parentId)
                    .fileName(fileName)
                    .filePath(fullPath)
                    .fileSize((long) fileData.length)
                    .fileType(ext)
                    .fileMd5(actualMd5)
                    .mimeType(FileUtil.getMimeType(ext))
                    .isFolder(0)
                    .isDeleted(0)
                    .shareStatus(0)
                    .build();

            fileItemMapper.insert(fileItem);
            updateUserStorage(userId);
            log.info("文件上传成功: {} ({} bytes)", fileName, fileData.length);
            return Result.success("上传成功", fileItem);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public Result<FileItem> checkInstantUpload(Long userId, Long parentId,
                                                String fileName, String fileMd5) {
        if (fileMd5 == null || fileMd5.isEmpty()) {
            return Result.success(null); // 无MD5则走正常上传
        }
        FileItem existing = fileItemMapper.selectByMd5(fileMd5);
        if (existing != null && existing.getFilePath() != null) {
            File f = new File(existing.getFilePath());
            if (f.exists()) {
                return Result.success(existing);
            }
        }
        return Result.success(null);
    }

    @Override
    public Result<?> uploadChunk(Long userId, String fileMd5, int chunkIndex,
                                  int totalChunks, String fileName, byte[] chunkData) {
        try {
            // 检查是否已上传过此分片
            FileChunk existing = chunkMapper.selectByMd5AndIndex(fileMd5, chunkIndex);
            if (existing != null) {
                return Result.success("分片已存在");
            }

            // 保存分片
            String chunkDir = uploadBasePath + "chunks/" + fileMd5 + "/";
            File dir = new File(chunkDir);
            if (!dir.exists()) dir.mkdirs();

            String chunkPath = chunkDir + chunkIndex + ".part";
            try (FileOutputStream fos = new FileOutputStream(chunkPath)) {
                fos.write(chunkData);
            }

            String chunkMd5 = MD5Util.md5(chunkData);

            FileChunk chunk = FileChunk.builder()
                    .fileMd5(fileMd5)
                    .chunkIndex(chunkIndex)
                    .chunkMd5(chunkMd5)
                    .chunkSize((long) chunkData.length)
                    .chunkPath(chunkPath)
                    .totalChunks(totalChunks)
                    .fileName(fileName)
                    .userId(userId)
                    .build();

            chunkMapper.insert(chunk);
            log.info("分片上传成功: {} [{} / {}]", fileMd5, chunkIndex + 1, totalChunks);
            return Result.success("分片上传成功");
        } catch (IOException e) {
            log.error("分片上传失败", e);
            return Result.error("分片上传失败");
        }
    }

    @Override
    public Result<List<Integer>> checkChunkProgress(String fileMd5) {
        List<Integer> uploadedIndexes = chunkMapper.selectUploadedChunkIndexes(fileMd5);
        return Result.success(uploadedIndexes);
    }

    @Override
    public Result<FileItem> mergeChunks(Long userId, Long parentId, String fileMd5, String fileName) {
        try {
            int totalChunks = chunkMapper.countByFileMd5(fileMd5);
            if (totalChunks == 0) {
                return Result.error("没有可合并的分片");
            }

            // 检查存储空间
            User user = userMapper.selectById(userId);
            long totalSize = 0;
            List<FileChunk> chunks = chunkMapper.selectByFileMd5(fileMd5);
            for (FileChunk c : chunks) {
                totalSize += c.getChunkSize();
            }

            Long used = fileItemMapper.sumFileSizeByUserId(userId);
            if (used == null) used = 0L;
            if (used + totalSize > user.getStorageMax()) {
                return Result.error("存储空间不足");
            }

            // 合并文件
            String ext = FileUtil.getExtension(fileName);
            String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            String relativePath = userId + "/" + storedName;
            String targetPath = uploadBasePath + relativePath;

            String chunkDir = uploadBasePath + "chunks/" + fileMd5 + "/";
            FileUtil.mergeChunks(chunkDir, targetPath, totalChunks);

            // 清理分片记录
            chunkMapper.deleteByFileMd5(fileMd5);

            // 创建文件记录
            FileItem fileItem = FileItem.builder()
                    .userId(userId)
                    .parentId(parentId)
                    .fileName(fileName)
                    .filePath(targetPath)
                    .fileSize(totalSize)
                    .fileType(ext)
                    .fileMd5(fileMd5)
                    .mimeType(FileUtil.getMimeType(ext))
                    .isFolder(0)
                    .isDeleted(0)
                    .shareStatus(0)
                    .build();

            fileItemMapper.insert(fileItem);
            updateUserStorage(userId);
            log.info("分片合并成功: {} ({} bytes)", fileName, totalSize);
            return Result.success("合并成功", fileItem);
        } catch (IOException e) {
            log.error("分片合并失败", e);
            return Result.error("合并失败: " + e.getMessage());
        }
    }

    @Override
    public Result<?> renameFile(Long fileId, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return Result.error("名称不能为空");
        }
        fileItemMapper.updateName(fileId, newName.trim());
        log.info("文件重命名: fileId={}, newName={}", fileId, newName);
        return Result.success("重命名成功");
    }

    @Override
    public Result<?> moveFile(Long fileId, Long targetParentId) {
        fileItemMapper.updateParent(fileId, targetParentId);
        log.info("文件移动: fileId={}, targetParentId={}", fileId, targetParentId);
        return Result.success("移动成功");
    }

    @Override
    public Result<?> deleteFile(Long fileId) {
        fileItemMapper.softDelete(fileId);
        log.info("文件移入回收站: fileId={}", fileId);
        return Result.success("已移入回收站");
    }

    @Override
    public Result<?> batchDeleteFiles(List<Long> fileIds) {
        for (Long id : fileIds) {
            fileItemMapper.softDelete(id);
        }
        log.info("批量删除: {} 个文件", fileIds.size());
        return Result.success("已批量移入回收站");
    }

    @Override
    public Result<?> restoreFile(Long fileId) {
        fileItemMapper.restore(fileId);
        log.info("还原文件: fileId={}", fileId);
        return Result.success("已还原");
    }

    @Override
    public Result<?> permanentDelete(Long fileId) {
        FileItem file = fileItemMapper.selectById(fileId);
        if (file != null) {
            // 删除物理文件
            if (file.getFilePath() != null && !file.isFolder()) {
                FileUtil.deleteFile(file.getFilePath());
            }
            fileItemMapper.deleteById(fileId);
            updateUserStorage(file.getUserId());
        }
        log.info("彻底删除文件: fileId={}", fileId);
        return Result.success("已彻底删除");
    }

    @Override
    public Result<List<FileItem>> getRecycleBinList(Long userId) {
        List<FileItem> list = fileItemMapper.selectDeletedByUserId(userId);
        return Result.success(list);
    }

    @Override
    public Result<?> clearRecycleBin(Long userId) {
        List<FileItem> deletedFiles = fileItemMapper.selectDeletedByUserId(userId);
        for (FileItem file : deletedFiles) {
            if (file.getFilePath() != null && !file.isFolder()) {
                FileUtil.deleteFile(file.getFilePath());
            }
            fileItemMapper.deleteById(file.getId());
        }
        updateUserStorage(userId);
        log.info("清空回收站: userId={}, count={}", userId, deletedFiles.size());
        return Result.success("回收站已清空");
    }

    @Override
    public void downloadFile(Long fileId, HttpServletResponse response) {
        try {
            FileItem file = fileItemMapper.selectById(fileId);
            if (file == null || file.isFolder()) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"文件不存在\"}");
                return;
            }

            File physicalFile = new File(file.getFilePath());
            if (!physicalFile.exists()) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"物理文件不存在\"}");
                return;
            }

            // 支持断点续传（Range请求）
            long fileLength = physicalFile.length();
            String rangeHeader = response.getHeader("Range");

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(file.getFileName(), "UTF-8") + "\"");
            response.setHeader("Accept-Ranges", "bytes");

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                // Range请求处理
                String[] ranges = rangeHeader.substring(6).split("-");
                long start = Long.parseLong(ranges[0]);
                long end = ranges.length > 1 && !ranges[1].isEmpty()
                        ? Long.parseLong(ranges[1]) : fileLength - 1;

                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
                response.setContentLengthLong(end - start + 1);

                try (RandomAccessFile raf = new RandomAccessFile(physicalFile, "r");
                     OutputStream os = response.getOutputStream()) {
                    raf.seek(start);
                    byte[] buffer = new byte[8192];
                    long remaining = end - start + 1;
                    while (remaining > 0) {
                        int read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (read == -1) break;
                        os.write(buffer, 0, read);
                        remaining -= read;
                    }
                }
            } else {
                // 完整下载
                response.setContentLengthLong(fileLength);
                try (FileInputStream fis = new FileInputStream(physicalFile);
                     OutputStream os = response.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
            }
            log.info("文件下载: {}", file.getFileName());
        } catch (IOException e) {
            log.error("文件下载失败", e);
        }
    }

    @Override
    public Result<List<FileItem>> getSharedFiles(Long userId) {
        List<FileItem> list = fileItemMapper.selectSharedByUserId(userId);
        return Result.success(list);
    }

    @Override
    public Result<List<FileItem>> searchFiles(Long userId, String keyword) {
        List<FileItem> list = fileItemMapper.searchByUserIdAndKeyword(userId, keyword);
        return Result.success(list);
    }

    @Override
    public FileItem getFileById(Long fileId) {
        return fileItemMapper.selectById(fileId);
    }

    @Override
    public Long getUsedStorage(Long userId) {
        Long used = fileItemMapper.sumFileSizeByUserId(userId);
        return used != null ? used : 0L;
    }

    @Override
    public Long getMaxStorage(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getStorageMax() : 1073741824L;
    }

    @Override
    public List<FileItem> getAllFiles() {
        return fileItemMapper.selectAllFiles();
    }

    @Override
    public List<FileItem> getFilesByUserId(Long userId) {
        return fileItemMapper.selectAllFilesByUserId(userId);
    }

    @Override
    public Result<?> adminDeleteFile(Long fileId) {
        FileItem file = fileItemMapper.selectById(fileId);
        if (file != null) {
            if (file.getFilePath() != null) {
                FileUtil.deleteFile(file.getFilePath());
            }
            fileItemMapper.deleteById(fileId);
        }
        return Result.success("删除成功");
    }

    @Override
    public List<FileItem> getAllSharedFiles() {
        // 全部被分享的文件通过列表过滤
        List<FileItem> all = fileItemMapper.selectAllFiles();
        List<FileItem> shared = new ArrayList<>();
        for (FileItem f : all) {
            if (f.isShared()) {
                shared.add(f);
            }
        }
        return shared;
    }

    /**
     * 更新用户已用存储空间
     */
    private void updateUserStorage(Long userId) {
        Long used = fileItemMapper.sumFileSizeByUserId(userId);
        if (used == null) used = 0L;
        userMapper.updateStorageUsed(userId, used);
    }
}
