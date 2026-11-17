package io.github.alhusseinzaghloul.restfulbooker.endpoints.booking;

import io.github.alhusseinzaghloul.restfulbooker.client.BaseApiRequest;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.PartialBookingPayload;
import io.restassured.http.Method;

/** PATCH /booking/{id} */
public final class PartialUpdateBookingRequest extends BaseApiRequest {

    public PartialUpdateBookingRequest(int bookingId, PartialBookingPayload patch) {
        super(Method.PATCH, "/booking/{id}");
        pathParam("id", bookingId);
        body(patch);
    }

    /** O5: send this token instead of the TokenManager token. */
    public PartialUpdateBookingRequest withToken(String token) {
        tokenOverride(token);
        return this;
    }
}
