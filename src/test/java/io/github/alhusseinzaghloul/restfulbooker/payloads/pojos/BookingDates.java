package io.github.alhusseinzaghloul.restfulbooker.payloads.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Nested dates shared by booking payloads and responses (O20). ISO yyyy-MM-dd strings. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingDates(String checkin, String checkout) {}
