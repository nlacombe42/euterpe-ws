let localStorageKeys = {
    spotifyAccessToken: 'spotifyAccessToken',
};

function init() {
    let storedSpotifyAccessToken = getStoredSpotifyAccessToken();
    let spotifyAccessTokenUrlParamValue = getUrlParameter('spotifyAccessToken');

    if (storedSpotifyAccessToken) {
        loadPage();
    } else if (spotifyAccessTokenUrlParamValue) {
        localStorage.setItem(localStorageKeys.spotifyAccessToken, JSON.stringify(spotifyAccessTokenUrlParamValue));
        location.href = `${location.origin}/pages/index.html`;
    }
    else {
        showMessage('Redirecting to spotify for authentication...');
        redirectToSpotifyOauthPage();
    }
}

function loadPage() {
    showMessage('Loading spotify user info...');

    getSpotifyUserInfo().done(response => {
        showMessage('Spotify user info loaded');
        console.log({response});
    }).fail(() => showMessage('Failed to load spotify user info.'));
}

function getSpotifyUserInfo() {
    return $.ajax({
        url: 'https://api.spotify.com/v1/me',
        headers: {
            Authorization: `Bearer ${getStoredSpotifyAccessToken()}`
        },
    });
}

function getSpotifyAuthConfig() {
    return $.ajax({
        method: 'get',
        url: '/api/v1/spotifyOauthFrontendConfig',
    });
}

function getStoredSpotifyAccessToken() {
    return JSON.parse(localStorage.getItem(localStorageKeys.spotifyAccessToken));
}

function redirectToSpotifyOauthPage() {
    return getSpotifyAuthConfig().done(spotifyOauthConfig => {
        let urlParameters = $.param({
            response_type: 'code',
            client_id: spotifyOauthConfig.spotifyAuthClientId,
            scope: 'user-read-email',
            redirect_uri: spotifyOauthConfig.spotifyAuthRedirectUri
        });

        location.href = `https://accounts.spotify.com/authorize?${urlParameters}`;
    }).fail(() => showMessage("Failed to get spotify oauth frontend config"));
}

function showMessage(message, objectToLog) {
    var text = message;

    if (objectToLog) {
        text += ' : ' + JSON.stringify(objectToLog, undefined, 2);
    }

    $('.message').text(text);
}

function getUrlParameter(sParam) {
    var sPageURL = window.location.search.substring(1),
        sURLVariables = sPageURL.split('&'),
        sParameterName,
        i;

    for (i = 0; i < sURLVariables.length; i++) {
        sParameterName = sURLVariables[i].split('=');

        if (sParameterName[0] === sParam) {
            return sParameterName[1] === undefined ? true : decodeURIComponent(sParameterName[1]);
        }
    }
}

init();
