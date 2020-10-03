package net.nlacombe.euterpews.spotify.forfrontend;

import net.nlacombe.euterpews.onetimesecret.OneTimeSecretService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class SpotifyAuthWebService {

    private final SpotifyAuthServiceV2 spotifyAuthServiceV2;
    private final OneTimeSecretService oneTimeSecretService;

    public SpotifyAuthWebService(SpotifyAuthServiceV2 spotifyAuthServiceV2, OneTimeSecretService oneTimeSecretService) {
        this.spotifyAuthServiceV2 = spotifyAuthServiceV2;
        this.oneTimeSecretService = oneTimeSecretService;
    }

    @RequestMapping(value = "/spotifyOauthRedirectUri", method = RequestMethod.GET)
    public ResponseEntity<?> getSpotifyAuthTokenSecretId(@RequestParam String code) {
        var spotifyAccessToken = spotifyAuthServiceV2.getSpotifyAuthToken(code);
        var secretId = oneTimeSecretService.createOneTimeSecret(spotifyAccessToken).getSecretId();

        var headers = new HttpHeaders();
        headers.add("Location", "/pages/index.html?oneTimeSecretId=" + secretId);

        return new ResponseEntity<String>(headers, HttpStatus.SEE_OTHER);
    }

    @RequestMapping(value = "/spotifyAuthToken/{oneTimeSecretId}", method = RequestMethod.GET)
    public ResponseEntity<SpotifyAuthTokenV2> getSpotifyAuthToken(@PathVariable String oneTimeSecretId) {
        var spotifyAuthToken = oneTimeSecretService.<SpotifyAuthTokenV2>getAndRemoveOneTimeSecret(UUID.fromString(oneTimeSecretId));

        if (spotifyAuthToken == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.of(Optional.of(spotifyAuthToken));
    }

    @RequestMapping(value = "/spotifyOauthFrontendConfig", method = RequestMethod.GET)
    public SpotifyOauthFrontendConfig getAccessToken() {
        return spotifyAuthServiceV2.getSpotifyOauthFrontendConfig();
    }
}
