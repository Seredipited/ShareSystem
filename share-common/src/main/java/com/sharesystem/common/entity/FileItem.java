package com.sharesystem.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("file_item")
public class FileItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 父目录ID, 0表示根目录 */
    private Long parentId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private String fileMd5;
    private String mimeType;
    private Integer isFolder;
    private Integer isDeleted;
    private LocalDateTime deleteTime;
    private Integer shareStatus;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
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
