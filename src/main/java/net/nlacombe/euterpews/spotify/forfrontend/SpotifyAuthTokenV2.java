package net.nlacombe.euterpews.spotify.forfrontend;

import java.time.Instant;

public class SpotifyAuthTokenV2 {

    private String accessToken;
    private Instant expiredAt;

    public SpotifyAuthTokenV2(String accessToken, Instant expiredAt) {
        this.accessToken = accessToken;
        this.expiredAt = expiredAt;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }
}
