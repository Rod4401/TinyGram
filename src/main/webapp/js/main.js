/**
 * URL bases
 */
const postRandomsUrl =
    "api/apiTinyGram/v1/fetchNewPostsGlobal";
const likeUrl =
    "api/apiTinyGram/v1/likePost/";
const isLikeUrl =
    "api/apiTinyGram/v1/likeState/";
const followedUrl =
    "api/apiTinyGram/v1/followState/";
const signInUrl =
    "api/apiTinyGram/v1/signIn";
const connectUrl =
    "api/apiTinyGram/v1/connect";
const userInfo =
    "api/apiTinyGram/v1/basicUserInfos/";
const followUserUrl =
    "api/apiTinyGram/v1/followUser/";
const postPublicationUrl =
    "api/apiTinyGram/v1/postPublication";

/**
 * Function that allows to process the connection of a google client
 * @param {The "token" google} response
 */
function connect(response) {
    //console.log("Login");
    const responsePayload = jwt_decode(
        response.credential);
    User.init(responsePayload, response
        .credential);
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
            return m("li", {
                class: "list-unstyled dropdown"
            }, [
                m("button", {
                        class: "btn material-icons unselectable",
                        id: "iconUser",
                        "data-bs-toggle": "dropdown",
                        "aria-expanded": "false",
                        type: "button"
                    },
                    m("img", {
                        class: "iconProfile",
                        src: User
                            .getUrl()
                    })),
                m("ul", {
                    class: "dropdown-menu unselectable",
                    "aria-labelledby": "navbarDropdownMenuLink"
                }, [
                    m("li",
                        m("a", {
                                class: "dropdown-item",
                                onclick: function() {
                                    MainView
                                        .changeView(
                                            "profile"
                                        );
                                }
                            },
                            "Mon profil"
                        )
                    ),
                    m("li",
                        m("a", {
                                class: "dropdown-item"
                            },
                            "A propos"
                        )
                    )
                ]),
            ])

        } else {
            return m("button", {
                    class: "btn material-icons unselectable",
                    id: "iconUser",
                    "data-bs-toggle": "dropdown",
                    "aria-expanded": "false",
                    type: "button",
                    onclick: function() {
                        User
                    .showConnectView();
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
    followers: 0,
    follows: 0,

    /** 
    The init function is a setter of the response 
    * @param {response} response The response from google login
    */
    init: function(response,
        credential) {
        this.response =
            response;
        this.credential =
            credential;
        User.connect()
    },

    connect: function() {

        return m.request({
                method: "PUT",
                url: connectUrl,
                params: {
                    access_token: User
                        .getAccessToken()
                },
            })
            .then(function(
                result) {
                Post
            .loadListRandom();
            })
            .catch(function(
                result) {
                User
            .signIn();
            })
    },

    signIn: function() {
        const responsePayload =
            jwt_decode(User
                .getAccessToken()
            );
        return m.request({
                method: "POST",
                url: signInUrl,
                params: {
                    access_token: User
                        .getAccessToken(),
                    pseudo: responsePayload
                        .given_name,
                    fname: responsePayload
                        .family_name,
                    lname: responsePayload
                        .given_name,
                    pictureURL: responsePayload
                        .picture
                },
            })
            .then(function(
                result) {
                Post
            .loadListRandom();
            })
            .catch(function(
                result) {
                window
                    .alert(
                        "Erreur de connexion, veuillez réessayer"
                    );
            })
    },

    /**
     *  The function allows to fill the lists 
     */
    loadLists: function() {
        m.request({
                method: "POST",
                url: followedUrl +
                    ":UserID",
                params: {
                    access_token: User
                        .getAccessToken(),
                    UserID: User
                        .response
                        .sub
                }
            })
            .then(function(
                result) {
                User.followers =
                    result
                    .properties
                    .nbFollow
            })




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
        return this.credential;
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
        return m.request({
                method: "POST",
                url: followUserUrl +
                    ":idUser",
                params: {
                    idUser: user,
                    access_token: User
                        .getAccessToken()
                }
            })
            .then(function(
                result) {
                Post.list
                    .map(
                        function(
                            item
                        ) {
                            if(item
                                .properties
                                .creatorID ==
                                user
                                ) {
                                item.properties
                                    .userHasFollowed =
                                    true
                            }
                        })
            })
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
    cursor: "",

    /**
     * The function that allows you to load my post list
     */
    loadListPerso: function() {
        return m.request({
                method: "GET",
                url: "_ah/api/myApi/v1/myPosts/0" +
                    "?access_token=" +
                    User
                    .getAccessToken()
            })
            .then(function(
                    result) {
                    Post.myList =
                        result
                        .items
                },
                this
                .connectLikes(
                    Post.list),
            )
    },

    /**
     * The function that allows you to load the list of posts 
     * (the most recent) of people I don't follow
     */
    loadListRandom: function() {
        this.chargerSuivants()
    },

    chargerSuivants: function() {
        return m.request({
                method: "GET",
                url: postRandomsUrl,
                params: {
                    access_token: User
                        .getAccessToken(),
                    cursor: Post
                        .cursor
                }
            })
            .then(function(
                result) {
                Post.list
                    .push
                    .apply(
                        Post
                        .list,
                        result
                        .items
                    ),
                    Post
                    .connectLikes(
                        Post
                        .list
                    ),
                    Post
                    .connectUser(
                        Post
                        .list
                    ),
                    Post
                    .cursor =
                    result
                    .nextPageToken
            })
    },

    connectLikes: function(list) {
        list.map(function(
            item) {
            //The likes 
            m.request({
                    method: "GET",
                    url: isLikeUrl +
                        ":id",
                    params: {
                        id: item
                            .key
                            .name,
                        access_token: User
                            .getAccessToken()
                    }
                })
                .then(
                    function(
                        result
                    ) {
                        item.properties
                            .likes =
                            result
                            .properties
                            .nbLikes;
                        item.properties
                            .like =
                            result
                            .properties
                            .userHasLiked;
                    })
        })
    },

    connectUser: function(list) {
        list.map(function(
            item) {
            //The users infos
            m.request({
                    method: "GET",
                    url: userInfo +
                        ":UserID",
                    params: {
                        UserID: item
                            .properties
                            .creatorID,
                        access_token: User
                            .getAccessToken()
                    }
                })
                .then(
                    function(
                        result
                    ) {
                        item.properties
                            .creatorURL =
                            result
                            .properties
                            .pictureUrl;
                        item.properties
                            .creatorPseudo =
                            result
                            .properties
                            .pseudo;
                        item.properties
                            .userHasFollowed =
                            result
                            .properties
                            .userHasFollowed;
                    })
        })
    },

    /**
     * The function that allows you to like a post
     * @param {post} post The post to like
     */
    like: function(post) {
        if(User.isLoged()) {
            //console.log("Je like : " + post);
            if(!post.properties
                .like) {
                return m
                    .request({
                        method: "POST",
                        url: likeUrl +
                            ":postId",
                        params: {
                            postId: post
                                .key
                                .name,
                            access_token: User
                                .getAccessToken()
                        }
                    })
                    .then(
                        function(
                            result
                        ) {
                            post.properties
                                .like =
                                true;
                            post.properties
                                .likes++;
                        })
            }
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
            if(view ==
                "profile") {
                Post
                    .loadListPerso();
            } else {
                Post
                    .loadListRandom();
            }

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
                                                User
                                                .followers +
                                                " followers"
                                            ),
                                            m("span", {
                                                    class: "me-4"
                                                },
                                                User
                                                .follows +
                                                " suivi(e)s"
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
                                                            src: item
                                                                .properties
                                                                .url,
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
            Post
            .list
            .map(
                function(
                    item
                ) {
                    return m(
                        'div', {
                            class: "border rounded row row-cols-1 mt-2 bg-white",
                            id: item
                                .key
                                .name,
                        },
                        [
                            // TOP POST
                            m('div', {
                                    class: "mt-2 ps-0 border-bottom col"
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
                                                                    item
                                                                    .properties
                                                                    .creatorID,
                                                                )
                                                            },
                                                            src: item
                                                                .properties
                                                                .creatorURL
                                                        })
                                                    ),
                                                    m('div', {
                                                            class: "col-2"
                                                        },
                                                        m('p',
                                                            item
                                                            .properties
                                                            .creatorPseudo
                                                        )
                                                    ),
                                                    m('div', {
                                                            class: "col-4"
                                                        },
                                                        item
                                                        .properties
                                                        .creatorID !=
                                                        User
                                                        .response
                                                        .sub &&
                                                        item
                                                        .properties
                                                        .userHasFollowed ==
                                                        false ?
                                                        (m('p', {
                                                                class: "pt-0 text-primary",
                                                                style: "cursor: pointer;",
                                                                onclick: function() {
                                                                    User.follow(
                                                                        item
                                                                        .properties
                                                                        .creatorID
                                                                    )
                                                                }
                                                            },
                                                            "• Suivre"
                                                        )) :
                                                        "",
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
                                        src: item
                                            .properties
                                            .pictureUrl
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
                                                    class: "col-1 ps-0"
                                                },
                                                [
                                                    m('span', {
                                                            class: "material-icons unselectable ms-2",
                                                            style: item
                                                                .properties
                                                                .like ==
                                                                true ?
                                                                "margin-left:0;margin-right:0; color:red;" :
                                                                "margin-left:0;margin-right:0;",
                                                            onclick: function() {
                                                                Post.like(
                                                                    item,
                                                                )
                                                            }
                                                        },
                                                        item
                                                        .properties
                                                        .like ==
                                                        true ?
                                                        "favorite" :
                                                        "favorite_border",
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
                                                        item
                                                        .properties
                                                        .likes +
                                                        " Likes"
                                                    ),
                                                ]
                                            ),
                                        ]
                                    ),
                                ]
                            ),

                            // DESCRIPTION
                            m('div',
                                item
                                .properties
                                .body
                            ),

                        ]
                    )
                }),
            m("div", {
                    "class": "border rounded row row-cols-1 mt-2 bg-white"
                },
                m("div", {
                        "class": "d-flex justify-content-center"
                    },
                    m("button", {
                            "class": "btn",
                            onclick: function() {
                                Post
                                    .chargerSuivants();
                            }
                        },
                        "Suivant"
                    )
                )
            )
        ]))
    }
}

/**
 * The component that allows you to manage the div to create a post
 */
var NewPost = {
    url: "",
    body: "",
    isShow: false,
    view: function() {
        if(this.isShow ==
            true) {
            this.url = "";
            this.body = "";
            return m("div", {
                    "id": "newPost"
                },
                [
                    m("div", {
                        "class": "fondTransparent",
                        onclick: function() {
                            NewPost
                                .hide()
                        }
                    }),
                    m("div", {
                            "class": "overlayDiv centered rounded onTop bg-light"
                        },
                        [
                            m("div", {
                                    "class": "centered-width"
                                },
                                m("p", {
                                        "class": "text-justify fw-bold",
                                        style: "font-family: 'Dancing Script';font-size: 25;"
                                    },
                                    "Créer une publication"
                                )
                            ),
                            m("div", {
                                    "class": "row overlayContainer centered"
                                },
                                [
                                    m("div", {
                                            "class": "col-8 bg-body border",
                                            "id": "url"
                                        },
                                        m("textarea", {
                                            "class": "form-control textArea",
                                            "rows": "8",
                                            "placeholder": "Saisir une url...",
                                            oninput: function(
                                                e
                                            ) {
                                                NewPost
                                                    .url =
                                                    e
                                                    .target
                                                    .value
                                            }
                                        })
                                    ),
                                    m("div", {
                                            "class": "col-4 bg-body border",
                                            "id": "infos"
                                        },
                                        m("div", {
                                                "class": "row row-cols-1"
                                            },
                                            [
                                                m("div", {
                                                        "class": "col mt-2 d-flex align-items-center",
                                                        "id": "sender"
                                                    },
                                                    [
                                                        m("img", {
                                                            "class": "iconProfile",
                                                            "src": User
                                                                .response
                                                                .picture
                                                        }),
                                                        m("h4", {
                                                                "class": "fw-bold ms-2"
                                                            },
                                                            User
                                                            .response
                                                            .given_name
                                                        )
                                                    ]
                                                ),
                                                m("div", {
                                                        "class": "col mt-1",
                                                        "id": "description"
                                                    },
                                                    m("textarea", {
                                                        "class": "form-control textArea",
                                                        "id": "textAreaDescription",
                                                        "rows": "8",
                                                        "placeholder": "Ajoutez une légende...",
                                                        oninput: function(
                                                            e
                                                            ) {
                                                            NewPost
                                                                .body =
                                                                e
                                                                .target
                                                                .value
                                                        }
                                                    })
                                                ),
                                                m("div", {
                                                        "class": "col mt-1",
                                                        "id": "send",
                                                        "style": {
                                                            "text-align": "end"
                                                        }
                                                    },
                                                    m("button", {
                                                            "class": "btn border",
                                                            onclick: function() {
                                                                NewPost
                                                                    .post();
                                                            }
                                                        },
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
    },

    post: function() {
        return m.request({
                method: "POST",
                url: postPublicationUrl,
                params: {
                    access_token: User
                        .getAccessToken(),
                    pictureURL: NewPost
                        .url,
                    body: NewPost
                        .body
                }
            })
            .then(function(
                result) {
                //Post added
                NewPost
                    .hide();
            })
    }

}
