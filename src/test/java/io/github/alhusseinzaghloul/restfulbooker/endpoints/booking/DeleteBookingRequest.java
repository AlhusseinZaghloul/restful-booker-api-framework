package io.github.alhusseinzaghloul.restfulbooker.endpoints.booking;

import io.github.alhusseinzaghloul.restfulbooker.client.BaseApiRequest;
import io.restassured.http.Method;

/** DELETE /booking/{id} */
public final class DeleteBookingRequest extends BaseApiRequest {

    public DeleteBookingRequest(int bookingId) {
        super(Method.DELETE, "/booking/{id}");
        pathParam("id", bookingId);
    }

    /** O5: send this token instead of the TokenManager token. */
    public DeleteBookingRequest withToken(String token) {
        tokenOverride(token);
        return this;
    }

    /** O12: restful-booker returns 405 for an id that is already gone (RB-DEF-005). */
    @Override
    public boolean isAlreadyGone(int statusCode) {
        return statusCode == 404 || statusCode == 405;
    }
}
