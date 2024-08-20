package ru.hvayon.Gateway.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TokenResponse {
    @JsonProperty(value = "access_token")
    private String accessToken;
}