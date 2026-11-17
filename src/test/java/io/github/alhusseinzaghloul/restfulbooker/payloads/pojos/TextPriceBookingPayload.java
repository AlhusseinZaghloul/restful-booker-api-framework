package io.github.alhusseinzaghloul.restfulbooker.payloads.pojos;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Wrong-type negative (O5): totalprice sent as text. A typed DTO, not a raw string (Rule 2.12). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TextPriceBookingPayload(String firstname,
                                      String lastname,
                                      String totalprice,
                                      Boolean depositpaid,
                                      BookingDates bookingdates,
                                      String additionalneeds) {}
