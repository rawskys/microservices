(function fetchUsername() {
    var accessToken = localStorage.getItem("accessToken")
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
        console.log(response)
    })
})()
