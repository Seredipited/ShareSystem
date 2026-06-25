-- ============================================
-- 个人学习资料分享系统 - 数据库初始化脚本
-- Spring Cloud 微服务架构版本
-- ============================================

DROP DATABASE IF EXISTS share_system;
CREATE DATABASE share_system CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE share_system;

-- ============================================
-- 用户表（user-service 管理）
-- ============================================
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码(MD5加密)',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `role` TINYINT DEFAULT 0 COMMENT '角色: 0-普通用户, 1-管理员',
    `storage_used` BIGINT DEFAULT 0 COMMENT '已用存储空间(字节)',
    `storage_max` BIGINT DEFAULT 1073741824 COMMENT '最大存储空间,默认1GB',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    INDEX `idx_role` (`role`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 第三方登录表（user-service 管理）
-- ============================================
CREATE TABLE `user_oauth` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
    `platform` VARCHAR(20) NOT NULL COMMENT '平台: qq, wechat',
    `open_id` VARCHAR(100) NOT NULL COMMENT '第三方平台OpenID',
    `access_token` VARCHAR(255) DEFAULT NULL COMMENT '访问令牌',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '第三方昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '第三方头像',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_platform_openid` (`platform`, `open_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方登录表';

-- ============================================
-- 文件/文件夹表（file-service 管理）
-- ============================================
CREATE TABLE `file_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父目录ID,0表示根目录',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件/文件夹名称',
    `file_path` VARCHAR(500) DEFAULT NULL COMMENT '物理存储路径',
    `file_size` BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件扩展名',
    `file_md5` VARCHAR(64) DEFAULT NULL COMMENT '文件MD5值(用于秒传)',
    `mime_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
    `is_folder` TINYINT DEFAULT 0 COMMENT '0-文件, 1-文件夹',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '0-正常, 1-回收站',
    `delete_time` DATETIME DEFAULT NULL COMMENT '删除时间',
    `share_status` TINYINT DEFAULT 0 COMMENT '0-未分享, 1-已分享',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_file_md5` (`file_md5`),
    INDEX `idx_is_deleted` (`is_deleted`),
    INDEX `idx_share_status` (`share_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件/文件夹表';

-- ============================================
-- 文件分片表 - 分片上传（file-service 管理）
-- ============================================
CREATE TABLE `file_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分片ID',
    `file_md5` VARCHAR(64) NOT NULL COMMENT '完整文件MD5',
    `chunk_index` INT NOT NULL COMMENT '分片序号(从0开始)',
    `chunk_md5` VARCHAR(64) DEFAULT NULL COMMENT '分片MD5',
    `chunk_size` BIGINT DEFAULT 0 COMMENT '分片大小(字节)',
    `chunk_path` VARCHAR(500) DEFAULT NULL COMMENT '分片存储路径',
    `total_chunks` INT DEFAULT NULL COMMENT '总分片数',
    `file_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    `user_id` BIGINT NOT NULL COMMENT '上传用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_file_md5` (`file_md5`),
    INDEX `idx_user_id` (`user_id`),
    UNIQUE KEY `uk_md5_chunk` (`file_md5`, `chunk_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分片表';

-- ============================================
-- 文件分享表（file-service 管理）
-- ============================================
CREATE TABLE `share` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分享ID',
    `file_id` BIGINT NOT NULL COMMENT '文件/文件夹ID',
    `user_id` BIGINT NOT NULL COMMENT '分享者用户ID',
    `share_code` VARCHAR(20) NOT NULL COMMENT '分享码',
    `share_pwd` VARCHAR(10) DEFAULT NULL COMMENT '提取码(可选)',
    `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间(NULL表示永久有效)',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `download_count` INT DEFAULT 0 COMMENT '下载次数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_share_code` (`share_code`),
    INDEX `idx_file_id` (`file_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分享表';

-- ============================================
-- 操作日志表（file-service 管理）
-- ============================================
CREATE TABLE `operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
    `operation` VARCHAR(50) DEFAULT NULL COMMENT '操作类型',
    `target` VARCHAR(255) DEFAULT NULL COMMENT '操作对象',
    `detail` TEXT DEFAULT NULL COMMENT '详细信息',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============================================
-- 默认测试数据
-- ============================================
-- 管理员: admin / admin123 (MD5)
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `storage_max`) VALUES
('admin', '0192023a7bbd73250516f069df18b500', '系统管理员', 1, 10737418240);

-- 普通用户: test / test123 (MD5)
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `storage_max`) VALUES
('test', 'cc03e747a6afbbcbf8be7668acfebee5', '测试用户', 0, 1073741824);
