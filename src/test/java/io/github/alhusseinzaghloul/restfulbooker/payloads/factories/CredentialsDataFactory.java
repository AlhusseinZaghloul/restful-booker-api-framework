package io.github.alhusseinzaghloul.restfulbooker.payloads.factories;

import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.CredentialsPayload;
import io.github.alhusseinzaghloul.restfulbooker.utils.EnvironmentConfig;

import java.util.UUID;

/** Credentials come from config and the AUTH_PASSWORD env var, never from code (Rules 3.1, 3.4). */
public final class CredentialsDataFactory {

    /** Deliberately invalid token for the O5 withToken(...) negatives. Tests never write token literals. */
    public static final String INVALID_TOKEN = "invalid-token";

    private CredentialsDataFactory() {}

    public static CredentialsPayload fromConfig() {
        return new CredentialsPayload(EnvironmentConfig.get("auth.username"), EnvironmentConfig.get("auth.password"));
    }

    public static CredentialsPayload invalid() {
        return new CredentialsPayload(EnvironmentConfig.get("auth.username"),
                "wrong-" + UUID.randomUUID().toString().substring(0, 8));
    }
}
