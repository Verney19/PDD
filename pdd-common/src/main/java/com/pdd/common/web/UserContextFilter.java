package com.pdd.common.web;

import com.pdd.common.security.JwtUser;
import com.pdd.common.security.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 用户上下文过滤器。
 * <p>
 * Gateway 已经把用户信息透传到请求头，这里再把它转成 ThreadLocal，
 * 方便 Controller/Service 通过 UserContext 直接读取当前用户。
 */
public class UserContextFilter extends OncePerRequestFilter {
    /**
     * 从请求头读取用户信息并写入 UserContext，供当前请求链路复用。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String userId = request.getHeader(UserContext.USER_ID_HEADER);
            if (StringUtils.hasText(userId)) {
                String username = request.getHeader(UserContext.USERNAME_HEADER);
                String role = request.getHeader(UserContext.ROLE_HEADER);
                UserContext.set(new JwtUser(Long.valueOf(userId), username, role));
            }
            filterChain.doFilter(request, response);
        } finally {
            // ThreadLocal 必须及时清理，避免线程复用时串用户。
            UserContext.clear();
        }
    }
}
