package io.github.alhusseinzaghloul.restfulbooker.payloads.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One item of the GET /booking list. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingIdResponse(@JsonProperty("bookingid") int bookingId) {}
