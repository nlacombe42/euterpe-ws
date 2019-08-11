package net.nlacombe.euterpe.spotify;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.exceptions.detailed.TooManyRequestsException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.Playlist;
import com.wrapper.spotify.model_objects.specification.PlaylistSimplified;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.model_objects.specification.User;
import net.nlacombe.commonlib.stream.PageSource;
import net.nlacombe.commonlib.stream.StreamUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class SpotifyService {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyService.class);

    private SpotifyApi spotifyApi;
    private User currentSpotifyUser;

    public SpotifyService(AuthorizationCodeCredentials spotifyAuthToken) {
        spotifyApi = SpotifyApi.builder()
                .setAccessToken(spotifyAuthToken.getAccessToken())
                .setRefreshToken(spotifyAuthToken.getRefreshToken())
                .build();

        currentSpotifyUser = getCurrentSpotifyUser();
    }

    public void importLibrarySongs(Path csvFilePath) {
        try (InputStreamReader inputStreamReader = new FileReader(csvFilePath.toFile());
             var csvRows = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(inputStreamReader)) {

            var totalTrackNotFound = 0;

            for (CSVRecord csvRow : csvRows) {
                String artistName = normalizeArtist(csvRow.get("Song artist"));
                String albumName = normalizeName(csvRow.get("Song album"));
                String trackName = normalizeName(csvRow.get("Song title"));

                try {
                    addTrackToLibrary(artistName, albumName, trackName);
                } catch (Exception e) {
                    logger.warn("Could not add track", e);
                    totalTrackNotFound++;
                }
            }

            logger.info("Total number of tracks not found: " + totalTrackNotFound);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addTrackToLibrary(String artistName, String albumName, String trackName) {
        try {
            Track track = getTrack(artistName, albumName, trackName);

            spotifyApi.saveTracksForUser(track.getId()).build().execute();
        } catch (IOException | SpotifyWebApiException e) {
            if (e instanceof TooManyRequestsException) {
                waitToLimitRequestRate((TooManyRequestsException) e);
            }

            throw new RuntimeException("Could not add track to library.", e);
        }
    }

    public void importPlaylistsFromCsv(Path csvFilePath, String prefix) {
        try (InputStreamReader inputStreamReader = new FileReader(csvFilePath.toFile());
             var csvRows = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(inputStreamReader)) {

            var totalTrackNotFound = 0;

            for (CSVRecord csvRow : csvRows) {
                String playlistName = csvRow.get("Playlist name").trim();
                String artistName = normalizeArtist(csvRow.get("Song artist"));
                String albumName = normalizeName(csvRow.get("Song album"));
                String trackName = normalizeName(csvRow.get("Song title"));

                try {
                    addTrackToPlaylist(prefix + playlistName, artistName, albumName, trackName);
                } catch (Exception e) {
                    logger.warn("Could not add track", e);
                    totalTrackNotFound++;
                }
            }

            logger.info("Total number of tracks not found: " + totalTrackNotFound);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addTrackToPlaylist(String playlistName, String artistName, String albumName, String trackName) {
        try {
            Track track = getTrack(artistName, albumName, trackName);
            SpotifyPlaylistSummary playlist = getOrCreatePlaylist(playlistName);

            if (playlistContainsTrack(playlist.getId(), track.getId()))
                return;

            spotifyApi.addTracksToPlaylist(playlist.getId(), new String[]{track.getUri()}).build().execute();
        } catch (IOException | SpotifyWebApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteAllPlaylistsStartingWith(String prefix) {
        getCurrentUserPlaylist()
                .filter(playlist -> playlist.getName() != null && playlist.getName().startsWith(prefix))
                .forEach(playlist -> {
                    try {
                        spotifyApi.unfollowPlaylist(playlist.getId()).build().execute();
                    } catch (IOException | SpotifyWebApiException e) {
                        logger.warn("Error unfollowing playlist: " + playlist.getName(), e);
                    }
                });
    }

    public Playlist createPlaylist(String playlistName) {
        try {
            return spotifyApi.createPlaylist(currentSpotifyUser.getId(), playlistName).build().execute();
        } catch (IOException | SpotifyWebApiException e) {
            throw new RuntimeException("Error creating playlist with name: " + playlistName);
        }
    }

    public Stream<PlaylistSimplified> getCurrentUserPlaylist() {
        return StreamUtil.createStream(new PageSource<>() {
            private int pageSize = 50;
            private int pageOffset = 0;

            @Override
            public List<PlaylistSimplified> getNextPage() {
                try {
                    var searchResult = spotifyApi.getListOfCurrentUsersPlaylists()
                            .offset(pageOffset)
                            .limit(pageSize)
                            .build().execute();

                    pageOffset++;

                    return List.of(searchResult.getItems());
                } catch (IOException | SpotifyWebApiException e) {
                    throw new RuntimeException("Error getting current user playlists.", e);
                }
            }
        });
    }

    public Stream<PlaylistTrack> getPlaylistTracks(String playlistId) {
        return StreamUtil.createStream(new PageSource<>() {
            private int pageSize = 50;
            private int pageOffset = 0;

            @Override
            public List<PlaylistTrack> getNextPage() {
                try {
                    var searchResult = spotifyApi.getPlaylistsTracks(playlistId)
                            .offset(pageOffset)
                            .limit(pageSize)
                            .build().execute();

                    pageOffset++;

                    return List.of(searchResult.getItems());
                } catch (IOException | SpotifyWebApiException e) {
                    throw new RuntimeException("Error getting playlist tracks.", e);
                }
            }
        });
    }

    public Track getTrack(String artistName, String albumName, String trackName) {
        try {
            String query = getSearchQuery(artistName, albumName, trackName);

            var searchResult = spotifyApi
                    .searchTracks(query)
                    .build()
                    .execute();

            return Arrays.stream(searchResult.getItems())
                    .filter(currentTrack -> isTrackMatch(currentTrack, artistName, albumName, trackName))
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("Track match not found. query: " + query));
        } catch (IOException | SpotifyWebApiException e) {
            if (e instanceof TooManyRequestsException) {
                waitToLimitRequestRate((TooManyRequestsException) e);
            }

            throw new RuntimeException(e);
        }
    }

    private String getSearchQuery(String artistName, String albumName, String trackName) {
        String query = "";
        query += "artist:\"" + artistName + "\" ";

        if (!isIgnoredAlbumName(albumName) && !albumName.isEmpty())
            query += "album:\"" + albumName + "\" ";

        query += "track:\"" + trackName + "\"";

        return query;
    }

    private boolean playlistContainsTrack(String playlistId, String trackId) {
        return getPlaylistTracks(playlistId)
                .anyMatch(playlistTrack -> StringUtils.equals(playlistTrack.getTrack().getId(), trackId));
    }

    private SpotifyPlaylistSummary getOrCreatePlaylist(String playlistName) {
        var existingPlaylist = getCurrentUserPlaylist()
                .filter(playlist -> StringUtils.equalsIgnoreCase(playlist.getName(), playlistName))
                .findAny()
                .orElse(null);

        if (existingPlaylist != null)
            return toSpotifyPlaylistSummary(existingPlaylist);

        var createdPlaylist = createPlaylist(playlistName);

        return toSpotifyPlaylistSummary(createdPlaylist);
    }

    private boolean isTrackMatch(Track track, String artistName, String albumName, String trackName) {
        return Arrays.stream(track.getArtists()).anyMatch(trackArtist -> isArtistMatch(trackArtist, artistName)) &&
                isAlbumMatch(track, albumName) &&
                isTrackMatch(track, trackName);
    }

    private boolean isArtistMatch(ArtistSimplified trackArtist, String artistName) {
        String trackArtistName = normalizeArtist(trackArtist.getName());

        return StringUtils.equalsIgnoreCase(trackArtistName, artistName);
    }

    private boolean isTrackMatch(Track track, String trackName) {
        String trackTitle = normalizeName(track.getName());

        return StringUtils.equalsIgnoreCase(trackTitle, trackName) || StringUtils.containsIgnoreCase(trackTitle, trackName);
    }

    private boolean isAlbumMatch(Track track, String albumName) {
        String trackAlbumName = normalizeName(track.getAlbum().getName());

        return isIgnoredAlbumName(albumName) ||
                StringUtils.equalsIgnoreCase(trackAlbumName, albumName) ||
                StringUtils.containsIgnoreCase(trackAlbumName, albumName);
    }

    private boolean isIgnoredAlbumName(String albumName) {
        return StringUtils.containsIgnoreCase(albumName, "best of") ||
                StringUtils.containsIgnoreCase(albumName, "greatest Hits") ||
                StringUtils.containsIgnoreCase(albumName, "anthology") ||
                StringUtils.containsIgnoreCase(albumName, "anniversary");
    }

    private User getCurrentSpotifyUser() {
        try {
            return spotifyApi.getCurrentUsersProfile().build().execute();
        } catch (IOException | SpotifyWebApiException e) {
            throw new RuntimeException("Error getting current spotify user", e);
        }
    }

    private SpotifyPlaylistSummary toSpotifyPlaylistSummary(Playlist playlist) {
        return new SpotifyPlaylistSummary() {
            @Override
            public String getId() {
                return playlist.getId();
            }

            @Override
            public String getName() {
                return playlist.getName();
            }
        };
    }

    private SpotifyPlaylistSummary toSpotifyPlaylistSummary(PlaylistSimplified playlist) {
        return new SpotifyPlaylistSummary() {
            @Override
            public String getId() {
                return playlist.getId();
            }

            @Override
            public String getName() {
                return playlist.getName();
            }
        };
    }

    private String normalizeArtist(String artist) {
        artist = normalizeName(artist);

        if ("The Eagles".equals(artist))
            return "Eagles";

        return artist;
    }

    private String normalizeName(String name) {
        name = name.replace("'", "");

        if (name.indexOf('-') != -1)
            name = name.substring(0, name.indexOf('-'));

        if (name.indexOf('(') != -1)
            name = name.substring(0, name.indexOf('('));

        return name.trim();
    }

    private void waitToLimitRequestRate(TooManyRequestsException e) {
        int secondsToWait = e.getRetryAfter();

        try {
            Thread.sleep(secondsToWait * 1000);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
