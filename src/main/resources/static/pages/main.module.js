import $ from './jquery.module.js';

let localStorageKeys = {
    spotifyAccessToken: 'spotifyAccessToken',
    spotifyTokenExpiry: 'spotifyTokenExpiry',
};

let playlists;
let selectedTrack;

function init() {
    let storedSpotifyAccessToken = getStoredSpotifyAccessToken();
    let storedSpotifyTokenExpiry = getStoredSpotifyTokenExpiry();
    let oneTimeSecretIdUrlParamValue = getUrlParameter('oneTimeSecretId');

    if (storedSpotifyTokenExpiry && new Date(storedSpotifyTokenExpiry) <= new Date()) {
        clearStoredSpotifyAuthInfo();
        storedSpotifyAccessToken = undefined;
        storedSpotifyTokenExpiry = undefined;
    }

    if (storedSpotifyAccessToken && new Date(storedSpotifyTokenExpiry) > new Date()) {
        loadPage();
    } else if (oneTimeSecretIdUrlParamValue) {
        $('.loginSection').css({display: 'none'});

        showMessage('Authenticating...');

        getAndStoreSpotifyAuthTokenFromOneTimeSecret(oneTimeSecretIdUrlParamValue).then(() => {
            redirectToMainPage();
        });
    } else {
        $('#loginButton').on('click', () => {
            showMessage('Redirecting to spotify for authentication...');
            redirectToSpotifyOauthPage();
        });
    }
}


function loadPage() {
    $('.loginSection').css({display: 'none'});

    setupLogoutSection();
    setupPlaylistsSection();
    setupSelectTrackSection();
}

function setupPlaylistsSection() {
    $('.playlistSection').css({display: 'block'});

    showMessage('Loading playlists...');

    getAllCurrentUserPlaylists().then(allPlaylists => {
        playlists = allPlaylists;

        showPlaylists(playlists, selectedTrack);

        $('#playlistSearchInput').on('input', () => updateShownPlaylistsBasedOnSearch());
        $('#numberOfPlaylists').text('Total number of playlists: ' + playlists.length);

        showMessage('Playlists loaded');
    }).catch(() => showMessage('Failed to load spotify user info.'));
}

function setupSelectTrackSection() {
    $('.selectTrackSection').css({display: 'block'});
    $('#selectTrackForm').submit(() => {
        let trackText = $('#trackToSelect').val().trim();
        let trackId = normalizeTrackId(trackText);

        getTrackFromId(trackId).then(track => {
            selectedTrack = track;
            showSelectedTrackInfo(selectedTrack);
            showPlaylists(playlists, selectedTrack);
        }).catch(() => showMessage('Failed to load track info.'));

        return false;
    });
}

function setupLogoutSection() {
    $('.logoutSection').css({display: 'block'});

    $('#logoutButton').on('click', () => {
        showMessage('Logging out...');
        clearStoredSpotifyAuthInfo();
        redirectToMainPage();
    });
}

function showSelectedTrackInfo(track) {
    $('#selectedTrackName').text(track.name);
    $('#selectedTrackAlbumName').text(track.album.name);
    $('#selectedTrackArtistsNames').text(track.artists.map(artist => artist.name).join(', '));
    $('.selectedTrackInfo').css({display: 'block'});
}

function normalizeTrackId(trackText) {
    let indexOfSlash = trackText.lastIndexOf('/');
    let indexOfColon = trackText.lastIndexOf(':');

    if (indexOfSlash >= 0) {
        return trackText.substr(indexOfSlash + 1);
    }

    if (indexOfColon >= 0) {
        return trackText.substr(indexOfColon + 1);
    }

    return trackText;
}

function updateShownPlaylistsBasedOnSearch() {
    let searchTermsText = $('#playlistSearchInput').val().trim().toLowerCase();

    if (searchTermsText === '') {
        showPlaylists(playlists, selectedTrack);

        return;
    }

    let playlistsToShow = playlists.filter(playlist => searchMatches(playlist.name, searchTermsText));
    showPlaylists(playlistsToShow, selectedTrack);
}

function searchMatches(text, searchQuery) {
    let lowerCaseText = text.toLowerCase();
    let searchTerms = searchQuery.toLowerCase().split(' ');

    return searchTerms.every(searchTerm => lowerCaseText.includes(searchTerm));
}

function showPlaylists(playlists, selectedTrack) {
    let playlistsHtml = playlists
        .sort((left, right) => left.name.toLowerCase() < right.name.toLowerCase() ? -1 : 1)
        .map(playlist => toPlaylistItemHtml(playlist, selectedTrack));

    $('.playlists').html(playlistsHtml);
}

function toPlaylistItemHtml(playlist, selectedTrack) {
    let listItemHtml = ``;
    listItemHtml += `<li>`;

    if (selectedTrack) {
        let uuid = uuidV4();

        $('body').on('click', `#${uuid}`, () => {
            addTrackToPlaylist(playlist.id, selectedTrack.id)
                .then(() => showMessage("Selected track added to playlist successfully"))
                .catch(() => showMessage("Error adding selected track to playlist"));
        });

        listItemHtml += `<button type="button" id="${uuid}">Add selected track to this playlist</button> `;
    }

    listItemHtml += `<a href="${playlist.external_urls.spotify}">${playlist.name}</a>`;
    listItemHtml += `</li>`;

    return listItemHtml;
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

function getTrackFromId(trackId) {
    return $.ajax({
        url: `https://api.spotify.com/v1/tracks/${trackId}`,
        headers: {
            Authorization: `Bearer ${getStoredSpotifyAccessToken()}`
        },
    });
}

function addTrackToPlaylist(playlistId, trackId) {
    let requestBody = JSON.stringify({
        uris: [`spotify:track:${trackId}`]
    });

    return $.ajax({
        method: 'post',
        url: `https://api.spotify.com/v1/playlists/${playlistId}/tracks`,
        data: requestBody,
        contentType: 'application/json',
        headers: {
            Authorization: `Bearer ${getStoredSpotifyAccessToken()}`
        },
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

function clearStoredSpotifyAuthInfo() {
    localStorage.removeItem(localStorageKeys.spotifyAccessToken);
    localStorage.removeItem(localStorageKeys.spotifyTokenExpiry);
}

function redirectToSpotifyOauthPage() {
    return getSpotifyAuthConfig().done(spotifyOauthConfig => {
        let urlParameters = $.param({
            response_type: 'code',
            client_id: spotifyOauthConfig.spotifyAuthClientId,
            scope: 'user-library-read playlist-modify-public',
            redirect_uri: spotifyOauthConfig.spotifyAuthRedirectUri,
            show_dialog: true
        });

        location.href = `https://accounts.spotify.com/authorize?${urlParameters}`;
    }).fail(() => showMessage("Failed to get spotify oauth frontend config"));
}

function redirectToMainPage() {
    location.href = `${location.origin}/pages/index.html`;
}

function showMessage(message, objectToLog) {
    var text = message;

    if (objectToLog) {
        text += ' : ' + JSON.stringify(objectToLog, undefined, 2);
    }

    $('.message').text(text);
}

function uuidV4() {
    return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
        (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
    );
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
