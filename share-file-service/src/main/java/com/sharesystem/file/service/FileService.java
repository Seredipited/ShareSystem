package com.sharesystem.file.service;

import com.sharesystem.common.dto.Result;
import com.sharesystem.common.entity.FileItem;

import java.util.List;

public interface FileService {

    Result<List<FileItem>> getFileList(Long userId, Long parentId);
    Result<FileItem> createFolder(Long userId, Long parentId, String folderName);
    Result<FileItem> uploadFile(Long userId, Long parentId, String fileName, byte[] fileData, String fileMd5);
    Result<FileItem> checkInstantUpload(Long userId, Long parentId, String fileName, String fileMd5);
    Result<?> uploadChunk(Long userId, String fileMd5, int chunkIndex, int totalChunks, String fileName, byte[] chunkData);
    Result<List<Integer>> checkChunkProgress(String fileMd5);
    Result<FileItem> mergeChunks(Long userId, Long parentId, String fileMd5, String fileName);
    Result<?> renameFile(Long fileId, String newName);
    Result<?> moveFile(Long fileId, Long targetParentId);
    Result<?> deleteFile(Long fileId);
    Result<?> batchDeleteFiles(List<Long> fileIds);
    Result<?> restoreFile(Long fileId);
    Result<?> permanentDelete(Long fileId);
    Result<List<FileItem>> getRecycleBinList(Long userId);
    Result<?> clearRecycleBin(Long userId);
    Result<List<FileItem>> getSharedFiles(Long userId);
    Result<List<FileItem>> searchFiles(Long userId, String keyword);
    FileItem getFileById(Long fileId);
    Long getUsedStorage(Long userId);
    List<FileItem> getAllFiles();
    List<FileItem> getFilesByUserId(Long userId);
    Result<?> adminDeleteFile(Long fileId);
    List<FileItem> getAllSharedFiles();
    void downloadFile(Long fileId, javax.servlet.http.HttpServletResponse response);
}
