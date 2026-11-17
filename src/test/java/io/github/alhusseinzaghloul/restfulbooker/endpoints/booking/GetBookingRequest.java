package io.github.alhusseinzaghloul.restfulbooker.endpoints.booking;

import io.github.alhusseinzaghloul.restfulbooker.client.BaseApiRequest;
import io.restassured.http.Method;

/** GET /booking/{id} */
public final class GetBookingRequest extends BaseApiRequest {

    public GetBookingRequest(int bookingId) {
        super(Method.GET, "/booking/{id}");
        pathParam("id", bookingId);
    }

    @Override
    protected boolean requiresAuth() { return false; }
}
