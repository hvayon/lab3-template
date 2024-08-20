package ru.hvayon.Gateway.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

public class AuthService {

    @Value("${identity_provider.host}")
    private String IDENTITY_PROVIDER;
    @Value("${oauth-token}")
    private String oauth_token_uri;

    @Value("${client-id}")
    private String clientId;
    @Value("${client-secret}")
    private String clientSecret;

    @Value("${grant-type}")
    private String grantType;

//    void authorize(String username, String password) {
//        String credentials = clientId + ":" + clientSecret;
//
//        byte[] encodedCredentials = Base64.getEncoder().encode(credentials.getBytes());
//        String base64Credentials = new String(encodedCredentials);
//
//        MultiValueMap<String, String> fields = new LinkedMultiValueMap<>();
//        fields.add("username", username);
//        fields.add("password", password);
//        fields.add("grant_type", grantType);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Authorization", "Basic " + base64Credentials);
//
//        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(fields, headers);
//
//        return new RestTemplate().exchange(IDENTITY_PROVIDER + oauth_token_uri, HttpMethod.POST, entity, Token.class);
//    }
}
