package ru.hvayon.Gateway.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@RequestMapping("api/v1")
@Controller
public class AuthController {

    @Value("${identity_provider.host}")
    private String IDENTITY_PROVIDER;
    @Value("${oauth-token}")
    private String oauth_token_uri;

    @PostMapping("/authorize")
    public ResponseEntity<Token> authorize(@RequestBody AuthRequest request) {
        String credentials = "rsoi-client:rsoi-client-secret";

        byte[] encodedCredentials = Base64.getEncoder().encode(credentials.getBytes());
        String base64Credentials = new String(encodedCredentials);

        MultiValueMap<String, String> fields = new LinkedMultiValueMap<>();
        fields.add("username", request.getUsername());
        fields.add("password", request.getPassword());
        fields.add("grant_type", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64Credentials);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(fields, headers);

        return new RestTemplate().exchange(IDENTITY_PROVIDER + oauth_token_uri, HttpMethod.POST, entity, Token.class);
    }
}
