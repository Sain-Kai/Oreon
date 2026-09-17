package com.ragplatform.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String tenantName,
        @NotBlank String username,
        @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password
) {
}
