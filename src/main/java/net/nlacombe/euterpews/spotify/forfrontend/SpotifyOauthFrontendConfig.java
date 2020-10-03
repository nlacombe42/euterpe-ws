package net.nlacombe.euterpews.spotify.forfrontend;

public class SpotifyOauthFrontendConfig {
    private final String spotifyAuthClientId;
    private final String spotifyAuthRedirectUri;

    public SpotifyOauthFrontendConfig(String spotifyAuthClientId, String spotifyAuthRedirectUri) {
        this.spotifyAuthClientId = spotifyAuthClientId;
        this.spotifyAuthRedirectUri = spotifyAuthRedirectUri;
    }

    public String getSpotifyAuthClientId() {
        return spotifyAuthClientId;
    }

    public String getSpotifyAuthRedirectUri() {
        return spotifyAuthRedirectUri;
    }
}
