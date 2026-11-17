package io.github.alhusseinzaghloul.restfulbooker.payloads.pojos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/** Body of PATCH /booking/{id}. Every field is optional. */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PartialBookingPayload(String firstname,
                                    String lastname,
                                    Integer totalprice,
                                    Boolean depositpaid,
                                    BookingDates bookingdates,
                                    String additionalneeds) {}
