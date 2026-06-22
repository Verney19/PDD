package com.pdd.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pdd.auth.dto.LoginRequest;
import com.pdd.auth.dto.LoginResponse;
import com.pdd.auth.dto.RegisterRequest;
import com.pdd.auth.entity.User;
import com.pdd.auth.mapper.UserMapper;
import com.pdd.common.api.ErrorCode;
import com.pdd.common.exception.BizException;
import com.pdd.common.security.JwtUser;
import com.pdd.common.security.JwtUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 认证服务核心业务。
 * <p>
 * 负责注册、登录、密码摘要和 JWT 签发。
 */
@Service
public class AuthService {
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    /**
     * 注入用户 Mapper 和 JWT 工具。
     */
    public AuthService(UserMapper userMapper, JwtUtils jwtUtils) {
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 注册新用户。
     * <p>
     * 注册成功后立即签发 JWT，返回结构与登录接口保持一致。
     */
    public LoginResponse register(RegisterRequest request) {
        Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, request.username()));
        if (count > 0) {
            throw new BizException(ErrorCode.CONFLICT, "用户名已存在");
        }

        // 注册时只写最基础的用户信息，默认角色为普通用户。
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(hash(request.password()));
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        // 注册成功后直接签发 token，前端可无缝进入登录态。
        String token = jwtUtils.createToken(new JwtUser(user.getId(), user.getUsername(), user.getRole()));
        return new LoginResponse(user.getId(), user.getUsername(), user.getRole(), token);
    }

    /**
     * 用户登录。
     * <p>
     * 校验用户名和密码摘要，成功后签发 JWT。
     */
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, request.username()));
        if (user == null || !hash(request.password()).equals(user.getPassword())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 登录成功重新签发 JWT，不依赖服务端 session。
        String token = jwtUtils.createToken(new JwtUser(user.getId(), user.getUsername(), user.getRole()));
        return new LoginResponse(user.getId(), user.getUsername(), user.getRole(), token);
    }

    /**
     * 对明文密码做摘要。
     */
    private String hash(String raw) {
        // 示例项目里用 MD5 做演示摘要，真实生产环境应使用更安全的密码算法。
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
