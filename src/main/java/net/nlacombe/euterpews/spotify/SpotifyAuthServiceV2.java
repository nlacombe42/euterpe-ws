package net.nlacombe.euterpews.spotify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrapper.spotify.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class SpotifyAuthServiceV2 {

    private final ObjectMapper jsonConverter;

    private final String spotifyAuthClientId;
    private final String spotifyAuthClientSecret;

    public SpotifyAuthServiceV2(
        @Value("${spotify.auth.clientId}") String spotifyAuthClientId,
        @Value("${spotify.auth.clientSecret}") String spotifyAuthClientSecret
    ) {
        this.jsonConverter = new ObjectMapper();
        this.spotifyAuthClientId = spotifyAuthClientId;
        this.spotifyAuthClientSecret = spotifyAuthClientSecret;
    }

    public String getSpotifyAccessToken(String code) {
        try {
            var requestBody = "code=" + code;
            requestBody += "&redirect_uri=" + getSpotifyAuthRedirectUri();
            requestBody += "&grant_type=authorization_code";

            var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://accounts.spotify.com/api/token"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Accept", "application/json; charset=UTF-8")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Authorization","Basic " + Base64.encode((spotifyAuthClientId + ':' + spotifyAuthClientSecret).getBytes(StandardCharsets.UTF_8)))
                .build();

            var httpClient = HttpClient.newHttpClient();
            var responseJson = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()).body();
            var spotifyOauthAccessTokenResponse = jsonConverter.readValue(responseJson, SpotifyOauthAccessTokenResponse.class);

            return spotifyOauthAccessTokenResponse.getAccess_token();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error calling spotify to get access token", e);
        }
    }

    public SpotifyOauthFrontendConfig getSpotifyOauthFrontendConfig() {
        return new SpotifyOauthFrontendConfig(spotifyAuthClientId, getSpotifyAuthRedirectUri());
    }

    private String getSpotifyAuthRedirectUri() {
        return "http://localhost:8008/api/v1/spotifyOauthRedirectUri";
    }
}
