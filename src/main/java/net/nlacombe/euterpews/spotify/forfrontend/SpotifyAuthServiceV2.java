package net.nlacombe.euterpews.spotify.forfrontend;

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
import java.time.Instant;

@Service
public class SpotifyAuthServiceV2 {

    private final ObjectMapper jsonConverter;

    private final String spotifyAuthClientId;
    private final String spotifyAuthClientSecret;
    private final String spotifyAuthRedirectUri;

    public SpotifyAuthServiceV2(
        @Value("${spotify.auth.clientId}") String spotifyAuthClientId,
        @Value("${spotify.auth.clientSecret}") String spotifyAuthClientSecret,
        @Value("${spotify.auth.redirectUri}") String spotifyAuthRedirectUri
    ) {
        this.jsonConverter = new ObjectMapper();
        this.spotifyAuthClientId = spotifyAuthClientId;
        this.spotifyAuthClientSecret = spotifyAuthClientSecret;
        this.spotifyAuthRedirectUri = spotifyAuthRedirectUri;
    }

    public SpotifyAuthTokenV2 getSpotifyAuthToken(String code) {
        try {
            var requestBody = "code=" + code;
            requestBody += "&redirect_uri=" + spotifyAuthRedirectUri;
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

            var tokenExpiry = Instant.now().plusSeconds(spotifyOauthAccessTokenResponse.getExpires_in() - 1);

            return new SpotifyAuthTokenV2(spotifyOauthAccessTokenResponse.getAccess_token(), tokenExpiry);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error calling spotify to get access token", e);
        }
    }

    public SpotifyOauthFrontendConfig getSpotifyOauthFrontendConfig() {
        return new SpotifyOauthFrontendConfig(spotifyAuthClientId, spotifyAuthRedirectUri);
    }
}
