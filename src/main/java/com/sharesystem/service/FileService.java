package com.sharesystem.service;

import com.sharesystem.dto.Result;
import com.sharesystem.entity.FileItem;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 文件服务接口
 */
public interface FileService {

    /**
     * 获取文件列表（指定目录下）
     */
    Result<List<FileItem>> getFileList(Long userId, Long parentId);

    /**
     * 创建文件夹
     */
    Result<FileItem> createFolder(Long userId, Long parentId, String folderName);

    /**
     * 文件上传（支持秒传检测）
     */
    Result<FileItem> uploadFile(Long userId, Long parentId, String fileName,
                                 byte[] fileData, String fileMd5);

    /**
     * 秒传检测 - 检查文件MD5是否已存在
     */
    Result<FileItem> checkInstantUpload(Long userId, Long parentId, String fileName, String fileMd5);

    /**
     * 分片上传 - 上传单个分片
     */
    Result<?> uploadChunk(Long userId, String fileMd5, int chunkIndex, int totalChunks,
                           String fileName, byte[] chunkData);

    /**
     * 检查分片上传进度（断点续传）
     */
    Result<List<Integer>> checkChunkProgress(String fileMd5);

    /**
     * 合并分片文件
     */
    Result<FileItem> mergeChunks(Long userId, Long parentId, String fileMd5, String fileName);

    /**
     * 文件重命名
     */
    Result<?> renameFile(Long fileId, String newName);

    /**
     * 文件移动
     */
    Result<?> moveFile(Long fileId, Long targetParentId);

    /**
     * 删除文件（移入回收站）
     */
    Result<?> deleteFile(Long fileId);

    /**
     * 批量删除文件
     */
    Result<?> batchDeleteFiles(List<Long> fileIds);

    /**
     * 从回收站还原文件
     */
    Result<?> restoreFile(Long fileId);

    /**
     * 彻底删除文件
     */
    Result<?> permanentDelete(Long fileId);

    /**
     * 获取回收站文件列表
     */
    Result<List<FileItem>> getRecycleBinList(Long userId);

    /**
     * 清空回收站
     */
    Result<?> clearRecycleBin(Long userId);

    /**
     * 文件下载
     */
    void downloadFile(Long fileId, HttpServletResponse response);

    /**
     * 获取分享文件列表
     */
    Result<List<FileItem>> getSharedFiles(Long userId);

    /**
     * 搜索文件
     */
    Result<List<FileItem>> searchFiles(Long userId, String keyword);

    /**
     * 获取文件信息
     */
    FileItem getFileById(Long fileId);

    /**
     * 获取用户已用空间
     */
    Long getUsedStorage(Long userId);

    /**
     * 获取用户总空间
     */
    Long getMaxStorage(Long userId);

    /**
     * 管理员 - 获取所有文件
     */
    List<FileItem> getAllFiles();

    /**
     * 管理员 - 根据用户ID获取文件
     */
    List<FileItem> getFilesByUserId(Long userId);

    /**
     * 管理员 - 删除任意文件
     */
    Result<?> adminDeleteFile(Long fileId);

    /**
     * 管理员 - 获取所有分享文件
     */
    List<FileItem> getAllSharedFiles();
}
