package net.nlacombe.euterpe.spotify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class SpotifyAuthService {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyAuthService.class);

    private SpotifyApi spotifyApi;

    public SpotifyAuthService(String clientId, String clientSecret, String redirectUriText) {
        spotifyApi = getSpotifyApi(clientId, clientSecret, redirectUriText);
    }

    public void openBrowserToGetAuthCode() {
        URI spotifyAuthUrl = spotifyApi.authorizationCodeUri()
                .scope("user-library-read user-library-modify playlist-modify-public")
                .show_dialog(true)
                .build()
                .execute();


        openUriInBrowser(spotifyAuthUrl);
    }

    public AuthorizationCodeCredentials getSpotifyAuthCredentials(String authCode) {
        try {
            return spotifyApi.authorizationCode(authCode).build().execute();
        } catch (IOException | SpotifyWebApiException e) {
            throw new RuntimeException(e);
        }
    }

    public AuthorizationCodeCredentials refreshAuthToken(String refreshToken) {
        try {
            return spotifyApi.authorizationCodeRefresh(spotifyApi.getClientId(), spotifyApi.getClientSecret(), refreshToken).build().execute();
        } catch (IOException | SpotifyWebApiException e) {
            throw new RuntimeException("Error refreshing auth token", e);
        }
    }

    public AuthorizationCodeCredentials getSpotifyAuthToken() {
        try {
            var spotifyAuthTokenFilePath = Path.of("./spotifyAuthToken.json");
            var jsonConverter = new ObjectMapper();
            AuthorizationCodeCredentials spotifyAuthToken;

            if (Files.exists(spotifyAuthTokenFilePath)) {
                spotifyAuthToken = jsonConverter.readValue(spotifyAuthTokenFilePath.toFile(), SpotifyAuthToken.class).toAuthorizationCodeCredentials();

                try {
                    spotifyAuthToken = refreshAuthToken(spotifyAuthToken.getRefreshToken());
                } catch (Exception e) {
                    logger.warn("Error refreshing stored token. ", e);

                    spotifyAuthToken = getNewAuthToken();
                }
            } else {
                spotifyAuthToken = getNewAuthToken();
            }

            jsonConverter.writeValue(spotifyAuthTokenFilePath.toFile(), new SpotifyAuthToken(spotifyAuthToken));

            return spotifyAuthToken;
        } catch (IOException e) {
            throw new RuntimeException("Error getting spotify auth token.", e);
        }
    }

    private AuthorizationCodeCredentials getNewAuthToken() {
        openBrowserToGetAuthCode();
        var authCode = getAuthCodeFromConsole();
        return getSpotifyAuthCredentials(authCode);
    }

    private static String getAuthCodeFromConsole() {
        System.out.print("Auth code: ");
        System.out.flush();

        Scanner scanner = new Scanner(System.in);

        return scanner.nextLine();
    }

    private SpotifyApi getSpotifyApi(String clientId, String clientSecret, String redirectUriText) {
        var redirectUri = SpotifyHttpManager.makeUri(redirectUriText);

        return new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(redirectUri)
                .build();
    }

    private void openUriInBrowser(URI uri) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
            throw new RuntimeException("Error: opening browser window not supported.");

        try {
            Desktop.getDesktop().browse(uri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
