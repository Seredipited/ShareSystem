package com.sharesystem.file.service;

import com.sharesystem.common.dto.Result;
import com.sharesystem.common.entity.Share;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface ShareService {
    Result<Share> createShare(Long fileId, Long userId, String sharePwd, Integer expireDays);
    Result<Share> getShareByCode(String shareCode);
    Result<?> verifyShareCode(String shareCode, String extractCode);
    Result<?> cancelShare(Long shareId, Long userId);
    Result<List<Share>> getUserShares(Long userId);
    void downloadSharedFile(Long shareId, HttpServletResponse response);
    List<Share> getAllShares();
    Result<?> adminDeleteShare(Long shareId);
}
