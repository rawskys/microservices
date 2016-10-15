function loadDashboard(accountUri, refreshTokenUri) {
    var Dashboard = ReactRedux.connect(
        function (state) {
            return {
                profile: state.profile
            }
        },
        function (dispatch) {
            return {
                fetchAccount: function () {
                    var fetchAccount = function () {
						var accessToken = localStorage.getItem("accessToken")
						if (!accessToken) {
							document.location = "/login"
						}
						var headers = new Headers()
						headers.append("Authorization", "Bearer " + accessToken)
						return fetch(
							accountUri,
							{
								method: "GET",
								headers: headers
							}
						)
						.then(function (response) {
						    if (response.ok) {
                                return response.json()
						    }
						    throw response
						})
                    }
                    return fetchAccount().catch(function(e) {
                        if (e.statusText === "Unauthorized") {
                            var params = new FormData()
                            params.append("grant_type", "refresh_token")
                            params.append("client_id", "frontend")
                            params.append("client_secret", "")
                            params.append("refresh_token", localStorage.getItem("refreshToken"))
                            return fetch(refreshTokenUri, {method: "POST", body: params})
                                .then(function(response) {
                                    if (response.statusText == "OK") {
                                        return response.json()
                                    }
                                    throw response
                                })
                                .catch(function (response) {
                                    return response.json().then(function(error) {
                                        throw error
                                    })
                                })
                                .then(function(response) {
                                    localStorage.setItem("accessToken", response.access_token)
                                    return fetchAccount()
                                })
                        } else {
                            throw e
                        }
                    })
                    .catch(function(e) {
                        console.error(e)
                        alert(e)
                    })
                    .then(function(account) {
                        dispatch({type: "ACCOUNT_FETCHED", account: account})
                        return account
                    })
                },
                fetchProfile: function(id) {
                    var headers = new Headers()
                    headers.append("Authorization", "Bearer " + localStorage.getItem("accessToken"))
                    return fetch(
                        "/profile/" + id,
                        {
                            method: "GET",
                            headers: headers
                        }
                    )
                    .then(function(response) {
                        if (response.statusText == "OK") {
                            return response.json()
                        }
                        throw response
                    })
                    .catch(function(e) {
                        console.error(e)
                    })
                    .then(function(profile) {
                        dispatch({type: "PROFILE_DATA_FETCHED", profile: profile})
                    })
                },
                logout: function () {
                    console.log("log out!")
                    console.log(dispatch)
                    localStorage.removeItem("accessToken")
                    localStorage.removeItem("refreshToken")
                    document.location = "/login"
                }
            }
        }
    )(React.createClass({
        componentDidMount: function() {
            var props = this.props
            props.fetchAccount().then(function (account) {
                return props.fetchProfile(account.id)
            })
        },
        render: function () {
            var profile = this.props.profile
            if (!profile) {
                return React.createElement("h1", null, "Loading...")
            }
            return React.createElement(
                "div",
                null,
                [
                    React.createElement("h1", {key: "header"}, "Dashboard"),
                    React.createElement("p", {key: "helloMessage"}, "Hello, " + profile.name + "!"),
                    React.createElement("a", {key: "logout", href: "#", onClick: this.props.logout}, "log out")
                ]
            )
        }
    }))

    var reducer = function (state, action) {
        switch(action.type) {
            case "PROFILE_DATA_FETCHED":
                return {
                    profile: action.profile
                }
            default:
                return {}
        }
    }
    var store = Redux.createStore(reducer)

    ReactDOM.render(
        React.createElement(
            ReactRedux.Provider,
            {store: store},
            React.createElement(Dashboard, {value: store.getState()})
        ),
        document.getElementById("container")
    )
}
