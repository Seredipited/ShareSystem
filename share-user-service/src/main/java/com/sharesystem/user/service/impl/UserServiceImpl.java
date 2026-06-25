package com.sharesystem.user.service.impl;

import com.sharesystem.common.dto.*;
import com.sharesystem.common.entity.User;
import com.sharesystem.common.entity.UserOauth;
import com.sharesystem.common.util.CodeUtil;
import com.sharesystem.common.util.JwtUtil;
import com.sharesystem.common.util.MD5Util;
import com.sharesystem.user.mapper.UserMapper;
import com.sharesystem.user.mapper.UserOauthMapper;
import com.sharesystem.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserOauthMapper oauthMapper;

    private static final String QQ_REDIRECT_URI = "http://localhost:8080/api/user/qq/callback";

    @Override
    public Result<?> register(RegisterDTO dto) {
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
        User exist = userMapper.selectByUsername(dto.getUsername().trim());
        if (exist != null) {
            return Result.error("用户名已被注册");
        }
        User user = User.builder()
                .username(dto.getUsername().trim())
                .password(MD5Util.md5(dto.getPassword()))
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .nickname(dto.getNickname() != null ? dto.getNickname() : CodeUtil.generateNickname())
                .role(0).storageUsed(0L).storageMax(1073741824L).status(1)
                .build();
        userMapper.insert(user);
        log.info("用户注册成功: {}", user.getUsername());
        return Result.success("注册成功");
    }

    @Override
    public Result<?> login(LoginDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty())
            return Result.error("用户名不能为空");
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty())
            return Result.error("密码不能为空");

        User user = userMapper.selectByUsername(dto.getUsername().trim());
        if (user == null) return Result.error("用户不存在");
        if (user.getStatus() == 0) return Result.error("账号已被禁用");
        if (!MD5Util.md5(dto.getPassword()).equals(user.getPassword()))
            return Result.error("密码错误");

        userMapper.updateLoginTime(user.getId());
        user.setLastLoginTime(LocalDateTime.now());

        // 生成 JWT Token
        UserDTO userDTO = toDto(user);
        String token = JwtUtil.generateToken(userDTO);

        user.setPassword(null);
        log.info("用户登录成功: {}", user.getUsername());

        // 将 token 放入返回数据（通过扩展字段）
        return Result.success(new java.util.HashMap<String, Object>() {{
            put("token", token);
            put("user", user);
        }});
    }

    @Override
    public Result<String> getQQLoginUrl() {
        String state = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String authUrl = "https://graph.qq.com/oauth2.0/authorize?" +
                "response_type=code&" +
                "client_id=your_qq_app_id&" +
                "redirect_uri=" + QQ_REDIRECT_URI + "&" +
                "state=" + state + "&" +
                "scope=get_user_info";
        return Result.success(authUrl);
    }

    @Override
    public Result<?> qqLoginCallback(String code, String state) {
        try {
            // 模拟QQ OAuth流程
            String accessToken = "mock_access_token_" + System.currentTimeMillis();
            String openId = "mock_openid_" + System.currentTimeMillis();

            UserOauth oauth = oauthMapper.selectByPlatformAndOpenId("qq", openId);
            User user;

            if (oauth != null) {
                user = userMapper.selectById(oauth.getUserId());
                if (user == null || user.getStatus() == 0) {
                    return Result.error("用户不存在或已被禁用");
                }
                oauth.setAccessToken(accessToken);
                oauthMapper.updateById(oauth);
            } else {
                user = User.builder()
                        .username("qq_" + openId.substring(0, 12))
                        .password(MD5Util.md5(openId))
                        .nickname("QQ用户" + System.currentTimeMillis() % 10000)
                        .role(0).storageUsed(0L).storageMax(1073741824L).status(1)
                        .build();
                userMapper.insert(user);

                UserOauth newOauth = UserOauth.builder()
                        .userId(user.getId()).platform("qq").openId(openId)
                        .accessToken(accessToken).nickname(user.getNickname())
                        .build();
                oauthMapper.insert(newOauth);
            }

            userMapper.updateLoginTime(user.getId());
            user.setLastLoginTime(LocalDateTime.now());
            user.setPassword(null);
            log.info("QQ登录成功: {}", user.getUsername());

            // 生成JWT Token并放入返回
            UserDTO userDTO = toDto(user);
            String token = JwtUtil.generateToken(userDTO);

            return Result.success(new java.util.HashMap<String, Object>() {{
                put("token", token);
                put("user", user);
            }});
        } catch (Exception e) {
            log.error("QQ登录失败", e);
            return Result.error("QQ登录失败");
        }
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public UserDTO getUserDtoById(Long id) {
        User user = userMapper.selectById(id);
        return user != null ? toDto(user) : null;
    }

    @Override
    public Result<?> changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");
        if (!MD5Util.md5(oldPassword).equals(user.getPassword()))
            return Result.error("原密码错误");
        if (newPassword == null || newPassword.length() < 6)
            return Result.error("新密码长度不能少于6位");
        user.setPassword(MD5Util.md5(newPassword));
        userMapper.updateById(user);
        return Result.success("密码修改成功");
    }

    @Override
    public Result<?> updateProfile(User user) {
        User dbUser = userMapper.selectById(user.getId());
        if (dbUser == null) return Result.error("用户不存在");
        if (user.getNickname() != null) dbUser.setNickname(user.getNickname());
        if (user.getEmail() != null) dbUser.setEmail(user.getEmail());
        if (user.getPhone() != null) dbUser.setPhone(user.getPhone());
        if (user.getAvatar() != null) dbUser.setAvatar(user.getAvatar());
        userMapper.updateById(dbUser);
        return Result.success("更新成功", dbUser);
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
        return Result.success("状态更新成功");
    }

    @Override
    public Result<?> allocateStorage(Long userId, Long storageMax) {
        userMapper.updateStorageMax(userId, storageMax);
        return Result.success("空间分配成功");
    }

    @Override
    public int getUserCount() {
        return userMapper.countActive();
    }

    private UserDTO toDto(User user) {
        return UserDTO.builder()
                .id(user.getId()).username(user.getUsername())
                .nickname(user.getNickname()).avatar(user.getAvatar())
                .role(user.getRole()).status(user.getStatus())
                .storageUsed(user.getStorageUsed()).storageMax(user.getStorageMax())
                .build();
    }
}
