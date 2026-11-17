package io.github.alhusseinzaghloul.restfulbooker.payloads.factories;

/** Raw malformed bodies for CreateBookingRequest.withRawBody(...) (O5). Constants only, no concatenation in tests. */
public final class MalformedPayloads {

    /** Truncated JSON object: the closing fields and brace are missing. */
    public static final String TRUNCATED_BOOKING_JSON = "{\"firstname\":\"QA\",\"lastname\":\"QA-AUTO\",";

    private MalformedPayloads() {}
}
