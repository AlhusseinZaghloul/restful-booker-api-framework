package io.github.alhusseinzaghloul.restfulbooker.payloads.pojos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/** Body of POST /booking and PUT /booking/{id}. */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingPayload(String firstname,
                             String lastname,
                             Integer totalprice,
                             Boolean depositpaid,
                             BookingDates bookingdates,
                             String additionalneeds) {}
