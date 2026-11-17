package io.github.alhusseinzaghloul.restfulbooker.endpoints.health;

import io.github.alhusseinzaghloul.restfulbooker.client.BaseApiRequest;
import io.restassured.http.Method;

/** GET /ping */
public final class CheckHealthRequest extends BaseApiRequest {

    public CheckHealthRequest() {
        super(Method.GET, "/ping");
    }

    @Override
    protected boolean requiresAuth() { return false; }
}
