package com.sharesystem.file.service.impl;

import com.sharesystem.common.dto.Result;
import com.sharesystem.common.entity.FileChunk;
import com.sharesystem.common.entity.FileItem;
import com.sharesystem.common.entity.User;
import com.sharesystem.common.util.FileUtil;
import com.sharesystem.common.util.MD5Util;
import com.sharesystem.file.feign.UserFeignClient;
import com.sharesystem.file.mapper.FileChunkMapper;
import com.sharesystem.file.mapper.FileItemMapper;
import com.sharesystem.file.mapper.OperationLogMapper;
import com.sharesystem.file.service.FileService;
import com.sharesystem.common.dto.R;
import com.sharesystem.common.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    @Autowired
    private FileItemMapper fileItemMapper;
    @Autowired
    private FileChunkMapper chunkMapper;
    @Autowired
    private OperationLogMapper logMapper;
    @Autowired
    private UserFeignClient userFeignClient;

    @Value("${file.upload.base-path:D:/share_system/files/}")
    private String uploadBasePath;

    @Override
    public Result<List<FileItem>> getFileList(Long userId, Long parentId) {
        return Result.success(fileItemMapper.selectByUserIdAndParent(userId, parentId));
    }

    @Override
    public Result<FileItem> createFolder(Long userId, Long parentId, String folderName) {
        if (folderName == null || folderName.trim().isEmpty())
            return Result.error("文件夹名称不能为空");
        FileItem exist = fileItemMapper.selectByNameAndParent(userId, parentId, folderName.trim());
        if (exist != null) return Result.error("已存在同名文件夹");

        FileItem folder = FileItem.builder()
                .userId(userId).parentId(parentId).fileName(folderName.trim())
                .fileSize(0L).isFolder(1).isDeleted(0).shareStatus(0).build();
        fileItemMapper.insert(folder);
        return Result.success("创建成功", folder);
    }

    @Override
    public Result<FileItem> uploadFile(Long userId, Long parentId, String fileName,
                                        byte[] fileData, String fileMd5) {
        try {
            // 检查空间
            R<UserDTO> userR = userFeignClient.getUserById(userId);
            if (!userR.success()) return Result.error("用户信息获取失败");
            UserDTO user = userR.getData();
            Long used = fileItemMapper.sumFileSizeByUserId(userId);
            if (used == null) used = 0L;
            if (used + fileData.length > user.getStorageMax()) {
                return Result.error("存储空间不足！已用 " + FileUtil.formatSize(used)
                        + " / " + FileUtil.formatSize(user.getStorageMax()));
            }

            // 秒传检测
            if (fileMd5 != null && !fileMd5.isEmpty()) {
                FileItem existing = fileItemMapper.selectByMd5(fileMd5);
                if (existing != null && existing.getFilePath() != null
                        && new File(existing.getFilePath()).exists()) {
                    String ext = FileUtil.getExtension(fileName);
                    FileItem newFile = FileItem.builder()
                            .userId(userId).parentId(parentId).fileName(fileName)
                            .filePath(existing.getFilePath()).fileSize(existing.getFileSize())
                            .fileType(ext).fileMd5(fileMd5)
                            .mimeType(FileUtil.getMimeType(ext))
                            .isFolder(0).isDeleted(0).shareStatus(0).build();
                    fileItemMapper.insert(newFile);
                    log.info("秒传成功: {}", fileName);
                    return Result.success("秒传成功", newFile);
                }
            }

            // 正常上传
            String ext = FileUtil.getExtension(fileName);
            String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            String fullPath = uploadBasePath + userId + "/" + storedName;
            File file = new File(fullPath);
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(fileData);
            }

            String actualMd5 = fileMd5 != null ? fileMd5 : MD5Util.md5(fileData);
            FileItem item = FileItem.builder()
                    .userId(userId).parentId(parentId).fileName(fileName)
                    .filePath(fullPath).fileSize((long) fileData.length)
                    .fileType(ext).fileMd5(actualMd5).mimeType(FileUtil.getMimeType(ext))
                    .isFolder(0).isDeleted(0).shareStatus(0).build();
            fileItemMapper.insert(item);
            updateUserStorage(userId);
            return Result.success("上传成功", item);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @Override
    public Result<FileItem> checkInstantUpload(Long userId, Long parentId,
                                                String fileName, String fileMd5) {
        if (fileMd5 == null || fileMd5.isEmpty()) return Result.success(null);
        FileItem existing = fileItemMapper.selectByMd5(fileMd5);
        if (existing != null && existing.getFilePath() != null
                && new File(existing.getFilePath()).exists()) {
            return Result.success(existing);
        }
        return Result.success(null);
    }

    @Override
    public Result<?> uploadChunk(Long userId, String fileMd5, int chunkIndex,
                                  int totalChunks, String fileName, byte[] chunkData) {
        try {
            FileChunk existing = chunkMapper.selectByMd5AndIndex(fileMd5, chunkIndex);
            if (existing != null) return Result.success("分片已存在");

            String chunkDir = uploadBasePath + "chunks/" + fileMd5 + "/";
            new File(chunkDir).mkdirs();
            String chunkPath = chunkDir + chunkIndex + ".part";
            try (FileOutputStream fos = new FileOutputStream(chunkPath)) {
                fos.write(chunkData);
            }

            FileChunk chunk = FileChunk.builder()
                    .fileMd5(fileMd5).chunkIndex(chunkIndex)
                    .chunkMd5(MD5Util.md5(chunkData)).chunkSize((long) chunkData.length)
                    .chunkPath(chunkPath).totalChunks(totalChunks)
                    .fileName(fileName).userId(userId).build();
            chunkMapper.insert(chunk);
            return Result.success("分片上传成功");
        } catch (IOException e) {
            return Result.error("分片上传失败");
        }
    }

    @Override
    public Result<List<Integer>> checkChunkProgress(String fileMd5) {
        return Result.success(chunkMapper.selectUploadedChunkIndexes(fileMd5));
    }

    @Override
    public Result<FileItem> mergeChunks(Long userId, Long parentId,
                                         String fileMd5, String fileName) {
        try {
            int totalChunks = chunkMapper.countByFileMd5(fileMd5);
            if (totalChunks == 0) return Result.error("没有可合并的分片");

            R<UserDTO> userR = userFeignClient.getUserById(userId);
            if (!userR.success()) return Result.error("用户信息获取失败");
            UserDTO user = userR.getData();

            List<FileChunk> chunks = chunkMapper.selectByFileMd5(fileMd5);
            long totalSize = chunks.stream().mapToLong(FileChunk::getChunkSize).sum();
            Long used = fileItemMapper.sumFileSizeByUserId(userId);
            if (used == null) used = 0L;
            if (used + totalSize > user.getStorageMax()) return Result.error("存储空间不足");

            String ext = FileUtil.getExtension(fileName);
            String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            String targetPath = uploadBasePath + userId + "/" + storedName;
            String chunkDir = uploadBasePath + "chunks/" + fileMd5 + "/";
            FileUtil.mergeChunks(chunkDir, targetPath, totalChunks);
            chunkMapper.deleteByFileMd5(fileMd5);

            FileItem item = FileItem.builder()
                    .userId(userId).parentId(parentId).fileName(fileName)
                    .filePath(targetPath).fileSize(totalSize)
                    .fileType(ext).fileMd5(fileMd5).mimeType(FileUtil.getMimeType(ext))
                    .isFolder(0).isDeleted(0).shareStatus(0).build();
            fileItemMapper.insert(item);
            updateUserStorage(userId);
            return Result.success("合并成功", item);
        } catch (IOException e) {
            return Result.error("合并失败: " + e.getMessage());
        }
    }

    @Override
    public Result<?> renameFile(Long fileId, String newName) {
        if (newName == null || newName.trim().isEmpty()) return Result.error("名称不能为空");
        fileItemMapper.updateName(fileId, newName.trim());
        return Result.success("重命名成功");
    }

    @Override
    public Result<?> moveFile(Long fileId, Long targetParentId) {
        fileItemMapper.updateParent(fileId, targetParentId);
        return Result.success("移动成功");
    }

    @Override
    public Result<?> deleteFile(Long fileId) {
        fileItemMapper.softDelete(fileId);
        return Result.success("已移入回收站");
    }

    @Override
    public Result<?> batchDeleteFiles(List<Long> fileIds) {
        for (Long id : fileIds) fileItemMapper.softDelete(id);
        return Result.success("已批量移入回收站");
    }

    @Override
    public Result<?> restoreFile(Long fileId) {
        fileItemMapper.restore(fileId);
        return Result.success("已还原");
    }

    @Override
    public Result<?> permanentDelete(Long fileId) {
        FileItem file = fileItemMapper.selectById(fileId);
        if (file != null) {
            if (file.getFilePath() != null && !file.isFolder())
                FileUtil.deleteFile(file.getFilePath());
            fileItemMapper.deleteById(fileId);
            updateUserStorage(file.getUserId());
        }
        return Result.success("已彻底删除");
    }

    @Override
    public Result<List<FileItem>> getRecycleBinList(Long userId) {
        return Result.success(fileItemMapper.selectDeletedByUserId(userId));
    }

    @Override
    public Result<?> clearRecycleBin(Long userId) {
        List<FileItem> deleted = fileItemMapper.selectDeletedByUserId(userId);
        for (FileItem f : deleted) {
            if (f.getFilePath() != null && !f.isFolder()) FileUtil.deleteFile(f.getFilePath());
            fileItemMapper.deleteById(f.getId());
        }
        updateUserStorage(userId);
        return Result.success("回收站已清空");
    }

    @Override
    public void downloadFile(Long fileId, HttpServletResponse response) {
        try {
            FileItem file = fileItemMapper.selectById(fileId);
            if (file == null || file.isFolder()) { response.sendError(404); return; }
            File physicalFile = new File(file.getFilePath());
            if (!physicalFile.exists()) { response.sendError(404); return; }

            long fileLength = physicalFile.length();
            String rangeHeader = response.getHeader("Range");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(file.getFileName(), "UTF-8") + "\"");
            response.setHeader("Accept-Ranges", "bytes");

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
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
                response.setContentLengthLong(fileLength);
                try (FileInputStream fis = new FileInputStream(physicalFile);
                     OutputStream os = response.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) os.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) { log.error("下载失败", e); }
    }

    @Override
    public Result<List<FileItem>> getSharedFiles(Long userId) {
        return Result.success(fileItemMapper.selectSharedByUserId(userId));
    }

    @Override
    public Result<List<FileItem>> searchFiles(Long userId, String keyword) {
        return Result.success(fileItemMapper.searchByUserIdAndKeyword(userId, keyword));
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
            if (file.getFilePath() != null) FileUtil.deleteFile(file.getFilePath());
            fileItemMapper.deleteById(fileId);
        }
        return Result.success("删除成功");
    }

    @Override
    public List<FileItem> getAllSharedFiles() {
        List<FileItem> all = fileItemMapper.selectAllFiles();
        List<FileItem> shared = new ArrayList<>();
        for (FileItem f : all) if (f.isShared()) shared.add(f);
        return shared;
    }

    private void updateUserStorage(Long userId) {
        Long used = fileItemMapper.sumFileSizeByUserId(userId);
        if (used == null) used = 0L;
        // Note: 直接更新数据库即可，Feign通知user-service更新缓存
    }
}
