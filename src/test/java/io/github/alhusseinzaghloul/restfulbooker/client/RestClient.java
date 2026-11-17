package io.github.alhusseinzaghloul.restfulbooker.client;

import io.github.alhusseinzaghloul.restfulbooker.specs.RequestSpecFactory;
import io.github.alhusseinzaghloul.restfulbooker.utils.CleanupRegistry;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/** The only class that calls given() (Rule 2.8). */
public final class RestClient {

    private RestClient() {}

    static Response send(BaseApiRequest request) {
        RequestSpecification base = request.tokenOverride() != null
                ? RequestSpecFactory.withToken(request.tokenOverride())
                : request.requiresAuth() ? RequestSpecFactory.authenticated() : RequestSpecFactory.anonymous();

        RequestSpecification spec = given()
                .spec(base)
                .pathParams(request.pathParams())
                .queryParams(request.queryParams())
                .headers(request.headers());

        if (request.body() != null) {
            spec.body(request.body());
        }

        // Outside a test (suite setup: health check, sweep) there is no Allure context to report into
        Response response = reportingActive()
                ? Allure.step(request.toString(), () -> spec.request(request.method(), request.path()))
                : spec.request(request.method(), request.path());

        registerCleanup(request, response);
        return response;
    }

    private static boolean reportingActive() {
        return Allure.getLifecycle().getCurrentExecutableKey().isPresent();
    }

    private static void registerCleanup(BaseApiRequest request, Response response) {
        boolean succeeded = response.statusCode() >= 200 && response.statusCode() < 300;
        if (!succeeded) {
            return;
        }
        BaseApiRequest deleteRequest = request.cleanupFor(response);
        if (deleteRequest != null) {
            CleanupRegistry.register(deleteRequest);
        }
    }
}
