function loadDashboard() {
    var Dashboard = ReactRedux.connect(
        function (state) {
            return {
                user: state.user
            }
        }
    )(React.createClass({
        componentWillMount: function() {
            var dispatch = this.props.dispatch
            this.fetchUsername().then(function(user) {
                dispatch({type: "USER_DATA_FETCHED", user: user})
            })
        },
        fetchUsername: function() {
            var accessToken = localStorage.getItem("accessToken")
            var self = this.fetchUsername
            if (!accessToken) {
                document.location = "/login"
            }
            var headers = new Headers()
            headers.append("Authorization", "Bearer " + accessToken)
            return fetch(
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
                if (response.error) {
                    throw response
                }
                return response.user
            })
            .catch(function(e) {
                if (e.error === "invalid token") {
                    var params = new FormData()
                    params.append("grant_type", "refresh_token")
                    params.append("client_id", "frontend")
                    params.append("client_secret", "")
                    params.append("refresh_token", localStorage.getItem("refreshToken"))
                    return fetch(e.refreshTokenUri, {method: "POST", body: params})
                        .then(function(response) {
                            return response.json()
                        })
                        .then(function(response) {
                            if (response.error === "invalid_grant") {
                                throw "invalid grant"
                            }
                            localStorage.setItem("accessToken", response.access_token)
                            return self()
                        })
                } else {
                    throw "invalid grant"
                }
            })
            .catch(function(e) {
                document.location = "/login"
            })
        },
        render: function () {
            var user = this.props.user
            if (!user) {
                return React.createElement("h1", null, "Loading...")
            }
            return React.createElement(
                "div",
                null,
                [
                    React.createElement("h1", {key: "header"}, "Dashboard"),
                    React.createElement("p", {key: "helloMessage"}, "Hello, " + user.name + "!")
                ]
            )
        }
    }))

    var reducer = function (state, action) {
        switch(action.type) {
            case "USER_DATA_FETCHED":
                return {
                    user: action.user
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
