package com.sharesystem.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件/文件夹实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileItem {

    private Long id;
    private Long userId;
    /**
     * 父目录ID, 0表示根目录
     */
    private Long parentId;
    /**
     * 文件/文件夹名称
     */
    private String fileName;
    /**
     * 物理存储路径
     */
    private String filePath;
    /**
     * 文件大小(字节)
     */
    private Long fileSize;
    /**
     * 文件扩展名
     */
    private String fileType;
    /**
     * 文件MD5值(用于秒传)
     */
    private String fileMd5;
    /**
     * MIME类型
     */
    private String mimeType;
    /**
     * 0-文件, 1-文件夹
     */
    private Integer isFolder;
    /**
     * 0-正常, 1-回收站
     */
    private Integer isDeleted;
    /**
     * 删除时间
     */
    private LocalDateTime deleteTime;
    /**
     * 0-未分享, 1-已分享
     */
    private Integer shareStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public boolean isFolder() {
        return isFolder != null && isFolder == 1;
    }

    public boolean isDeleted() {
        return isDeleted != null && isDeleted == 1;
    }

    public boolean isShared() {
        return shareStatus != null && shareStatus == 1;
    }

    /**
     * 格式化文件大小
     */
    public String getFormattedSize() {
        if (fileSize == null || fileSize == 0) return "0 B";
        if (isFolder() && fileSize == 0) return "-";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(fileSize) / Math.log10(1024));
        if (digitGroups > 4) digitGroups = 4;
        return String.format("%.1f %s",
                fileSize / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
