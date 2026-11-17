package io.github.alhusseinzaghloul.restfulbooker.endpoints.booking;

import io.github.alhusseinzaghloul.restfulbooker.client.BaseApiRequest;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.BookingPayload;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.TextPriceBookingPayload;
import io.restassured.http.Method;
import io.restassured.response.Response;

/** POST /booking */
public final class CreateBookingRequest extends BaseApiRequest {

    public CreateBookingRequest(BookingPayload booking) {
        this((Object) booking);
    }

    /** Wrong-type negative (O5). */
    public CreateBookingRequest(TextPriceBookingPayload booking) {
        this((Object) booking);
    }

    private CreateBookingRequest(Object body) {
        super(Method.POST, "/booking");
        body(body);
    }

    /** Malformed-JSON negative (O5). Pass a constant from MalformedPayloads. */
    public static CreateBookingRequest withRawBody(String rawJson) {
        return new CreateBookingRequest((Object) rawJson);
    }

    @Override
    protected boolean requiresAuth() { return false; }

    @Override
    protected BaseApiRequest cleanupFor(Response response) {
        Integer id = response.jsonPath().get("bookingid");
        return id == null ? null : new DeleteBookingRequest(id);
    }
}
