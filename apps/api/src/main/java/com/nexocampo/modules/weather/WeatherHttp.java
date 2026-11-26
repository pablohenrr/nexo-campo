package com.nexocampo.modules.weather;

import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

final class WeatherHttp {

    private static final Logger log = LoggerFactory.getLogger(WeatherHttp.class);

    private WeatherHttp() {}

    static RestClient client(String baseUrl, Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "NEXO-Campo/1.0")
                .build();
    }

    static String get(RestClient client, String uri, String operation, boolean hideFailureDetail) {
        RestClientException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return client.get().uri(uri).retrieve().body(String.class);
            } catch (RestClientResponseException ex) {
                if (ex.getStatusCode().is4xxClientError()) {
                    logFailure(operation, ex, hideFailureDetail);
                    return null;
                }
                last = ex;
            } catch (RestClientException ex) {
                last = ex;
            }
            sleep(200L * attempt);
        }
        if (last != null) {
            logFailure(operation, last, hideFailureDetail);
        }
        return null;
    }

    private static void logFailure(String operation, Exception ex, boolean hideFailureDetail) {
        if (hideFailureDetail) {
            log.warn("{} tipo={}", operation, ex.getClass().getSimpleName());
            return;
        }
        log.warn("{}", operation, ex);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
