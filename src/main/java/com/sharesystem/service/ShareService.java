package com.sharesystem.service;

import com.sharesystem.dto.Result;
import com.sharesystem.entity.Share;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 分享服务接口
 */
public interface ShareService {

    /**
     * 创建分享链接
     */
    Result<Share> createShare(Long fileId, Long userId, String sharePwd, Integer expireDays);

    /**
     * 通过分享码获取分享信息
     */
    Result<Share> getShareByCode(String shareCode);

    /**
     * 验证提取码并获取分享文件信息
     */
    Result<?> verifyShareCode(String shareCode, String extractCode);

    /**
     * 取消分享
     */
    Result<?> cancelShare(Long shareId, Long userId);

    /**
     * 获取用户的分享列表
     */
    Result<List<Share>> getUserShares(Long userId);

    /**
     * 通过分享下载文件
     */
    void downloadSharedFile(Long shareId, HttpServletResponse response);

    /**
     * 管理员 - 获取所有分享列表
     */
    List<Share> getAllShares();

    /**
     * 管理员 - 删除分享
     */
    Result<?> adminDeleteShare(Long shareId);
}
