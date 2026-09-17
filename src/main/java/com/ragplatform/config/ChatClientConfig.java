package com.ragplatform.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Two independent ChatClients: "primary" hits NVIDIA (autoconfigured from spring.ai.openai.* in
 * application.yml), "fallback" hits a free OpenRouter model. ResilientChatService tries primary
 * first and only falls back on failure/timeout - this is what turns "calls an LLM" into something
 * that survives a rate-limited free-tier model having a bad five minutes.
 */
@Configuration
public class ChatClientConfig {

    @Value("${app.openrouter.api-key}")
    private String openRouterApiKey;

    @Value("${app.openrouter.base-url}")
    private String openRouterBaseUrl;

    @Value("${app.openrouter.fallback-chat-model}")
    private String fallbackModel;

    @Bean
    @Qualifier("primaryChatClient")
    public ChatClient primaryChatClient(ChatModel chatModel) {
        // 'chatModel' here is the auto-configured OpenAiChatModel pointed at NVIDIA
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Qualifier("fallbackChatClient")
    public ChatClient fallbackChatClient() {
        OpenAiApi api = OpenAiApi.builder()
                .apiKey(openRouterApiKey)
                .baseUrl(openRouterBaseUrl)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(fallbackModel)
                .maxTokens(2048)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();

        return ChatClient.builder(chatModel).build();
    }
}
