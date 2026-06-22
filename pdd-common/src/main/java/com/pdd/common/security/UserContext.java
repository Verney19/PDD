package com.pdd.common.security;

/**
 * 当前请求用户上下文。
 * <p>
 * 通过 ThreadLocal 保存当前线程中的用户信息，适合在 Controller/Service 中快速取 userId。
 */
public final class UserContext {
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USERNAME_HEADER = "X-Username";
    public static final String ROLE_HEADER = "X-Role";

    private static final ThreadLocal<JwtUser> HOLDER = new ThreadLocal<>();

    /**
     * 工具类不允许实例化。
     */
    private UserContext() {
    }

    /**
     * 写入当前请求的用户信息。
     */
    public static void set(JwtUser user) {
        HOLDER.set(user);
    }

    /**
     * 获取当前请求的完整用户信息。
     */
    public static JwtUser get() {
        return HOLDER.get();
    }

    /**
     * 获取当前请求的用户 ID。
     */
    public static Long userId() {
        JwtUser user = get();
        return user == null ? null : user.userId();
    }

    public static String role() {
        JwtUser user = get();
        return user == null ? null : user.role();
    }

    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role());
    }

    /**
     * 清理当前线程里的用户上下文。
     */
    public static void clear() {
        HOLDER.remove();
    }
}
