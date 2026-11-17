package io.github.alhusseinzaghloul.restfulbooker.payloads.pojos;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Body of POST /auth. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CredentialsPayload(String username, String password) {}
