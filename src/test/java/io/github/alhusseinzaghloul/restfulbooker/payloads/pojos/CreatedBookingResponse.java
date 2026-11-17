package io.github.alhusseinzaghloul.restfulbooker.payloads.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Body returned by POST /booking. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreatedBookingResponse(@JsonProperty("bookingid") int bookingId,
                                     BookingResponse booking) {}
