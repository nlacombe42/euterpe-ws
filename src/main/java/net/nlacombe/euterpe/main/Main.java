package net.nlacombe.euterpe.main;

import net.nlacombe.euterpe.spotify.SpotifyAuthService;
import net.nlacombe.euterpe.spotify.SpotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final String SPOTIFY_AUTH_CLIENT_ID = "SPOTIFY_AUTH_CLIENT_ID";
    private static final String SPOTIFY_AUTH_CLIENT_SECRET = "SPOTIFY_AUTH_CLIENT_SECRET";
    private static final String SPOTIFY_AUTH_REDIRECT_URI = "SPOTIFY_AUTH_REDIRECT_URI";

    public static void main(String[] args) {
        var spotifyAuthToken =
                new SpotifyAuthService(SPOTIFY_AUTH_CLIENT_ID, SPOTIFY_AUTH_CLIENT_SECRET, SPOTIFY_AUTH_REDIRECT_URI)
                .getSpotifyAuthToken();

        var spotifyService = new SpotifyService(spotifyAuthToken);
        spotifyService.getCurrentUserPlaylist()
                .forEach(playlist -> logger.info("playlist: " + playlist.getName()));
    }
}
