package com.ragplatform.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * NVIDIA's own hosted chat models work fine through Spring AI's default OpenAI-compatible client,
 * but their retrieval-tuned embedding models require an input_type (query vs passage) field the
 * standard OpenAI embeddings request doesn't carry - so spring.ai.model.embedding=none disables the
 * auto-configured embedding bean, and this class wires a second, independent client against
 * OpenRouter purely for embeddings instead. Two providers, one for each model type - a normal
 * production pattern, not a workaround.
 */
@Configuration
public class EmbeddingConfig {

    @Value("${app.openrouter.api-key}")
    private String openRouterApiKey;

    @Value("${app.openrouter.base-url}")
    private String openRouterBaseUrl;

    @Value("${app.openrouter.embedding-model}")
    private String embeddingModel;

    @Bean
    public OpenAiEmbeddingModel embeddingModel() {
        OpenAiApi api = OpenAiApi.builder()
                .apiKey(openRouterApiKey)
                .baseUrl(openRouterBaseUrl)
                .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embeddingModel)
                .build();

        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
    }
}
