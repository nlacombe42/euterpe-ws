package net.nlacombe.euterpe.spotify;

import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;

public class SpotifyAuthToken {

    private String accessToken;
    private String refreshToken;
    private String tokenType;

    public SpotifyAuthToken() {
    }

    public SpotifyAuthToken(AuthorizationCodeCredentials authorizationCodeCredentials) {
        this.accessToken = authorizationCodeCredentials.getAccessToken();
        this.refreshToken = authorizationCodeCredentials.getRefreshToken();
        this.tokenType = authorizationCodeCredentials.getTokenType();
    }

    public AuthorizationCodeCredentials toAuthorizationCodeCredentials() {
        return new AuthorizationCodeCredentials.Builder()
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setTokenType(tokenType)
                .build();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
