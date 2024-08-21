package ru.hvayon.Gateway.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@RequestMapping("api")
@RestController
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping(value = "/v1/authorize", produces = MediaType.APPLICATION_JSON_VALUE)
    public String authorize(@RequestBody AuthRequest request) {
        return authService.authorize(request.username, request.password);
    }

    @PostMapping(value = "/v1/create/user", produces = MediaType.APPLICATION_JSON_VALUE)
    public String createUser(@RequestBody UserInfoRequest request) {
        return authService.createUser(request);
    }
}
