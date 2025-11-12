package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAiConfig {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiConfig.class);

    @Value("${openai.api.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    // read API key from properties (we expect application.properties to map this to env var)
    @Value("${openai.api.key:}")
    private String openAiKey;

    @Bean(name = "openAiWebClient")
    public WebClient openAiWebClient() {
        // increase in-memory buffer for large responses
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(openAiBaseUrl)
                .exchangeStrategies(strategies)
                .defaultHeader("Content-Type", "application/json");

        // if key present, add Authorization default header (trim to remove stray whitespace/newline)
        if (openAiKey != null && !openAiKey.isBlank()) {
            String token = openAiKey.trim();
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            // log masked token for debug (never log whole key)
            String masked = token.length() <= 8 ? token + "...[masked]" :
                    token.substring(0, 8) + "...[masked]";
            logger.info("OpenAI API key detected (masked) = {}", masked);
        } else {
            logger.warn("OpenAI API key is empty. calls to OpenAI will fail with 401 unless the key is provided via environment or properties.");
        }

        return builder.build();
    }
}
