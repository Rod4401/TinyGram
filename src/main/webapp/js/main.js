/**
 * Function that allows to process the connection of a google client
 * @param {The "token" google} response
 */
function connect(response) {
    //console.log("Login");
    const responsePayload = jwt_decode(
        response.credential);
    User.init(responsePayload, response.credential);
    m.redraw();
    
}

/**
 * Component that manages the connection
 */
var Connection = {
    /**
     * The view function
     */
    view: function() {
        /**
         * If the user is logged in, "disconnection" is displayed, otherwise the google sigin 
         */
        if(User.isLoged()) {
            return m("button", {
                        class: "btn material-icons unselectable",
                        id: "iconUser",
                        "data-bs-toggle": "dropdown",
                        "aria-expanded": "false",
                        type: "button",
                        onclick: function() {
                            MainView.changeView("profile");
                        }
                    },
                    m("img", {
                        class: "iconProfile",
                        src: User
                            .getUrl()
                    }))
            
        } else {
            return m("button", {
                        class: "btn material-icons unselectable",
                        id: "iconUser",
                        "data-bs-toggle": "dropdown",
                        "aria-expanded": "false",
                        type: "button",
                        onclick: function() {
                            User.showConnectView();
                        }
                    },
                    "account_circle"
                )
        }
    }
}

/**
 *  The component that manages the user 
 */
var User = {
    //The google response
    response: null,
    credential: null,
    //The list of the user's followers
    listFollowers: [],
    //The list of the user's follows
    listFollows: [],
    
    /** 
    The init function is a setter of the response 
    * @param {response} response The response from google login
    */
    init: function(response, credential) {
        this.response =
            response;
            this.credential = credential;
    },
    
    /**
     *  The function allows to fill the lists 
     */
    loadLists: function() {
        return m.request({
                method: "GET",
                url: "url"
            })
            .then(function(
                result) {
                //this.listFollowers = result.followers.items;
                //this.listFollows = result.follows.items;
                this.listFollowers =
                    this
                    .listFollowers
                    .sort(
                        function(
                            a,
                            b
                        ) {
                            return a
                                .localeCompare(
                                    b
                                )
                        });
                this.listFollows =
                    this
                    .listFollows
                    .sort(
                        function(
                            a,
                            b
                        ) {
                            return a
                                .localeCompare(
                                    b
                                )
                        });
                User.listView = [
                    ...
                    this
                    .listFollowers
                ];
            })
    },
    
    /**
     * Function that returns the user's access token
     * @return {String} The user's access token
     */
    getAccessToken: function() {
        return this.response
            .credential;
    },
    
    /**
     * Function that returns the user's picture
     * @return {String} The user's picture
     */
    getUrl: function() {
        return this.response
            .picture;
    },
    
    /**
     * Function that returns the user's name
     * @return {String} The user's name
     */
    getName: function() {
        return this.response
            .name;
    },
    
    /**
     * The user it is loged ?
     * @return {bool} True if loged, false otherwise
     */
    isLoged: function() {
        if(this.response !=
            null) {
            this
        .hideConnectView();
            return true;
        } else {
            this
        .showConnectView();
        }
        return false;
    },
    
    /**
     * Function that allows to follow the user
     * @param {User} user The user to follow
     */
    follow: function(user) {
        
    },
    
    /**
     * Does user this follow user user?
     * @param {User} user The user to be tested
     * @return {bool} True if this follow user, false otherwise
     */
    isFollow: function(user) {
        
    },
    
    showConnectView: function() {
        document.getElementById(
                "login")
            .className =
            "container centered";
    },
    
    hideConnectView: function() {
        document.getElementById(
                "login")
            .className = "hide";
    }
    
}

/**
 * The post component allows you to manage posts
 */
var Post = {
    //A list of random posts from user
    list: [],
    //List of my posts
    myList: [],
    //List of posts from people I follow
    followedList: [],
    
    /**
     * The function that allows you to load my post 
     * list and the post list (the most recent) 
     * of the people I follow
     */
    loadLists: function() {
        this.loadListFollowed();
        this.loadListPerso();
    },
    
    /**
     * The function that allows you to load my post list
     */
    loadListPerso: function() {
        return m.request({
                method: "GET",
                url: "_ah/api/myApi/v1/myPosts/0" + "?access_token=" + User.credential
            })
            .then(function(
                result) {
                Post.myList =
                    result
                    .items
            })
    },
    
    /**
     * The function that allows you to load the list of posts 
     * (the most recent) of people I don't follow
     */
    loadListRandom: function() {
        return m.request({
                method: "GET",
                url: "_ah/api/myApi/v1/myPosts/0" + "?access_token=" + "eyJhbGciOiJSUzI1NiIsImtpZCI6ImVlMWI5Zjg4Y2ZlMzE1MWRkZDI4NGE2MWJmOGNlY2Y2NTliMTMwY2YiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJuYmYiOjE2NjY0MzQyNTEsImF1ZCI6IjEzMjU4MDM4MjY3Ny1kZGhpbW44Yjh1YTZrY25hOGwwa21ucDdhNjV1MGljcC5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSIsInN1YiI6IjEwODEzMjA4OTM3NDM1NTIxNTQ1OCIsImVtYWlsIjoibS5tZXVuaWVyLnJvZHJpZ3VlQGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJhenAiOiIxMzI1ODAzODI2NzctZGRoaW1uOGI4dWE2a2NuYThsMGttbnA3YTY1dTBpY3AuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJuYW1lIjoiUm9kcmlndWUgTWV1bmllciIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BTG01d3UxOUxDclN2YmNyYm5UUEx6NmpveXQzZGx5LWZfTGZCd1F4RW5DOGlBPXM5Ni1jIiwiZ2l2ZW5fbmFtZSI6IlJvZHJpZ3VlIiwiZmFtaWx5X25hbWUiOiJNZXVuaWVyIiwiaWF0IjoxNjY2NDM0NTUxLCJleHAiOjE2NjY0MzgxNTEsImp0aSI6IjRlMDcyNDUzMDhmOWM3YTkzMDJhYjA1YzE0ZDk3ZmFhMDMyYThkNWYifQ.oz4yhRntO8O2rf-ebjZ_BkT0NsaXmzmOgpU3zHodpj72eUnfrIec-1MGj_9hQ9KtSFu-yF65adVXuJfpCiUCN_qni5tnqTARzWLMeTb6myAA5vrSSQGUFIOn06umGV77qr3afidSLnjzTkiZdmPdfVyR_cBFnCV-l5Q2OkRrijncRg5Eyw8kqNy-VgtdnwPa8yLyf38VoVA3oZPdlO1Y_t-_KMXReAwSD9YDSmdeM4vXeXqoo-SwyzBlh4KaIha5B3gBRF6YqfKO4eS3FnTpQHtxWuWi4K-W3spuLu3rYrY-RVxYh8hLo6VTo7Bd3s_l7aHIwAEIh4GsmgthesXb0Q"
            })
            .then(function(
                result) {
                Post.list =
                    result
                    .items
            })
    },
    
    /**
     * The function that allows you to load the list of posts 
     * (the most recent) of the people I follow
     */
    loadListFollowed: function() {
        return m.request({
                method: "GET",
                url: "url"
            })
            .then(function(
                result) {
                Post.followedList =
                    result
                    .items
            })
    },
    
    /**
     * The function that allows you to like a post
     * @param {post} post The post to like
     */
    like: function(post) {
        if(User.isLoged()) {
            //console.log("Je like : " + post);
            return m.request({
                    method: "PUT",
                    url: "url"
                })
                .then(function(
                    result
                ) {
                    post.like =
                        true;
                })
        } else {
            //We could make a popup that invites us to connect
            //console.log("User not logged");
        }
        
    },
    
}

/**
 * The main component that displays either my profile or the posts
 */
var MainView = {
    //The type of the current post ("welcome": posts; "profile": profile)
    type: "welcome",
    
    /**
     * The view function
     */
    view: function() {
        switch(this.type) {
            case "welcome":
                return PostView
                    .view();
            case "profile":
                return ProfileView
                    .view();
        }
    },
    
    /**
     * The function that allows you to change views
     * @param {view} view The type of view
     */
    changeView: function(view) {
        if(User.isLoged()) {
            if(this.type !=
                view) {
                this.type = view
            }
            Post.loadLists();
        }
    }
    
}

/**
 * The component that manages the profile view
 */
var ProfileView = {
    listView: [],
    
    //The type of user displayed (followers or follows)
    type: "followers",
    
    //For the navBar, user rendering is done via an "active" class,
    //"passive" is the class for the unselected type
    active: "d-flex align-items-center justify-content-center unselectable border-0 border-top border-dark bg-light ms-2",
    passive: "d-flex align-items-center justify-content-center unselectable border-0 bg-light ms-2",
    
    /**
     * The view function
     */
    view: function() {
        return m("div", {
            class: "container"
        }, m("div", {
            class: "row row-cols-1 row-cols-lg-3"
        }, [
            m("div", {
                id: "profileInfos",
                class: "col-lg-4 mb-2 mt-3 bg-light offset-lg-2 h-25"
            }, [
                m("div", {
                        class: "container"
                    },
                    m("div", {
                            class: "row"
                        },
                        [
                            m("div", {
                                    class: "col-3 postsProfilePicture w-25"
                                },
                                m("img", {
                                    style: "border-radius: 50%;",
                                    src: User
                                        .getUrl(),
                                    alt: "img_user",
                                    width: "100%",
                                    height: "100%",
                                    class: "unselectable"
                                })
                            ),
                            m("div", {
                                    class: "col-9 row-cols-1 pe-0"
                                },
                                [
                                    m("div", {
                                            class: "h-50 align-items-center d-flex"
                                        },
                                        m("h4",
                                            User
                                            .getName()
                                        )
                                    ),
                                    m("div", {
                                            class: "h-50 align-items-right d-flex"
                                        },
                                        [m("span", {
                                                    class: "me-4"
                                                },
                                                "X followers"
                                            ),
                                            m("span", {
                                                    class: "me-4"
                                                },
                                                "X suivi(e)s"
                                            )
                                        ]
                                    )
                                ]
                            )
                        ]
                    )
                ),
                m("div", {
                        class: "border-top d-flex justify-content-center"
                    },
                    [
                        m("button", {
                                type: "button",
                                id: "followers",
                                onclick: function() {
                                    ProfileView
                                        .changeView(
                                            'followers'
                                        );
                                },
                                class: (
                                    ProfileView
                                    .type ==
                                    "followers" ?
                                    ProfileView
                                    .active :
                                    ProfileView
                                    .passive
                                ),
                                
                            },
                            [m("span", {
                                        class: "material-icons ms-0"
                                    },
                                    "groups"
                                ),
                                m("span", {
                                        class: "ms-2"
                                    },
                                    "Followers"
                                )
                            ]
                        ),
                        m("button", {
                                type: "button",
                                id: "follows",
                                onclick: function() {
                                    ProfileView
                                        .changeView(
                                            'follows'
                                        )
                                },
                                class: (
                                    ProfileView
                                    .type ==
                                    "follows" ?
                                    ProfileView
                                    .active :
                                    ProfileView
                                    .passive
                                )
                            },
                            [m("span", {
                                        class: "material-icons ms-0"
                                    },
                                    "people_alt"
                                ),
                                m("span", {
                                        class: "ms-2"
                                    },
                                    "Follows"
                                )
                            ]
                        )
                    ]
                ),
                m("div", {
                        class: "form-outline mt-2 mb-2"
                    },
                    [
                        m("input", {
                            "oninput": function() {
                                ProfileView
                                    .search(
                                        this
                                        .value
                                    )
                            },
                            type: "search",
                            class: "form-control",
                            placeholder: "Rechercher",
                            "aria-label": "Search"
                        }),
                    ]
                ),
                m("div", {
                        class: "rounded h-100"
                    },
                    [
                        ProfileView
                        .listView
                        .map(
                            function(
                                item
                            ) {
                                return m(
                                    "p", {
                                        class: "fw-bold"
                                    },
                                    item
                                );
                            }
                        )
                    ]
                )
                
            ]), m(
                "div", {
                    class: "col-lg-4 me-2 pe-0 ps-0 mt-3 mb-2 offset-lg-1 bg-light",
                    id: "profilePosts"
                },
                m("div", {
                        class: "container"
                    },
                    m("div", {
                            class: "row"
                        },
                        [
                            Post
                            .myList
                            .map(
                                function(
                                    item
                                ) {
                                    return m(
                                        "div", {
                                            class: "col-4 postsProfilePicture"
                                        },
                                        m("div", {
                                                class: "container"
                                            },
                                            [
                                                m("div", {
                                                        class: "d-flex align-items-center justify-content-center"
                                                    },
                                                    [
                                                        m("img", {
                                                            width: "100%",
                                                            height: "100%",
                                                            src: item.properties.url,
                                                            class: "card unPost"
                                                        }),
                                                        m("div", {
                                                                class: "hide likeStyle text-white",
                                                                width: "100%"
                                                            },
                                                            "0" +
                                                            "♡"
                                                            )
                                                    ]
                                                    )
                                                
                                            ]
                                        )
                                    )
                                }
                            )
                        ]
                    )
                )
            )
        ]));
    },
    
    /**
     * The function that allows you to change views (followers or follows)
     * @param {type} type The new type
     */
    changeView: function(type) {
        //console.log(type);
        this.type = type;
        this.search("");
    },
    
    /**
     * The function that allows you to search for users based on a pattern (name)
     *  @param {name} name The name to search
     */
    search: function(name) {
        this.listView = (this
                .type ==
                "followers" ?
                User
                .listFollowers :
                User.listFollows
            )
            .filter(
                username =>
                username
                .includes(name)
            );
        m.redraw();
    }
    
}

/**
 * The component that manages the post view
 */
var PostView = {
    oninit: Post.loadListRandom(),
    
    /**
     * Function that returns to the top of page
     */
    goTop: function() {
        //console.log("Go on top");
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        })
    },
    
    /**
     * The view function
     */
    view: function() {
        return m("div", {
            class: "container d-flex justify-content-center",
            style: "width:400px;"
        }, m('div', [
            Post.list.map(function(item) {
                    return m(
                        'div', {
                            class: "border rounded row row-cols-1 mt-2 bg-white",
                            id: "IDDUPOST"
                        },
                        [
                            // ENTETE
                            m('div', {
                                    class: "mt-2 border-bottom col"
                                },
                                [
                                    m('div', {
                                            class: "container"
                                        },
                                        [
                                            m('div', {
                                                    class: "row"
                                                },
                                                [
                                                    m('div', {
                                                            class: "col-1"
                                                        },
                                                        m('img', {
                                                            class: "iconProfile unselectable",
                                                            onclick: function() {
                                                                User.follow(
                                                                    "IDEMMETEUR"
                                                                )
                                                            },
                                                            src: "LELIEN"
                                                        })
                                                    ),
                                                    m('div', {
                                                            class: "col-2"
                                                        },
                                                        m('p', {
                                                                class: "fw-bold",
                                                                onclick: function() {
                                                                    User.follow(
                                                                        "IDEMMETEUR"
                                                                    )
                                                                }
                                                            },
                                                            "LEPSEUDO"
                                                        )
                                                    ),
                                                ]
                                            ),
                                        ]
                                    ),
                                ]
                            ),
                            //IMAGE
                            m('div', {
                                    class: "border-bottom",
                                    style: "padding-left:0;padding-right:0;"
                                },
                                [
                                    m('img', {
                                        class: "w-100 unselectable",
                                        src: item.properties.url
                                    }),
                                ]
                            ),
                            
                            //LIKE
                            m('div', {
                                    class: "container"
                                },
                                [
                                    m('div', {
                                            class: "row"
                                        },
                                        [
                                            m('div', {
                                                    class: "col-1"
                                                },
                                                [
                                                    m('span', {
                                                            class: "material-icons unselectable",
                                                            style: "margin-left:0;margin-right:0;",
                                                            onclick: function() {
                                                                Post.like(
                                                                    item
                                                                )
                                                            }
                                                        },
                                                        "favorite_border"
                                                    ),
                                                ]
                                            ),
                                            m('div', {
                                                    class: "col-4 offset-7"
                                                },
                                                [
                                                    m('p', {
                                                            class: "fw-bold text-end"
                                                        },
                                                        "LE NOMBRE DE LIKES"
                                                    ),
                                                ]
                                            ),
                                        ]
                                    ),
                                ]
                            ),
                            
                            // DESCRIPTION
                            m('div',
                                item.properties.description
                            ),
                            
                        ]
                    )
                })
        ]))
    }
}

/**
 * The component that allows you to manage the div to create a post
 */
var NewPost = {
    isShow: false,
    view: function() {
        if(this.isShow ==
            true) {
            //return ce que valou à fait !
            return m("div", {"id":"newPost"},
            [
              m("div", {"class":"fondTransparent",onclick: function() {
                NewPost.hide()
            }}),
              m("div", {"class":"overlayDiv centered rounded onTop bg-light"},
                [
                  m("div", {"class":"centered-width"}, 
                    m("p", {"class":"text-justify fw-bold"}, 
                      "Créer une publication"
                    )
                  ),
                  m("div", {"class":"row overlayContainer centered"},
                    [
                      m("div", {"class":"col-8 bg-body border","id":"url"}, 
                        m("textarea", {"class":"form-control textArea","rows":"8","placeholder":"Saisir une url..."})
                      ),
                      m("div", {"class":"col-4 bg-body border","id":"infos"}, 
                        m("div", {"class":"row row-cols-1"},
                          [
                            m("div", {"class":"col mt-2","id":"sender"},
                              [
                                m("img", {"class":"iconProfile","src":"https://lh3.googleusercontent.com/a/ALm5wu19LCrSvbcrbnTPLz6joyt3dly-f_LfBwQxEnC8iA=s96-c"}),
                                m("h3", {"class":"fw-bold"}, 
                                  "rod4401"
                                )
                              ]
                            ),
                            m("div", {"class":"col mt-1","id":"description"}, 
                              m("textarea", {"class":"form-control textArea","id":"textAreaDescription","rows":"8","placeholder":"Ajoutez une légende..."})
                            ),
                            m("div", {"class":"col mt-1","id":"send","style":{"text-align":"end"}}, 
                              m("a", {"class":"text-decoration-none","href":"#"}, 
                                "Partager"
                              )
                            )
                          ]
                        )
                      )
                    ]
                  )
                ]
              )
            ]
          );
        }
    },
    
    /**
     * The function that displays it
     * The user must be logged in
     */
    show: function() {
        if(User.isLoged()) {
            this.isShow = true;
            m.redraw();
        }
    },
    
    /**
     * The function that hides it
     */
    hide: function() {
        this.isShow = false;
        m.redraw();
    }
    
}
