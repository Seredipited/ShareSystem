package com.sharesystem.file.controller;

import com.sharesystem.common.dto.Result;
import com.sharesystem.common.dto.UserDTO;
import com.sharesystem.common.entity.Share;
import com.sharesystem.common.util.JwtUtil;
import com.sharesystem.file.service.ShareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/share")
public class ShareController {

    private static final Logger log = LoggerFactory.getLogger(ShareController.class);

    @Autowired
    private ShareService shareService;

    private Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("X-User-Token");
        if (token != null) {
            UserDTO user = JwtUtil.getUserFromToken(token);
            return user != null ? user.getId() : null;
        }
        return null;
    }

    @PostMapping("/create")
    public Result<Share> createShare(@RequestParam Long fileId,
                                      @RequestParam(required = false) String sharePwd,
                                      @RequestParam(required = false) Integer expireDays,
                                      HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        return shareService.createShare(fileId, userId, sharePwd, expireDays);
    }

    @GetMapping("/info/{shareCode}")
    public Result<Share> getShareInfo(@PathVariable String shareCode) {
        return shareService.getShareByCode(shareCode);
    }

    @PostMapping("/verify")
    public Result<?> verifyShareCode(@RequestParam String shareCode,
                                      @RequestParam(required = false) String extractCode) {
        return shareService.verifyShareCode(shareCode, extractCode);
    }

    @GetMapping("/download/{shareCode}")
    public void downloadSharedFile(@PathVariable String shareCode,
                                    @RequestParam(required = false) String extractCode,
                                    HttpServletResponse response) {
        Result<Share> r = shareService.getShareByCode(shareCode);
        if (r.isSuccess() && r.getData() != null) {
            // verify if needed
            if (r.getData().hasPassword()) {
                if (extractCode == null || !extractCode.equals(r.getData().getSharePwd())) {
                    response.setContentType("application/json;charset=UTF-8");
                    try {
                        response.getWriter().write("{\"code\":500,\"message\":\"提取码错误\"}");
                    } catch (Exception ignored) {}
                    return;
                }
            }
            shareService.downloadSharedFile(r.getData().getId(), response);
        } else {
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":500,\"message\":\"分享不存在或已过期\"}");
            } catch (Exception ignored) {}
        }
    }

    @DeleteMapping("/{shareId}")
    public Result<?> cancelShare(@PathVariable Long shareId, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        return shareService.cancelShare(shareId, userId);
    }

    @GetMapping("/my")
    public Result<List<Share>> getMyShares(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        return shareService.getUserShares(userId);
    }
}
