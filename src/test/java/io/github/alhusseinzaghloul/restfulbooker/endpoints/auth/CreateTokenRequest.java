package io.github.alhusseinzaghloul.restfulbooker.endpoints.auth;

import io.github.alhusseinzaghloul.restfulbooker.client.BaseApiRequest;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.CredentialsPayload;
import io.github.alhusseinzaghloul.restfulbooker.utils.EnvironmentConfig;
import io.restassured.http.Method;

/** POST ${auth.path} */
public final class CreateTokenRequest extends BaseApiRequest {

    public CreateTokenRequest(CredentialsPayload credentials) {
        super(Method.POST, EnvironmentConfig.get("auth.path"));
        body(credentials);
    }

    @Override
    protected boolean requiresAuth() { return false; }
}
