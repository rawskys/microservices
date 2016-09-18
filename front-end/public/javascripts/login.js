function login(form, redirectUri) {
	fetch(form.action, {method: "POST", body: new FormData(form)})
		.then(function(response) {
		    if (response.ok) {
                return response.json()
		    } else {
		        throw "Bad credentials"
		    }
		})
		.then(function(json) {
		    localStorage.setItem("accessToken", json.access_token)
		    localStorage.setItem("refreshToken", json.refresh_token)
            window.location = redirectUri
		})
		.catch(function(e) {
		    console.error(e)
		})
	return false
}
