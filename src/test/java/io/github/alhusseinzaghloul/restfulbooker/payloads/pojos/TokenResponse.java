package io.github.alhusseinzaghloul.restfulbooker.payloads.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Body returned by POST /auth: token on success, reason on bad credentials. accessToken/expiresIn: auth.type=bearer only. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TokenResponse(String token,
                            String reason,
                            @JsonProperty("access_token") String accessToken,
                            @JsonProperty("expires_in") Long expiresIn) {}
