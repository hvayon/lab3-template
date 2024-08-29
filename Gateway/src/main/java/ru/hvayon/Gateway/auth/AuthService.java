package ru.hvayon.Gateway.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.hvayon.Gateway.request.UserInfoRequest;
import ru.hvayon.Gateway.response.UserInfoResponse;

import java.util.Base64;

@Service
public class AuthService {

    TokenRepository tokenRepository;

    @Value("${identity_provider.host}")
    private String IDENTITY_PROVIDER;
    @Value("${oauth-token}")
    private String oauth_token_uri;

    @Value("${create_user}")
    private String create_user_uri;

    @Value("${client-id}")
    private String clientId;
    @Value("${client-secret}")
    private String clientSecret;

    @Value("${user_info}")
    private String user_uri;

    @Value("${grant-type}")
    private String grantType;

    @Autowired
    public AuthService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public String hashHeaders() {
        String credentials = clientId + ":" + clientSecret;

        byte[] encodedCredentials = Base64.getEncoder().encode(credentials.getBytes());
        return new String(encodedCredentials);
    }

    HttpHeaders setAuthorizationHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + hashHeaders());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    public UserInfoResponse auth(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        System.out.println(authHeader);
        return new RestTemplate().exchange(
                IDENTITY_PROVIDER + user_uri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserInfoResponse.class
        ).getBody();
    }

    public String getAuthToken() {
        return tokenRepository.findAll().get(0).authToken;
    }

    public String authorize(String username, String password) {

        MultiValueMap<String, String> fields = new LinkedMultiValueMap<>();
        fields.add("username", username);
        fields.add("password", password);
        fields.add("grant_type", grantType);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(fields, setAuthorizationHeaders());

        Token token = new RestTemplate().exchange(IDENTITY_PROVIDER + oauth_token_uri, HttpMethod.POST, entity, Token.class).getBody();

        tokenRepository.deleteAll();
        tokenRepository.save(token);
        // для теста, потом удалить
        System.out.println(getAuthToken());
        return "Login success!";
    }

    public String createUser(UserInfoRequest user) {
        this.authorize("user", "100");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + getAuthToken());
        new RestTemplate().postForEntity(
                IDENTITY_PROVIDER + create_user_uri,
                new HttpEntity<>(user, headers),
                String.class).getBody();
        return "New user " + user.getUsername() + " create!";
    }

    public void logout() {
        tokenRepository.deleteAll();
    }
}
