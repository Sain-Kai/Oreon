package com.ragplatform.security;

import java.util.UUID;

public record AuthResponse(String token, UUID tenantId, String username) {
}
