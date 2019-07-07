package net.nlacombe.euterpe.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        exportGooglePlayMusicDataToCsv();
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
