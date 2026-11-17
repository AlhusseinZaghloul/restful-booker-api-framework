package io.github.alhusseinzaghloul.restfulbooker.endpoints.booking;

import io.github.alhusseinzaghloul.restfulbooker.client.BaseApiRequest;
import io.restassured.http.Method;

/** GET /booking with optional filters (Rule 2.7). */
public final class GetBookingIdsRequest extends BaseApiRequest {

    public GetBookingIdsRequest() {
        super(Method.GET, "/booking");
    }

    public GetBookingIdsRequest firstname(String firstname) {
        queryParam("firstname", firstname);
        return this;
    }

    public GetBookingIdsRequest lastname(String lastname) {
        queryParam("lastname", lastname);
        return this;
    }

    /** ISO yyyy-MM-dd. Actual semantics: checkin &gt; date (RB-DEF-006). */
    public GetBookingIdsRequest checkin(String isoDate) {
        queryParam("checkin", isoDate);
        return this;
    }

    /** ISO yyyy-MM-dd. Actual semantics: checkout &lt;= date (RB-DEF-007). */
    public GetBookingIdsRequest checkout(String isoDate) {
        queryParam("checkout", isoDate);
        return this;
    }

    @Override
    protected boolean requiresAuth() { return false; }
}
