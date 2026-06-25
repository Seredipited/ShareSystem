package com.sharesystem.service.impl;

import com.sharesystem.dto.Result;
import com.sharesystem.entity.FileItem;
import com.sharesystem.entity.Share;
import com.sharesystem.mapper.FileItemMapper;
import com.sharesystem.mapper.ShareMapper;
import com.sharesystem.service.ShareService;
import com.sharesystem.util.CodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分享服务实现
 */
@Service
@Transactional
public class ShareServiceImpl implements ShareService {

    private static final Logger log = LoggerFactory.getLogger(ShareServiceImpl.class);

    @Autowired
    private ShareMapper shareMapper;

    @Autowired
    private FileItemMapper fileItemMapper;

    @Override
    public Result<Share> createShare(Long fileId, Long userId, String sharePwd, Integer expireDays) {
        FileItem file = fileItemMapper.selectById(fileId);
        if (file == null) {
            return Result.error("文件不存在");
        }
        if (!file.getUserId().equals(userId)) {
            return Result.error("无权操作此文件");
        }
        if (file.isDeleted()) {
            return Result.error("文件已被删除");
        }

        // 检查是否已分享
        Share existShare = shareMapper.selectByFileId(fileId);
        if (existShare != null) {
            // 已分享则返回已有链接
            if (existShare.isExpired()) {
                shareMapper.deleteById(existShare.getId());
            } else {
                return Result.success("分享已存在", existShare);
            }
        }

        // 创建分享
        String shareCode = CodeUtil.generateShareCode();

        LocalDateTime expireTime = null;
        if (expireDays != null && expireDays > 0) {
            expireTime = LocalDateTime.now().plusDays(expireDays);
        }

        Share share = Share.builder()
                .fileId(fileId)
                .userId(userId)
                .shareCode(shareCode)
                .sharePwd(sharePwd)
                .expireTime(expireTime)
                .viewCount(0)
                .downloadCount(0)
                .build();

        shareMapper.insert(share);

        // 更新文件分享状态
        fileItemMapper.updateShareStatus(fileId, 1);

        log.info("创建分享: fileId={}, shareCode={}", fileId, shareCode);
        return Result.success("分享创建成功", share);
    }

    @Override
    public Result<Share> getShareByCode(String shareCode) {
        Share share = shareMapper.selectByShareCode(shareCode);
        if (share == null) {
            return Result.error("分享不存在");
        }
        if (share.isExpired()) {
            return Result.error("分享已过期");
        }
        // 增加浏览次数
        shareMapper.incrementViewCount(share.getId());
        return Result.success(share);
    }

    @Override
    public Result<?> verifyShareCode(String shareCode, String extractCode) {
        Share share = shareMapper.selectByShareCode(shareCode);
        if (share == null) {
            return Result.error("分享不存在或已被取消");
        }
        if (share.isExpired()) {
            return Result.error("分享已过期");
        }

        // 检查提取码
        if (share.hasPassword()) {
            if (extractCode == null || extractCode.trim().isEmpty()) {
                return Result.error(1001, "请输入提取码");
            }
            if (!share.getSharePwd().equals(extractCode.trim())) {
                return Result.error("提取码错误");
            }
        }

        // 获取文件信息
        FileItem file = fileItemMapper.selectById(share.getFileId());
        if (file == null || file.isDeleted()) {
            return Result.error("文件不存在或已被删除");
        }

        // 增加浏览次数
        shareMapper.incrementViewCount(share.getId());

        return Result.success(file);
    }

    @Override
    public Result<?> cancelShare(Long shareId, Long userId) {
        Share share = shareMapper.selectById(shareId);
        if (share == null) {
            return Result.error("分享不存在");
        }
        if (!share.getUserId().equals(userId)) {
            return Result.error("无权取消此分享");
        }

        // 更新文件分享状态
        fileItemMapper.updateShareStatus(share.getFileId(), 0);
        shareMapper.deleteById(shareId);

        log.info("取消分享: shareId={}", shareId);
        return Result.success("已取消分享");
    }

    @Override
    public Result<List<Share>> getUserShares(Long userId) {
        List<Share> list = shareMapper.selectByUserId(userId);
        return Result.success(list);
    }

    @Override
    public void downloadSharedFile(Long shareId, HttpServletResponse response) {
        try {
            Share share = shareMapper.selectById(shareId);
            if (share == null || share.isExpired()) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"分享不存在或已过期\"}");
                return;
            }

            FileItem file = fileItemMapper.selectById(share.getFileId());
            if (file == null || file.isDeleted()) {
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

            // 增加下载次数
            shareMapper.incrementDownloadCount(share.getId());

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(file.getFileName(), "UTF-8") + "\"");
            response.setContentLengthLong(physicalFile.length());

            try (FileInputStream fis = new FileInputStream(physicalFile);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            log.info("分享文件下载: shareCode={}, fileName={}", share.getShareCode(), file.getFileName());
        } catch (IOException e) {
            log.error("分享文件下载失败", e);
        }
    }

    @Override
    public List<Share> getAllShares() {
        return shareMapper.selectByUserId(null); // 注意：这里可能需要一个查询所有的方法
    }

    @Override
    public Result<?> adminDeleteShare(Long shareId) {
        Share share = shareMapper.selectById(shareId);
        if (share != null) {
            fileItemMapper.updateShareStatus(share.getFileId(), 0);
            shareMapper.deleteById(shareId);
        }
        return Result.success("分享已删除");
    }
}
