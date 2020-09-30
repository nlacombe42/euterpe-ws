package net.nlacombe.euterpews.spotify;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SpotifyAuthWebService {

    private final SpotifyAuthServiceV2 spotifyAuthServiceV2;

    public SpotifyAuthWebService(SpotifyAuthServiceV2 spotifyAuthServiceV2) {
        this.spotifyAuthServiceV2 = spotifyAuthServiceV2;
    }

    @RequestMapping(value = "/spotifyOauthRedirectUri", method = RequestMethod.GET)
    public ResponseEntity<?> getAccessToken(@RequestParam String code) {
        var spotifyAccessToken = spotifyAuthServiceV2.getSpotifyAccessToken(code);
        var headers = new HttpHeaders();
        headers.add("Location", "/pages/index.html?spotifyAccessToken=" + spotifyAccessToken);

        return new ResponseEntity<String>(headers, HttpStatus.SEE_OTHER);
    }

    @RequestMapping(value = "/spotifyOauthFrontendConfig", method = RequestMethod.GET)
    public SpotifyOauthFrontendConfig getAccessToken() {
        return spotifyAuthServiceV2.getSpotifyOauthFrontendConfig();
    }
}
