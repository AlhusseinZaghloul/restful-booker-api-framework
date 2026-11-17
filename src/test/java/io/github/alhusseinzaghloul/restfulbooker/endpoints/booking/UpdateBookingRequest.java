package io.github.alhusseinzaghloul.restfulbooker.endpoints.booking;

import io.github.alhusseinzaghloul.restfulbooker.client.BaseApiRequest;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.BookingPayload;
import io.restassured.http.Method;

/** PUT /booking/{id} */
public final class UpdateBookingRequest extends BaseApiRequest {

    public UpdateBookingRequest(int bookingId, BookingPayload booking) {
        super(Method.PUT, "/booking/{id}");
        pathParam("id", bookingId);
        body(booking);
    }

    /** O5: send this token instead of the TokenManager token. */
    public UpdateBookingRequest withToken(String token) {
        tokenOverride(token);
        return this;
    }
}
