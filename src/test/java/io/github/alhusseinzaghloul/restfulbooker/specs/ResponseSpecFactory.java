package io.github.alhusseinzaghloul.restfulbooker.specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;

/** Reusable response preconditions (Rule 2.11). Error bodies are text/plain, so negatives use status(). */
public final class ResponseSpecFactory {

    private ResponseSpecFactory() {}

    public static ResponseSpecification json(int statusCode) {
        return new ResponseSpecBuilder()
                .expectStatusCode(statusCode)
                .expectContentType(ContentType.JSON)
                .build();
    }

    public static ResponseSpecification status(int statusCode) {
        return new ResponseSpecBuilder().expectStatusCode(statusCode).build();
    }
}
