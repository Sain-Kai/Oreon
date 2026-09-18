package com.ragplatform.ratelimit;

import com.ragplatform.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Fixed-window rate limiter, keyed per tenant per minute, backed by Redis so it works correctly
 * across multiple pods (an in-memory counter would let each pod give the tenant its own separate
 * quota, which defeats the point once you're running more than one replica in Kubernetes).
 * Protects the free-tier NVIDIA/OpenRouter quotas from being burned by one noisy tenant.
 */
public class RedisRateLimiterFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final int requestsPerMinute;

    public RedisRateLimiterFilter(StringRedisTemplate redisTemplate, int requestsPerMinute) {
        this.redisTemplate = redisTemplate;
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        UUID tenantId = TenantContext.tenantId();
        // no tenant context yet (e.g. /api/auth/**, /actuator/health) - nothing to rate limit by
        if (tenantId == null || !request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        long currentMinuteBucket = Instant.now().getEpochSecond() / 60;
        String key = "ratelimit:%s:%d".formatted(tenantId, currentMinuteBucket);

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(90));
        }

        if (count != null && count > requestsPerMinute) {
            // 429 Too Many Requests - not defined as a constant on HttpServletResponse (it predates
            // RFC 6585), so the literal code is used instead of a symbol that doesn't exist.
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Rate limit exceeded - max %d requests/minute per tenant\"}".formatted(requestsPerMinute));
            return;
        }

        chain.doFilter(request, response);
    }
}
