package net.nlacombe.euterpe.main;

import net.nlacombe.euterpe.googleplaymusic.GooglePlayMusicService;
import net.nlacombe.euterpe.spotify.SpotifyAuthService;
import net.nlacombe.euterpe.spotify.SpotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final String SPOTIFY_AUTH_CLIENT_ID = "SPOTIFY_AUTH_CLIENT_ID";
    private static final String SPOTIFY_AUTH_CLIENT_SECRET = "SPOTIFY_AUTH_CLIENT_SECRET";
    private static final String SPOTIFY_AUTH_REDIRECT_URI = "SPOTIFY_AUTH_REDIRECT_URI";

    public static void main(String[] args) {
        var spotifyAuthToken = new SpotifyAuthService(SPOTIFY_AUTH_CLIENT_ID, SPOTIFY_AUTH_CLIENT_SECRET, SPOTIFY_AUTH_REDIRECT_URI)
                .getSpotifyAuthToken();

        var spotifyService = new SpotifyService(spotifyAuthToken);
        Path songsCsvFilePath = Path.of("/home/nlacombe/tmp/google-play-music-all-songs.csv");

        spotifyService.importLibrarySongs(songsCsvFilePath);
    }

    private static void exportGooglePlayMusicDataToCsv() {
        Path playlistsCsvFilePath = Path.of("/home/nlacombe/tmp/google-play-music-playlists.csv");
        Path allSongsCsvFilePath = Path.of("/home/nlacombe/tmp/google-play-music-all-songs.csv");

        var googlePlayMusicService = new GooglePlayMusicService();

        logger.info("Exporting Google Play Music all library songs to CSV at: " + allSongsCsvFilePath.toAbsolutePath());
        googlePlayMusicService.exportAllSongsToCsv(allSongsCsvFilePath);

        logger.info("Exporting Google Play Music PLaylists to CSV at: " + playlistsCsvFilePath.toAbsolutePath());
        googlePlayMusicService.exportPlayListsToCsv(playlistsCsvFilePath);

        logger.info("Done");
    }

}
