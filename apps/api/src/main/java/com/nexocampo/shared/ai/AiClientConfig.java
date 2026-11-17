package com.nexocampo.shared.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "openai-compatible")
public class AiClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);

    @Bean
    OpenAiCompatibleClient openAiCompatibleClient(
            RestClient.Builder restClientBuilder,
            AiProperties properties,
            ObjectMapper objectMapper) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout() == null ? DEFAULT_READ_TIMEOUT : properties.timeout());
        return new OpenAiCompatibleClient(restClientBuilder.clone().requestFactory(requestFactory), properties, objectMapper);
    }
}
