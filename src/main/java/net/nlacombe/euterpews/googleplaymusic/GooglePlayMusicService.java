package net.nlacombe.euterpews.googleplaymusic;

import com.github.felixgail.gplaymusic.api.GPlayMusic;
import com.github.felixgail.gplaymusic.api.PlaylistApi;
import com.github.felixgail.gplaymusic.model.Playlist;
import com.github.felixgail.gplaymusic.model.PlaylistEntry;
import com.github.felixgail.gplaymusic.model.Track;
import com.github.felixgail.gplaymusic.model.requests.PagingRequest;
import com.github.felixgail.gplaymusic.model.responses.ListResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svarzee.gps.gpsoauth.AuthToken;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class GooglePlayMusicService {

    private static final Logger logger = LoggerFactory.getLogger(GooglePlayMusicService.class);

    private GPlayMusic googlePlayMusicApi;
    private PlaylistApi playlistApi;
    private List<Track> allLibraryTracks;

    public GooglePlayMusicService() {
        var authToken = new AuthToken("AUTH_TOKEN");
        googlePlayMusicApi = new GPlayMusic.Builder()
                .setAuthToken(authToken)
                .build();

        playlistApi = googlePlayMusicApi.getPlaylistApi();
    }

    public void exportPlayListsToCsv(Path csvFilePath) {
        try (FileWriter fileWriter = new FileWriter(csvFilePath.toFile());
             CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.RFC4180)) {

            csvPrinter.printRecord("Playlist name", "Song artist", "Song album artist", "Song album", "Song title");

            playlistApi.listPlaylists().forEach(playlist -> writePlaylistToCsv(csvPrinter, playlist));
        } catch (Exception e) {
            throw new RuntimeException("Error exporting Google Play Music playlists to CSV.", e);
        }
    }

    public void exportAllSongsToCsv(Path csvFilePath) {
        try (FileWriter fileWriter = new FileWriter(csvFilePath.toFile());
             CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.RFC4180)) {

            csvPrinter.printRecord("Song artist", "Song album artist", "Song album", "Song title");

            getAllLibraryTracks().forEach(track -> writeTrackToCsv(csvPrinter, track));
        } catch (Exception e) {
            throw new RuntimeException("Error exporting Google Play Music playlists to CSV.", e);
        }
    }

    private void writeTrackToCsv(CSVPrinter csvPrinter, Track track) {
        try {
            csvPrinter.printRecord(track.getArtist(), track.getAlbumArtist(), track.getAlbum(), track.getTitle());
        } catch (IOException e) {
            logger.warn("Could not get track info: track ID: \"" + track.getID() + "\"", e);
        }
    }

    private void writePlaylistToCsv(CSVPrinter csvPrinter, Playlist playlist) {
        try {
            playlistApi.getContents(playlist, 3000)
                    .forEach(playlistEntry -> writePlaylistEntryToCsv(csvPrinter, playlist, playlistEntry));
        } catch (Exception e) {
            throw new RuntimeException("Error writing playlist to CSV.", e);
        }
    }

    private void writePlaylistEntryToCsv(CSVPrinter csvPrinter, Playlist playlist, PlaylistEntry playlistEntry) {
        try {
            var track = getTrack(playlist, playlistEntry);
            csvPrinter.printRecord(playlist.getName(), track.getArtist(), track.getAlbumArtist(),
                    track.getAlbum(), track.getTitle());
        } catch (Exception e) {
            throw new RuntimeException("Could not get track info for playlist \"" + playlist.getName() + "\", " +
                    "playlist entry position: \"" + playlistEntry.getAbsolutePosition() + "\" and" +
                    "playlist entry track ID: \"" + playlistEntry.getTrackId() + "\"", e);
        }
    }

    private Track getTrack(Playlist playlist, PlaylistEntry playlistEntry) {
        try {
            return playlistEntry.getTrack();
        } catch (Exception e) {
            logger.warn("Error getting track from api, falling back to manually finding the track in all tracks.", e);

            Track manualTrack = getAllLibraryTracks().stream()
                    .filter(track -> playlistEntry.getTrackId().equals(track.getID()) ||
                            (track.getUuid().isPresent() && playlistEntry.getTrackId().equals(track.getUuid().get())))
                    .findAny()
                    .orElse(null);

            if (manualTrack != null)
                return manualTrack;

            throw new RuntimeException("Could not manually find track with ID: " + playlistEntry.getTrackId());
        }
    }

    private List<Track> getAllLibraryTracks() {
        if (allLibraryTracks == null) {
            allLibraryTracks = new LinkedList<>();
            String nextPageToken = null;

            try {
                while (true) {
                    ListResult<Track> responseBody = googlePlayMusicApi.getService()
                            .listTracks(new PagingRequest(nextPageToken, -1))
                            .execute()
                            .body();

                    allLibraryTracks.addAll(responseBody.toList());

                    nextPageToken = responseBody.getNextPageToken();

                    if (nextPageToken == null)
                        break;
                }
            } catch (Exception e) {
                throw new RuntimeException("Error getting all tracks.", e);
            }
        }

        return allLibraryTracks;
    }
}
