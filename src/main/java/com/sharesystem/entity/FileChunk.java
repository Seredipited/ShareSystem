package com.sharesystem.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件分片实体类（分片上传）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileChunk {

    private Long id;
    /**
     * 完整文件MD5
     */
    private String fileMd5;
    /**
     * 分片序号(从0开始)
     */
    private Integer chunkIndex;
    /**
     * 分片MD5
     */
    private String chunkMd5;
    /**
     * 分片大小(字节)
     */
    private Long chunkSize;
    /**
     * 分片存储路径
     */
    private String chunkPath;
    /**
     * 总分片数
     */
    private Integer totalChunks;
    /**
     * 原始文件名
     */
    private String fileName;
    private Long userId;
    private LocalDateTime createTime;
}
