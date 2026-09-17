package com.ragplatform.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Retries the primary (NVIDIA) model a couple of times with backoff, then falls back to a
 * secondary (OpenRouter) model if the primary is still failing - e.g. its free-tier rate limit
 * got hit. Deliberately a plain manual retry loop rather than Spring Retry's @Retryable: annotation
 * -based retry is proxy-based AOP, and calling an @Retryable method on `this` from inside the same
 * bean (self-invocation) silently skips the proxy and the retry never actually happens - a classic
 * gotcha. A manual loop sidesteps it entirely and is easier to reason about here anyway.
 */
@Service
public class ResilientChatService {

    private static final Logger log = LoggerFactory.getLogger(ResilientChatService.class);
    private static final int MAX_PRIMARY_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 1000L;

    private final ChatClient primary;
    private final ChatClient fallback;

    public ResilientChatService(
            @Qualifier("primaryChatClient") ChatClient primary,
            @Qualifier("fallbackChatClient") ChatClient fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    public String answer(String systemPrompt, String userMessage) {
        Exception lastFailure = null;

        for (int attempt = 1; attempt <= MAX_PRIMARY_ATTEMPTS; attempt++) {
            try {
                return primary.prompt().system(systemPrompt).user(userMessage).call().content();
            } catch (Exception e) {
                lastFailure = e;
                log.warn("Primary chat model attempt {}/{} failed: {}", attempt, MAX_PRIMARY_ATTEMPTS, e.getMessage());
                sleepQuietly(RETRY_DELAY_MS * attempt);
            }
        }

        log.warn("Primary chat model exhausted retries, falling back to secondary model", lastFailure);
        try {
            return fallback.prompt().system(systemPrompt).user(userMessage).call().content();
        } catch (Exception fallbackFailure) {
            log.error("Fallback chat model also failed: {}", fallbackFailure.getMessage());
            throw new IllegalStateException("Both primary and fallback chat models are unavailable", fallbackFailure);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
