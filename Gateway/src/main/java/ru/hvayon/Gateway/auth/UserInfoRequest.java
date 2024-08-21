package ru.hvayon.Gateway.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserInfoRequest {

    private String username;

    private String password;

    private String email;
}
