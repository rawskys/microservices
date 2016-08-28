function login(e) {
    var username = document.getElementById("username").value
    var password = document.getElementById("password").value
    var http = new XMLHttpRequest()
    http.open("POST", document.URL, true)
    http.setRequestHeader("Content-type", "application/json")
    http.setRequestHeader("Accept", "application/json")
    http.onreadystatechange = function () {
        if (http.readyState == 4) {
            console.log(JSON.parse(http.responseText))
        }
    }
    http.send(JSON.stringify({username: username, password: password}))
    return false
}
