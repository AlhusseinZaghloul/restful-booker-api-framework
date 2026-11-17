package io.github.alhusseinzaghloul.restfulbooker.utils;

import io.github.alhusseinzaghloul.restfulbooker.endpoints.auth.CreateTokenRequest;
import io.github.alhusseinzaghloul.restfulbooker.payloads.factories.CredentialsDataFactory;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.TokenResponse;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/** The only place that logs in (Rules 3.13–3.16), per auth.type (O4). */
public final class TokenManager {

    private static final Logger LOG = LoggerFactory.getLogger(TokenManager.class);
    private static final long REFRESH_MARGIN_SECONDS = 30;
    private static String token;
    private static Instant expiresAt = Instant.MIN;

    private TokenManager() {}

    public static synchronized String getToken() {
        if (token == null || Instant.now().isAfter(expiresAt)) {
            if (Allure.getLifecycle().getCurrentExecutableKey().isPresent()) {
                Allure.step("Login (TokenManager)", TokenManager::refresh); // Rule 4.5
            } else {
                refresh(); // first login during the suite-setup sweep: no test to report into
            }
        }
        return token;
    }

    private static void refresh() {
        Response response = new CreateTokenRequest(CredentialsDataFactory.fromConfig()).send();
        int status = response.statusCode();
        TokenResponse login = status == 200 ? response.as(TokenResponse.class) : null;
        String value = login == null ? null : login.token() != null ? login.token() : login.accessToken();

        // restful-booker answers a bad password with HTTP 200 + "reason" (RB-DEF-001), so check the token too
        if (value == null || value.isBlank()) {
            String reason = login != null && login.reason() != null ? ", reason=" + login.reason() : "";
            throw new IllegalStateException("Login failed: HTTP " + status + reason);
        }

        String authType = EnvironmentConfig.get("auth.type", "none");
        token = value;
        expiresAt = "bearer".equals(authType) && login.expiresIn() != null
                ? Instant.now().plusSeconds(login.expiresIn() - REFRESH_MARGIN_SECONDS)
                : Instant.MAX; // cookie-token: no expiry info, cached for the run
        LOG.info("Token refreshed (auth.type={})", authType);
    }
}
