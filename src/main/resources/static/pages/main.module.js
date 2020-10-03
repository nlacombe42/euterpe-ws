import $ from './jquery.module.js';

let localStorageKeys = {
    spotifyAccessToken: 'spotifyAccessToken',
    spotifyTokenExpiry: 'spotifyTokenExpiry',
};

let playlists;

function init() {
    let storedSpotifyAccessToken = getStoredSpotifyAccessToken();
    let storedSpotifyTokenExpiry = getStoredSpotifyTokenExpiry();
    let oneTimeSecretIdUrlParamValue = getUrlParameter('oneTimeSecretId');

    if (storedSpotifyTokenExpiry && new Date(storedSpotifyTokenExpiry) <= new Date()) {
        localStorage.removeItem(localStorageKeys.spotifyAccessToken);
        localStorage.removeItem(localStorageKeys.spotifyTokenExpiry);
        storedSpotifyAccessToken = undefined;
        storedSpotifyTokenExpiry = undefined;
    }

    if (storedSpotifyAccessToken && new Date(storedSpotifyTokenExpiry) > new Date()) {
        loadPage();
    } else if (oneTimeSecretIdUrlParamValue) {
        getAndStoreSpotifyAuthTokenFromOneTimeSecret(oneTimeSecretIdUrlParamValue).then(() => {
            location.href = `${location.origin}/pages/index.html`;
        });
    } else {
        showMessage('Redirecting to spotify for authentication...');
        redirectToSpotifyOauthPage();
    }
}

function loadPage() {
    showMessage('Loading playlists...');

    getAllCurrentUserPlaylists().then(allPlaylists => {
        playlists = allPlaylists;

        showPlaylists(playlists);

        $('#playlistSearchInput').on('input', () => updateShownPlaylistsBasedOnSearch());
        $('#numberOfPlaylists').text('Total number of playlists: ' + playlists.length);

        showMessage('Playlists loaded');
    }).catch(() => showMessage('Failed to load spotify user info.'));
}

function updateShownPlaylistsBasedOnSearch() {
    let searchTermsText = $('#playlistSearchInput').val().trim().toLowerCase();

    if (searchTermsText === '') {
        showPlaylists(playlists);

        return;
    }

    let playlistsToShow = playlists.filter(playlist => searchMatches(playlist.name, searchTermsText));
    showPlaylists(playlistsToShow);
}

function searchMatches(text, searchQuery) {
    let lowerCaseText = text.toLowerCase();
    let searchTerms = searchQuery.toLowerCase().split(' ');

    return searchTerms.every(searchTerm => lowerCaseText.includes(searchTerm));
}

function showPlaylists(playlists) {
    let playlistsHtml = playlists
        .sort((left, right) => left.name.toLowerCase() < right.name.toLowerCase() ? -1 : 1)
        .map(playlist => toPlaylistItemHtml(playlist));

    $('.playlists').html(playlistsHtml);
}

function toPlaylistItemHtml(playlist) {
    return '<li><a href="' + playlist.external_urls.spotify + '">' + playlist.name + '</a></li>';
}

function getAllCurrentUserPlaylists(offset) {
    const limit = 50;
    offset = +offset ? +offset : 0;

    return new Promise((resolve, reject) => {
        getCurrentUserPlaylists(offset, limit)
            .then(playlistPage => {
                if (playlistPage.items.length < limit) {
                    resolve(playlistPage.items);
                    return;
                }

                getAllCurrentUserPlaylists(offset + limit)
                    .then(forwardPlaylists => resolve(playlistPage.items.concat(forwardPlaylists)))
                    .catch(error => reject(error));
            })
            .catch(error => reject(error));
    });
}

function getCurrentUserPlaylists(offset, limit) {
    let queryParameters = $.param({offset, limit});

    return $.ajax({
        url: `https://api.spotify.com/v1/me/playlists?${queryParameters}`,
        headers: {
            Authorization: `Bearer ${getStoredSpotifyAccessToken()}`
        },
    });
}

function getAndStoreSpotifyAuthTokenFromOneTimeSecret(oneTimeSecretId) {
    return $.ajax({
        method: 'get',
        url: `/api/v1/spotifyAuthToken/${oneTimeSecretId}`,
    }).then(spotifyAuthToken => {
        localStorage.setItem(localStorageKeys.spotifyAccessToken, JSON.stringify(spotifyAuthToken.accessToken));
        localStorage.setItem(localStorageKeys.spotifyTokenExpiry, JSON.stringify(spotifyAuthToken.expiredAt));
    }).catch(error => {
        showMessage('Error getting spotify auth token from one time secret.');
        throw error;
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

function getStoredSpotifyTokenExpiry() {
    return JSON.parse(localStorage.getItem(localStorageKeys.spotifyTokenExpiry));
}

function redirectToSpotifyOauthPage() {
    return getSpotifyAuthConfig().done(spotifyOauthConfig => {
        let urlParameters = $.param({
            response_type: 'code',
            client_id: spotifyOauthConfig.spotifyAuthClientId,
            scope: 'user-library-read',
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
