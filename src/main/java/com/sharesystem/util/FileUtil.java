package com.sharesystem.util;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * 文件操作工具类
 */
public class FileUtil {

    /**
     * 获取文件扩展名（不含点）
     */
    public static String getExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) return "";
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * 根据扩展名获取MIME类型
     */
    public static String getMimeType(String extension) {
        if (extension == null) return "application/octet-stream";
        switch (extension.toLowerCase()) {
            case "txt": return "text/plain";
            case "html": case "htm": return "text/html";
            case "css": return "text/css";
            case "js": return "application/javascript";
            case "json": return "application/json";
            case "xml": return "application/xml";
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "bmp": return "image/bmp";
            case "svg": return "image/svg+xml";
            case "ico": return "image/x-icon";
            case "webp": return "image/webp";
            case "mp3": return "audio/mpeg";
            case "wav": return "audio/wav";
            case "ogg": return "audio/ogg";
            case "flac": return "audio/flac";
            case "aac": return "audio/aac";
            case "mp4": return "video/mp4";
            case "avi": return "video/x-msvideo";
            case "mov": return "video/quicktime";
            case "wmv": return "video/x-ms-wmv";
            case "flv": return "video/x-flv";
            case "webm": return "video/webm";
            case "mkv": return "video/x-matroska";
            case "pdf": return "application/pdf";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt": return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "zip": return "application/zip";
            case "rar": return "application/x-rar-compressed";
            case "7z": return "application/x-7z-compressed";
            case "tar": case "gz": return "application/x-gzip";
            default: return "application/octet-stream";
        }
    }

    /**
     * 获取文件图标类型（用于前端显示）
     */
    public static String getFileIconType(String extension) {
        if (extension == null) return "file";
        switch (extension.toLowerCase()) {
            case "txt": case "html": case "htm": case "css": case "js":
            case "json": case "xml": case "java": case "py": case "c": case "cpp":
                return "text";
            case "jpg": case "jpeg": case "png": case "gif":
            case "bmp": case "svg": case "ico": case "webp":
                return "image";
            case "mp3": case "wav": case "ogg": case "flac": case "aac":
                return "audio";
            case "mp4": case "avi": case "mov": case "wmv":
            case "flv": case "webm": case "mkv":
                return "video";
            case "pdf":
                return "pdf";
            case "doc": case "docx":
                return "word";
            case "xls": case "xlsx":
                return "excel";
            case "ppt": case "pptx":
                return "ppt";
            case "zip": case "rar": case "7z": case "tar": case "gz":
                return "archive";
            default:
                return "file";
        }
    }

    /**
     * 判断文件是否可在线预览
     */
    public static boolean isPreviewable(String extension) {
        if (extension == null) return false;
        switch (extension.toLowerCase()) {
            case "txt": case "html": case "htm":
            case "jpg": case "jpeg": case "png": case "gif": case "bmp": case "svg": case "webp":
            case "mp3": case "wav": case "ogg":
            case "mp4": case "webm": case "ogg":
            case "pdf":
            case "doc": case "docx":
            case "xls": case "xlsx":
                return true;
            default:
                return false;
        }
    }

    /**
     * 复制文件
     */
    public static void copyFile(String sourcePath, String targetPath) throws IOException {
        Path source = Paths.get(sourcePath);
        Path target = Paths.get(targetPath);
        Files.createDirectories(target.getParent());
        Files.copy(source, target);
    }

    /**
     * 删除文件
     */
    public static boolean deleteFile(String filePath) {
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 递归删除目录
     */
    public static boolean deleteDirectory(String dirPath) {
        try {
            Path path = Paths.get(dirPath);
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException ignored) {}
                        });
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 合并分片文件
     */
    public static void mergeChunks(String chunkDir, String targetPath, int totalChunks) throws IOException {
        File targetFile = new File(targetPath);
        File parentDir = targetFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(targetFile, true);
             FileChannel outChannel = fos.getChannel()) {
            for (int i = 0; i < totalChunks; i++) {
                File chunkFile = new File(chunkDir, i + ".part");
                if (chunkFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(chunkFile);
                         FileChannel inChannel = fis.getChannel()) {
                        inChannel.transferTo(0, inChannel.size(), outChannel);
                    }
                }
            }
        }
        // 清理分片目录
        deleteDirectory(chunkDir);
    }

    /**
     * 格式化文件大小
     */
    public static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups > 4) digitGroups = 4;
        return String.format("%.1f %s",
                bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
