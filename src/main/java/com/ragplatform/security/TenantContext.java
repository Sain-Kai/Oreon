package com.ragplatform.security;

import java.util.UUID;

/**
 * Holds the current request's tenant + user identity, set by JwtAuthFilter once the token is
 * validated. Every tenant-scoped query in the app reads from here instead of trusting client input,
 * so a JWT for tenant A can never be used to read tenant B's documents or query history.
 */
public class TenantContext {

    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> USER_ID = new ThreadLocal<>();

    public static void set(UUID tenantId, UUID userId) {
        TENANT_ID.set(tenantId);
        USER_ID.set(userId);
    }

    public static UUID tenantId() {
        return TENANT_ID.get();
    }

    public static UUID userId() {
        return USER_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
    }
}
