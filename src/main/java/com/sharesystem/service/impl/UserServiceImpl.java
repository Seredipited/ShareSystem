package com.sharesystem.service.impl;

import com.sharesystem.dto.LoginDTO;
import com.sharesystem.dto.RegisterDTO;
import com.sharesystem.dto.Result;
import com.sharesystem.entity.User;
import com.sharesystem.entity.UserOauth;
import com.sharesystem.mapper.UserMapper;
import com.sharesystem.mapper.UserOauthMapper;
import com.sharesystem.service.UserService;
import com.sharesystem.util.CodeUtil;
import com.sharesystem.util.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 用户服务实现
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserOauthMapper oauthMapper;

    @Value("${file.upload.basePath}")
    private String uploadBasePath;

    // QQ互联AppID和AppKey（实际项目中应配置在配置文件中）
    private static final String QQ_APP_ID = "your_qq_app_id";
    private static final String QQ_APP_KEY = "your_qq_app_key";
    private static final String QQ_REDIRECT_URI = "http://localhost:8080/ShareSystem/api/user/qq/callback";

    @Override
    public Result<?> register(RegisterDTO dto) {
        // 参数校验
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (dto.getPassword().length() < 6) {
            return Result.error("密码长度不能少于6位");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return Result.error("两次密码输入不一致");
        }

        // 检查用户名是否已存在
        User existUser = userMapper.selectByUsername(dto.getUsername().trim());
        if (existUser != null) {
            return Result.error("用户名已被注册");
        }

        // 创建用户
        User user = User.builder()
                .username(dto.getUsername().trim())
                .password(MD5Util.md5(dto.getPassword()))
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .nickname(dto.getNickname() != null ? dto.getNickname() : CodeUtil.generateNickname())
                .role(0)
                .storageUsed(0L)
                .storageMax(1073741824L) // 默认1GB
                .status(1)
                .build();

        userMapper.insert(user);
        log.info("用户注册成功: {}", user.getUsername());
        return Result.success("注册成功");
    }

    @Override
    public Result<User> login(LoginDTO dto, HttpServletRequest request) {
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空");
        }

        User user = userMapper.selectByUsername(dto.getUsername().trim());
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (user.getStatus() == 0) {
            return Result.error("账号已被禁用，请联系管理员");
        }
        if (!MD5Util.md5(dto.getPassword()).equals(user.getPassword())) {
            return Result.error("密码错误");
        }

        // 更新最后登录时间
        userMapper.updateLoginTime(user.getId());
        user.setLastLoginTime(LocalDateTime.now());

        // 设置Session
        HttpSession session = request.getSession(true);
        session.setAttribute("currentUser", user);
        session.setMaxInactiveInterval(30 * 60); // 30分钟

        // 不返回密码
        user.setPassword(null);
        log.info("用户登录成功: {}", user.getUsername());
        return Result.success("登录成功", user);
    }

    @Override
    public Result<?> logout(HttpSession session) {
        if (session != null) {
            User user = (User) session.getAttribute("currentUser");
            if (user != null) {
                log.info("用户退出: {}", user.getUsername());
            }
            session.invalidate();
        }
        return Result.success("已退出登录");
    }

    @Override
    public Result<String> getQQLoginUrl() {
        // 构建QQ授权URL
        String state = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String authUrl = "https://graph.qq.com/oauth2.0/authorize?" +
                "response_type=code&" +
                "client_id=" + QQ_APP_ID + "&" +
                "redirect_uri=" + QQ_REDIRECT_URI + "&" +
                "state=" + state + "&" +
                "scope=get_user_info";
        return Result.success(authUrl);
    }

    @Override
    public Result<User> qqLoginCallback(String code, String state, HttpServletRequest request) {
        try {
            // 1. 用code换取access_token
            String accessToken = getQQAccessToken(code);

            // 2. 用access_token获取openid
            String openId = getQQOpenId(accessToken);

            // 3. 用access_token和openid获取用户信息
            QQUserInfo qqUserInfo = getQQUserInfo(accessToken, openId);

            // 4. 查找是否已有绑定的用户
            UserOauth oauth = oauthMapper.selectByPlatformAndOpenId("qq", openId);

            User user;
            if (oauth != null) {
                // 已有绑定，直接登录
                user = userMapper.selectById(oauth.getUserId());
                if (user == null || user.getStatus() == 0) {
                    return Result.error("用户不存在或已被禁用");
                }
                // 更新token
                oauth.setAccessToken(accessToken);
                oauthMapper.update(oauth);
            } else {
                // 新用户，创建账号并绑定
                user = User.builder()
                        .username("qq_" + openId.substring(0, 12))
                        .password(MD5Util.md5(openId))
                        .nickname(qqUserInfo.nickname != null ? qqUserInfo.nickname : CodeUtil.generateNickname())
                        .avatar(qqUserInfo.avatar)
                        .role(0)
                        .storageUsed(0L)
                        .storageMax(1073741824L)
                        .status(1)
                        .build();
                userMapper.insert(user);

                // 创建OAuth绑定
                UserOauth newOauth = UserOauth.builder()
                        .userId(user.getId())
                        .platform("qq")
                        .openId(openId)
                        .accessToken(accessToken)
                        .nickname(qqUserInfo.nickname)
                        .avatar(qqUserInfo.avatar)
                        .build();
                oauthMapper.insert(newOauth);
            }

            // 更新登录时间
            userMapper.updateLoginTime(user.getId());
            user.setLastLoginTime(LocalDateTime.now());

            // 设置Session
            HttpSession session = request.getSession(true);
            session.setAttribute("currentUser", user);

            user.setPassword(null);
            log.info("QQ登录成功: {}", user.getUsername());
            return Result.success("QQ登录成功", user);

        } catch (Exception e) {
            log.error("QQ登录失败", e);
            return Result.error("QQ登录失败: " + e.getMessage());
        }
    }

    /**
     * 通过code换取QQ access_token
     */
    private String getQQAccessToken(String code) throws IOException {
        // 模拟QQ OAuth流程 - 实际项目中调用QQ互联API
        // GET https://graph.qq.com/oauth2.0/token
        // 参数: grant_type=authorization_code, client_id, client_secret, code, redirect_uri, fmt=json
        // 返回: access_token=xxx&expires_in=7776000&refresh_token=xxx
        return "mock_access_token_" + System.currentTimeMillis();
    }

    /**
     * 通过access_token获取QQ openid
     */
    private String getQQOpenId(String accessToken) throws IOException {
        // 模拟获取openid - 实际项目中调用QQ互联API
        // GET https://graph.qq.com/oauth2.0/me?access_token=xxx&fmt=json
        return "mock_openid_" + System.currentTimeMillis();
    }

    /**
     * 获取QQ用户信息
     */
    private QQUserInfo getQQUserInfo(String accessToken, String openId) throws IOException {
        // 模拟获取用户信息 - 实际项目中调用QQ互联API
        // GET https://graph.qq.com/user/get_user_info
        // 参数: access_token, oauth_consumer_key, openid
        QQUserInfo info = new QQUserInfo();
        info.nickname = "QQ用户" + System.currentTimeMillis() % 10000;
        info.avatar = null;
        return info;
    }

    private static class QQUserInfo {
        String nickname;
        String avatar;
    }

    @Override
    public User getCurrentUser(HttpSession session) {
        if (session != null) {
            return (User) session.getAttribute("currentUser");
        }
        return null;
    }

    @Override
    public Result<?> changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!MD5Util.md5(oldPassword).equals(user.getPassword())) {
            return Result.error("原密码错误");
        }
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("新密码长度不能少于6位");
        }
        user.setPassword(MD5Util.md5(newPassword));
        userMapper.update(user);
        log.info("用户修改密码: {}", user.getUsername());
        return Result.success("密码修改成功");
    }

    @Override
    public Result<?> updateProfile(User user) {
        User dbUser = userMapper.selectById(user.getId());
        if (dbUser == null) {
            return Result.error("用户不存在");
        }
        dbUser.setNickname(user.getNickname());
        dbUser.setEmail(user.getEmail());
        dbUser.setPhone(user.getPhone());
        if (user.getAvatar() != null) {
            dbUser.setAvatar(user.getAvatar());
        }
        userMapper.update(dbUser);
        return Result.success("更新成功", dbUser);
    }

    @Override
    public Result<String> uploadAvatar(Long userId, byte[] avatarData, String originalName) {
        try {
            String ext = originalName.substring(originalName.lastIndexOf('.'));
            String avatarName = "avatar_" + userId + "_" + System.currentTimeMillis() + ext;
            String avatarDir = uploadBasePath + "avatars";
            File dir = new File(avatarDir);
            if (!dir.exists()) dir.mkdirs();

            String avatarPath = avatarDir + File.separator + avatarName;
            try (FileOutputStream fos = new FileOutputStream(avatarPath)) {
                fos.write(avatarData);
            }

            String avatarUrl = "/api/user/avatar/" + avatarName;
            User user = userMapper.selectById(userId);
            user.setAvatar(avatarUrl);
            userMapper.update(user);
            return Result.success("上传成功", avatarUrl);
        } catch (IOException e) {
            log.error("头像上传失败", e);
            return Result.error("头像上传失败");
        }
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }

    @Override
    public List<User> searchUsers(String keyword) {
        return userMapper.searchUsers(keyword);
    }

    @Override
    public Result<?> updateUserStatus(Long userId, Integer status) {
        userMapper.updateStatus(userId, status);
        log.info("更新用户状态: userId={}, status={}", userId, status);
        return Result.success("状态更新成功");
    }

    @Override
    public Result<?> allocateStorage(Long userId, Long storageMax) {
        userMapper.updateStorageMax(userId, storageMax);
        log.info("分配存储空间: userId={}, storageMax={}", userId, storageMax);
        return Result.success("空间分配成功");
    }
}
