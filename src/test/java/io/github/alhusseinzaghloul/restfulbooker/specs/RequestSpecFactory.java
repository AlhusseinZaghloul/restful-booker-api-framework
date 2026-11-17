package io.github.alhusseinzaghloul.restfulbooker.specs;

import io.github.alhusseinzaghloul.restfulbooker.client.ApiClientFactory;
import io.github.alhusseinzaghloul.restfulbooker.client.DiagnosticFilter;
import io.github.alhusseinzaghloul.restfulbooker.utils.EnvironmentConfig;
import io.github.alhusseinzaghloul.restfulbooker.utils.TokenManager;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/** Shared request defaults (Rule 2.10) and auth per auth.type (O4). */
public final class RequestSpecFactory {

    private static final String ACCEPT_JSON = "application/json";

    private RequestSpecFactory() {}

    public static RequestSpecification anonymous() {
        return new RequestSpecBuilder()
                .setBaseUri(EnvironmentConfig.get("base.uri"))
                .setBasePath(EnvironmentConfig.get("base.path"))
                .setContentType(ContentType.JSON)
                // O21: exactly "application/json". ContentType.JSON sends four media types, and
                // restful-booker answers any multi-value Accept with HTTP 418.
                .setAccept(ACCEPT_JSON)
                .setConfig(ApiClientFactory.config())
                .addFilter(new DiagnosticFilter())
                .build();
    }

    public static RequestSpecification authenticated() {
        return "none".equals(authType()) ? anonymous() : withToken(TokenManager.getToken());
    }

    /** anonymous() plus the credential for auth.type. Used for normal calls and for O5 negative tokens. */
    public static RequestSpecification withToken(String token) {
        RequestSpecBuilder builder = new RequestSpecBuilder().addRequestSpecification(anonymous());
        switch (authType()) {
            case "cookie-token" -> builder.addCookie("token", token);
            case "bearer" -> builder.addHeader("Authorization", "Bearer " + token);
            default -> throw new IllegalStateException(
                    "withToken() needs auth.type cookie-token|bearer, got " + authType());
        }
        return builder.build();
    }

    private static String authType() {
        return EnvironmentConfig.get("auth.type", "none");
    }
}
