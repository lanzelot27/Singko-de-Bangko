package com.singkodebangko.account.security;

/**
 * ThreadLocal context holding authenticated caller identity extracted from JWT claims or Gateway headers.
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {}

    public static void setContext(Long userId, String email) {
        CONTEXT.set(new UserContext(userId, email));
    }

    public static UserContext getContext() {
        return CONTEXT.get();
    }

    public static Long getCurrentUserId() {
        UserContext ctx = CONTEXT.get();
        return ctx != null ? ctx.userId() : null;
    }

    public static String getCurrentEmail() {
        UserContext ctx = CONTEXT.get();
        return ctx != null ? ctx.email() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public record UserContext(Long userId, String email) {}
}
