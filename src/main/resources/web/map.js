'use strict';

let map, maxUserId;

// Stores user ID and map layer of visible tracks for later removal
let visibleTracks = [];

/**
 * Fetch all users from the server
 * @param {HTMLElement} userList - reference to the user list HTML element
 * @return {Promise<>} a promise for performing additional actions after the update
 */
const fetchUsers = (userList) => {
    console.log("Fetching users");
    return fetch("/users")
        .then(res => res.json())
        .then(users => {
            maxUserId = Math.max(...users);
            document.getElementById("new_user_id").value = maxUserId + 1;

            // Clear existing data
            while (userList.lastChild) {
                userList.lastChild.remove();
            }

            // Add each user to the user list
            users.forEach(userId => {
                const item = document.createElement("li");
                item.dataset.userId = userId;

                const userLink = document.createElement("a");
                userLink.href = "#";
                userLink.innerText = `#${userId}`;
                userLink.style.color = getColor(userId);
                userLink.onclick = () => {
                    // First, try to remove the user's track, if it is visible
                    const wasVisible = removeTrack(userId);
                    if (!wasVisible) {
                        // Add the track to the map, if it was not visible before
                        fetchData(map, userId);
                    }
                };

                const deleteLink = document.createElement("a");
                deleteLink.classList.add("delete");
                deleteLink.href = "#";
                deleteLink.innerText = "[X]";
                deleteLink.onclick = () => {
                    deleteUser(userId);
                };

                item.appendChild(userLink);
                item.appendChild(deleteLink);

                userList.appendChild(item);
            });
        })
        .catch(error => console.error("Could not fetch user list", error));
};

/**
 * Hide a single user's track from the map if it is visible, returns true
 * @param {Number} userId - ID of the user whose track should be removed
 * @return {boolean} true if the track was visible before
 */
const removeTrack = (userId) => {
    const visible = visibleTracks.find(v => v.userId === userId);
    if (visible) {
        visible.layer.remove();
        visibleTracks = visibleTracks.filter(v => v.userId !== userId);
        return true;
    } else {
        return false;
    }
};

/**
 * Remove a single user and their tracks from the server
 * Any visible tracks are removed from the map before deleting the user.
 *
 * @param {Number} userId - ID of the user to delete
 */
const deleteUser = (userId) => {
    console.log("Deleting user with ID", userId);

    // Hide the user's track, if it is visible
    removeTrack(userId);

    fetch(`/users/${userId}`, {method: "DELETE"})
        .then(res => {
            fetchUsers(document.getElementById("user_list"));
        })
        .catch(err => {
            console.error("Could not delete user", err);
        });
};

/**
 * Calculate a consistent color for each user, spread out across the spectrum
 * @param {Number} userId - ID of the user
 * @return {string} CSS color specifier for the user
 */
const getColor = (userId) => {
    let [hue, sat, lightness] = [360 / maxUserId * userId, "50%", "50%"];
    return `hsl(${hue}, ${sat}, ${lightness})`;
};

/**
 * Fetch track length for a single user, returns a promise with the track information as JSON
 * @param {Number} userId - ID of the user
 * @return {Promise<>} track information as promise with JSON data
 */
const fetchTrackLength = (userId) => {
    return fetch(`/users/${userId}/trackLength`)
        .then(res => res.json())
};

/**
 * Fetch the track for a single user from the server
 * @param {HTMLElement} map - reference to the map container HTML element
 * @param {Number} userId - ID of the user
 */
const fetchData = (map, userId) => {
    fetch(`/users/${userId}/points`)
        .then(res => res.json())
        .then(points => {
            let coords = [];

            // Fetch info about the track for the currently selected user and display it in the popup
            const popupHandler = e => {
                fetchTrackLength(userId)
                    .then(data => {
                        e.popup.setContent(`
                                <h3>Track info</h3>
                                <table>
                                    <tr><td>User ID:</td><td>${userId}</td></tr>
                                    <tr><td>Number of points:</td><td>${data.numPoints}</td></tr>
                                    <tr><td>Track length:</td><td>${(data.length / 1000.0).toFixed(1)} km</td></tr>
                                </table>`);
                    });
            };

            // Add everything to a new layer
            let grp = L.layerGroup();

            // Add a marker for each point
            points.forEach(p => {
                let c = [p.latitude, p.longitude];
                coords.push(c);
                const marker = L.circleMarker(c, {
                    radius: 5,
                    fillOpacity: 0.5,
                    weight: 2,
                    color: getColor(userId)
                }).bindPopup().addTo(grp);
                marker.on("popupopen", popupHandler);
            });

            // Add a path connecting all points
            let polyline = L.polyline(coords, {color: getColor(userId), weight: 2}).bindPopup().addTo(grp);
            polyline.on("popupopen", popupHandler);
            map.fitBounds(polyline.getBounds());

            grp.addTo(map);
            visibleTracks.push({
                userId: userId,
                layer: grp
            });
        })
        .catch(error => console.error("Could not fetch tracks for user", error));
};

let newUser = {
    points: [],
    layer: undefined,
    polyline: undefined
};

/**
 * Initialize the base map from Mapbox
 */
const initMap = () => {
    map = L.map('map_container', {
        doubleClickZoom: false,
        center: [48.333889, 10.898333],
        zoom: 13,
    });
    L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
        attribution: 'Map data &copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors, <a href="https://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="https://www.mapbox.com/">Mapbox</a>',
        maxZoom: 18,
        id: 'mapbox.streets',
        accessToken: 'pk.eyJ1IjoiYWRyaWFub2tmIiwiYSI6ImNqcHkzYW45dzB3NjAzeGxzbGlsMjA2dngifQ.wPNVFONZIjF8Pj247_wmNA'
    }).addTo(map);

    map.on("dblclick", e => {
        if (!newUser.layer) {
            newUser.layer = L.layerGroup().addTo(map);
            newUser.polyline = L.polyline([]).addTo(newUser.layer);
        }

        const point = {
            coords: e.latlng,
            timestamp: Date.now()
        };

        L.marker(point.coords).addTo(newUser.layer);
        newUser.polyline.addLatLng(point.coords);
        newUser.points.push({
            latitude: point.coords.lat,
            longitude: point.coords.lng
        });
    });
};

/**
 * Show a user's track on the map
 * @param {Number} userId - ID of the user to show
 */
const showUser = userId => {
    const element = document.querySelector(`li[data-user-id="${userId}"] :first-child`);
    console.log("Showing user", userId, element);
    element.onclick();
};

/**
 * Clear all pending edits during new user creation
 */
const clearNewUser = () => {
    if (newUser.layer) {
        newUser.layer.remove();
    }
    newUser = {
        points: [],
        layer: undefined,
        polyline: undefined
    };
};

/**
 * Add a new user and save the track currently visible on the map
 */
const saveNewUser = () => {
    const userId = Number.parseInt(document.getElementById("new_user_id").value);
    console.log("Saving new user", newUser);

    const requestBody = {
        points: newUser.points
    };

    fetch(`/users/${userId}/points`, {method: "POST", body: JSON.stringify(requestBody)})
        .then(res => {
            console.log("New user created");
            clearNewUser();
            fetchUsers(document.getElementById("user_list"))
                .then(() => showUser(userId));
        })
        .catch(e => console.error("Could not create new user", err));
};

window.onload = () => {
    initMap();
    fetchUsers(document.getElementById("user_list"));

    // Buttons for new user creation
    document.getElementById("save_new_user").onclick = saveNewUser;
    document.getElementById("clear_new_user").onclick = clearNewUser;
};