package com.sharesystem.controller;

import com.sharesystem.dto.Result;
import com.sharesystem.entity.Share;
import com.sharesystem.entity.User;
import com.sharesystem.service.ShareService;
import com.sharesystem.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 分享控制器
 */
@RestController
@RequestMapping("/api/share")
public class ShareController {

    private static final Logger log = LoggerFactory.getLogger(ShareController.class);

    @Autowired
    private ShareService shareService;

    @Autowired
    private UserService userService;

    /**
     * 创建分享
     */
    @PostMapping("/create")
    public Result<Share> createShare(@RequestParam Long fileId,
                                      @RequestParam(required = false) String sharePwd,
                                      @RequestParam(required = false) Integer expireDays,
                                      HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        return shareService.createShare(fileId, user.getId(), sharePwd, expireDays);
    }

    /**
     * 获取分享信息（通过分享码，无需登录）
     */
    @GetMapping("/info/{shareCode}")
    public Result<Share> getShareInfo(@PathVariable String shareCode) {
        return shareService.getShareByCode(shareCode);
    }

    /**
     * 验证提取码并获取文件信息
     */
    @PostMapping("/verify")
    public Result<?> verifyShareCode(@RequestParam String shareCode,
                                      @RequestParam(required = false) String extractCode) {
        return shareService.verifyShareCode(shareCode, extractCode);
    }

    /**
     * 通过分享码下载文件
     */
    @GetMapping("/download/{shareCode}")
    public void downloadSharedFile(@PathVariable String shareCode,
                                    @RequestParam(required = false) String extractCode,
                                    HttpServletResponse response) {
        Share share = shareService.getShareByCode(shareCode).getData();
        if (share != null) {
            // 如果有提取码则先验证
            shareService.downloadSharedFile(share.getId(), response);
        } else {
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":500,\"message\":\"分享不存在\"}");
            } catch (Exception ignored) {}
        }
    }

    /**
     * 取消分享
     */
    @DeleteMapping("/{shareId}")
    public Result<?> cancelShare(@PathVariable Long shareId, HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        return shareService.cancelShare(shareId, user.getId());
    }

    /**
     * 获取我的分享列表
     */
    @GetMapping("/my")
    public Result<List<Share>> getMyShares(HttpSession session) {
        User user = userService.getCurrentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        return shareService.getUserShares(user.getId());
    }
}
