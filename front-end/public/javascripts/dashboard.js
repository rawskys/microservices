(function fetchUsername() {
    var accessToken = localStorage.getItem("accessToken")
    if (!accessToken) {
        document.location = "/login"
    }
    console.log("access token")
    console.log(accessToken)
    var headers = new Headers()
    headers.append("Authorization", "Bearer " + accessToken)
    fetch(
        "/profile",
        {
            method: "GET",
            headers: headers
        }
    )
    .then(function(response) {
        return response.json()
    })
    .then(function(response) {
        console.log(response)
        if (!response.user) {
            document.location = "/login"
        }
    })
})()
