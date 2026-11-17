package io.github.alhusseinzaghloul.restfulbooker.payloads.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Booking returned by GET, PUT and PATCH /booking/{id}. totalprice is nullable (RB-DEF-003). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingResponse(String firstname,
                              String lastname,
                              Integer totalprice,
                              Boolean depositpaid,
                              BookingDates bookingdates,
                              String additionalneeds) {}
