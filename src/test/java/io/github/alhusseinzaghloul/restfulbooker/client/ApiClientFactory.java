package io.github.alhusseinzaghloul.restfulbooker.client;

import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;

import java.time.Duration;

/** Builds the REST Assured config. O13: REST Assured 6.x Duration timeouts. */
public final class ApiClientFactory {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10); // Heroku dyno cold start
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private ApiClientFactory() {}

    public static RestAssuredConfig config() {
        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .connectTimeout(CONNECT_TIMEOUT)
                        .readTimeout(READ_TIMEOUT));
    }
}
