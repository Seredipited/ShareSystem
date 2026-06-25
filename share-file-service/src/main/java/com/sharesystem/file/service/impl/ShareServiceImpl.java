package com.sharesystem.file.service.impl;

import com.sharesystem.common.dto.Result;
import com.sharesystem.common.entity.FileItem;
import com.sharesystem.common.entity.Share;
import com.sharesystem.common.util.CodeUtil;
import com.sharesystem.file.mapper.FileItemMapper;
import com.sharesystem.file.mapper.ShareMapper;
import com.sharesystem.file.service.ShareService;
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
        if (file == null) return Result.error("文件不存在");
        if (!file.getUserId().equals(userId)) return Result.error("无权分享此文件");

        // 已分享则返回已有
        Share existing = shareMapper.selectByFileId(fileId);
        if (existing != null) return Result.success(existing);

        Share share = Share.builder()
                .fileId(fileId).userId(userId)
                .shareCode(CodeUtil.generateShareCode())
                .sharePwd(sharePwd != null && !sharePwd.trim().isEmpty() ? sharePwd : null)
                .expireTime(expireDays != null && expireDays > 0
                        ? LocalDateTime.now().plusDays(expireDays) : null)
                .viewCount(0).downloadCount(0).build();

        shareMapper.insert(share);
        fileItemMapper.updateShareStatus(fileId, 1);
        return Result.success("分享创建成功", share);
    }

    @Override
    public Result<Share> getShareByCode(String shareCode) {
        Share share = shareMapper.selectByShareCode(shareCode);
        if (share == null) return Result.error("分享不存在或已被取消");
        if (share.isExpired()) return Result.error("分享已过期");
        shareMapper.incrementViewCount(share.getId());
        share.setViewCount(share.getViewCount() + 1);
        if (share.hasPassword()) share.setSharePwd(null); // 不返回提取码
        return Result.success(share);
    }

    @Override
    public Result<?> verifyShareCode(String shareCode, String extractCode) {
        Share share = shareMapper.selectByShareCode(shareCode);
        if (share == null) return Result.error("分享不存在");
        if (share.isExpired()) return Result.error("分享已过期");
        if (share.hasPassword()) {
            if (extractCode == null || !extractCode.equals(share.getSharePwd()))
                return Result.error("提取码错误");
        }
        FileItem file = fileItemMapper.selectById(share.getFileId());
        if (file == null) return Result.error("文件不存在");
        shareMapper.incrementDownloadCount(share.getId());
        return Result.success(file);
    }

    @Override
    public Result<?> cancelShare(Long shareId, Long userId) {
        Share share = shareMapper.selectById(shareId);
        if (share == null) return Result.error("分享不存在");
        if (!share.getUserId().equals(userId)) return Result.error("无权取消此分享");
        fileItemMapper.updateShareStatus(share.getFileId(), 0);
        shareMapper.deleteById(shareId);
        return Result.success("已取消分享");
    }

    @Override
    public Result<List<Share>> getUserShares(Long userId) {
        return Result.success(shareMapper.selectByUserId(userId));
    }

    @Override
    public void downloadSharedFile(Long shareId, HttpServletResponse response) {
        try {
            Share share = shareMapper.selectById(shareId);
            if (share == null || share.isExpired()) { response.sendError(404); return; }
            FileItem file = fileItemMapper.selectById(share.getFileId());
            if (file == null || file.isFolder()) { response.sendError(404); return; }

            File physicalFile = new File(file.getFilePath());
            if (!physicalFile.exists()) { response.sendError(404); return; }

            shareMapper.incrementDownloadCount(share.getId());
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(file.getFileName(), "UTF-8") + "\"");
            response.setHeader("Accept-Ranges", "bytes");

            long fileLength = physicalFile.length();
            response.setContentLengthLong(fileLength);

            try (FileInputStream fis = new FileInputStream(physicalFile);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) os.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) { log.error("分享下载失败", e); }
    }

    @Override
    public List<Share> getAllShares() {
        return shareMapper.selectList(null);
    }

    @Override
    public Result<?> adminDeleteShare(Long shareId) {
        Share share = shareMapper.selectById(shareId);
        if (share != null) {
            fileItemMapper.updateShareStatus(share.getFileId(), 0);
            shareMapper.deleteById(shareId);
        }
        return Result.success("删除成功");
    }
}
