package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.dto.MpLoginDTO;
import com.xiaoan.bookstore.entity.User;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.UserMapper;
import com.xiaoan.bookstore.util.JwtUtil;
import com.xiaoan.bookstore.util.WxUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final WxUtil wxUtil;

    @Value("${app.upload.path}")
    private String uploadPath;

    public Map<String, Object> login(MpLoginDTO dto) {
        String openid = wxUtil.getOpenid(dto.getCode());
        if (openid == null || openid.isEmpty()) {
            // 开发模式：使用固定的 openid 避免每次 code 不同导致创建新用户
            openid = "dev_default_user";
            log.warn("微信登录获取openid失败，使用开发模式固定openid: {}", openid);
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getOpenid, openid)
        );
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(dto.getNickname() != null ? dto.getNickname() : "书友");
            user.setAvatar(dto.getAvatar() != null ? dto.getAvatar() : "");
            user.setStatus(Constants.STATUS_ENABLED);
            userMapper.insert(user);
            log.info("新用户注册: openid={}", openid);
        }

        String token = jwtUtil.generateToken(user.getId(), Constants.USER_TYPE_MP);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());
        return result;
    }

    public User getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            // 用户不存在，返回 401 让前端重新登录（会自动注册）
            throw new BusinessException(401, "用户不存在，请重新登录");
        }
        user.setOpenid(null); // 隐藏敏感信息
        return user;
    }

    public void updateProfile(Long userId, String nickname, String avatar) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在，请重新登录");
        }
        if (nickname != null) user.setNickname(nickname);
        if (avatar != null) user.setAvatar(avatar);
        userMapper.updateById(user);
    }

    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择头像文件");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 只允许图片格式
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只支持图片格式");
        }

        // 限制大小 2MB
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("头像大小不能超过2MB");
        }

        try {
            // 保存到 avatars 目录
            String filename = UUID.randomUUID().toString() + ext;
            Path avatarDir = Paths.get(uploadPath, "avatars");
            Files.createDirectories(avatarDir);
            Path filePath = avatarDir.resolve(filename);
            file.transferTo(filePath.toFile());

            // 返回相对路径，前端需要拼接完整 URL
            String avatarUrl = "/uploads/avatars/" + filename;

            // 更新用户头像
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setAvatar(avatarUrl);
                userMapper.updateById(user);
            }

            return avatarUrl;
        } catch (IOException e) {
            log.error("头像上传失败", e);
            throw new BusinessException("头像上传失败");
        }
    }
}
