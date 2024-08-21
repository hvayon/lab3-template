package ru.hvayon.Gateway.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AuthRequest {
    @JsonProperty("username")
    String username;
    @JsonProperty("password")
    String password;
}
