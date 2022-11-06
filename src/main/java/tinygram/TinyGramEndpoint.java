package tinygram;


import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.api.server.spi.response.NotFoundException;


import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

import tinygram.entities.PostIn;
import tinygram.entities.UserIn;

import com.google.appengine.api.datastore.Transaction;

@Api(name = "apiTinyGram",
     version = "v1",
     audiences = "883199197906-konv6vuei354vcmadoipr5b2v733sos8.apps.googleusercontent.com",
     clientIds = {"883199197906-konv6vuei354vcmadoipr5b2v733sos8.apps.googleusercontent.com"},
     namespace =
     @ApiNamespace(
		   ownerDomain = "helloworld.example.com",
		   ownerName = "helloworld.example.com",
		   packagePath = "")
     )

/*  
 *   =================================================================
 *
 *      oOoOOoOOo                  .oOOOo.                         
 *          o     o               .O     o                         
 *          o                     o                                
 *          O                     O                                
 *          o     O  'OoOo. O   o O   .oOOo `OoOo. .oOoO' `oOOoOO. 
 *          O     o   o   O o   O o.      O  o     O   o   O  o  o 
 *          O     O   O   o O   o  O.    oO  O     o   O   o  O  O 
 *          o'    o'  o   O `OoOO   `OooO'   o     `OoO'o  O  o  o 
 *                              o                                  
 *                           OoO'                                  
 * 
 *   ================================================================
 * 
 *  WEB, CLOUD & DATASTORE PROJECT - 2022 - TinyGram
 *  Master 1 ALMA - Nantes Université
 *  
 *      Quentin     Gomes Dos Reis
 *      Rodrigue    Meunier         (Captain Rillette)
 *      Valentin    Goubon
 */



public class TinyGramEndpoint {

    private static final Logger log = Logger.getLogger(TinyGramEndpoint.class.getName());


    //  We abstract the fact that this user is already registered and valid
    //  Because it will be just callable by this vlass not any user...
    private void createAndInitFollowCounters(DatastoreService datastore, User user) throws UnauthorizedException{
        if(user == null) throw new UnauthorizedException("Invalid credentials");
        Key userKey = KeyFactory.createKey("User", user.getId());
        Entity counter;

        for(int i = 1; i < 11; i++){
            counter = new Entity(KeyFactory.createKey(userKey, "FollowCounter", i));
            counter.setProperty("value", 0L);
            datastore.put(counter);
        }
    }


    private void createAndInitLikeCounters(DatastoreService datastore, Key postKey){
        Entity counter;

        for(int i = 1; i < 11; i++){
            counter = new Entity(KeyFactory.createKey(postKey, "LikeCounter", i));
            counter.setProperty("value", 0L);
            datastore.put(counter);
        }
    }


    /*
     *  signIn Method
     * 
     *      Only used once to register user into our datastore.
     *      Once the user is registered, any other call of this method will result by HTTP 401 UNUAUTHORIZED error.
     *      There is restrictions on user informations...
     *          -   The pseudonym must be shorter or equal than 16 caracters
     *          -   The firstName and lastName shouldn't exceed 50 caracters
     *          -   The user profile picture must be delivered and hosted by Google
     *              because we will reuse the one that is used for the user Google account
     */
    @ApiMethod(name = "signIn", httpMethod = HttpMethod.POST)
	public Entity signIn(User user, UserIn userInfos) throws BadRequestException, UnauthorizedException {

        //  Filter low performance cost verification parameters
        if(user == null) throw new UnauthorizedException("Invalid credentials");
        if(userInfos == null) throw new BadRequestException("Invalid parameters: Need user informations to be able de register someone !");
        if(userInfos.pseudo == null || userInfos.pseudo.isEmpty()) throw new BadRequestException("Invalid parameters: pseudo cannot be null or empty !");
        if(userInfos.fname == null || userInfos.fname.isEmpty()) throw new BadRequestException("Invalid firstName: firstname cannot be null or empty !");
        if(userInfos.lname == null || userInfos.lname.isEmpty()) throw new BadRequestException("Invalid lastName: lastname cannot be null or empty !");
        if(userInfos.pictureURL == null || userInfos.pictureURL.isEmpty()) throw new BadRequestException("Invalid profile picture URL: URL cannot be null or empty !");

        //  Avoid too long informations
        if(userInfos.pseudo.length() >= 16) throw new BadRequestException("User informations too long: Pseudonym might be too long !");
        if(userInfos.fname.length() >= 50 || userInfos.lname.length() >= 50) throw new BadRequestException("User informations too long: firstName or lastName might be too long !");

        //  Check if the provided picture profile URL is from google
        Pattern pattern = Pattern.compile("^https:\\/\\/lh[0-9]\\.googleusercontent\\.com\\/a\\/[a-zA-Z0-9]+=.*$");
        Matcher matcher = pattern.matcher(userInfos.pictureURL);

        if(!matcher.matches()) throw new BadRequestException("Invalid profile picture URL: Profile picture URL must be a picture of a google account !");
        
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Key userKey = KeyFactory.createKey("User", user.getId());

        //  Verify that the user is not already present
        Query query = new Query("User").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        userKey));

		PreparedQuery pq = datastore.prepare(query);
        FetchOptions fo = FetchOptions.Builder.withLimit(1);
        if(pq.countEntities(fo) > 0) throw new UnauthorizedException("User already registered !");

        //  Verify that the pseudonym is not already taken by an other user
        query = new Query("User").setFilter(new FilterPredicate("pseudo",
                                                                FilterOperator.EQUAL,
                                                                userInfos.pseudo));
		pq = datastore.prepare(query);
        if(pq.countEntities(fo) > 0) throw new UnauthorizedException("Pseudonym already taken, PLEASE choose another one !");

        //  Generate user entity that will be returned and stored
        Entity userEntity = new Entity(userKey);
        userEntity.setProperty("pseudo", userInfos.pseudo);
        userEntity.setProperty("firstName", userInfos.fname);
        userEntity.setProperty("lastName", userInfos.lname);
        userEntity.setProperty("pictureUrl", userInfos.pictureURL);
        userEntity.setProperty("lastLogin", new Date());

        datastore.put(userEntity);

        //  Just create and init follow counters
        createAndInitFollowCounters(datastore, user);

		return userEntity;
	}


    /*
     *  connect Method
     *      
     *      Just used to be able to refresh the lastLogin property of a user.
     * 
     *      NOTE: It can only be called normally if signIn has already been called successfully once.
     */
    @ApiMethod(name = "connect", httpMethod = HttpMethod.POST)
	public Entity connect(User user) throws UnauthorizedException {

        //  Provided user must be valid
        if(user == null) throw new UnauthorizedException("Invalid credentials");
        
        //  Search the user entity in our datastore
        Query query = new Query("User").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        KeyFactory.createKey("User", user.getId())));
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(query);
		Entity userEntity = pq.asSingleEntity();

        //  The user is not present in our datastore if the userEntity is null
        if(userEntity == null) throw new UnauthorizedException("Not registered: Please register before trying to log in !");

        //  Update lastLogin property save changes and return the entity
        userEntity.setProperty("lastLogin", new Date());
        datastore.put(userEntity);

		return userEntity;
	}


    /*
     *  postPublication Method
     * 
     *      This method will publish a post on TinyGram.
     *      
     *      There is some limitations on the post content...
     *          -   The body or description can be missing
     *          -   If there is a body or a description, the lenght is limited to 280 caracters (included).
     *          -   The picture are necessary and this is an obligation
     * 
     *      NOTE: It can only be called normally if signIn has already been called successfully once.
     */
    @ApiMethod(name = "postPublication", httpMethod = HttpMethod.POST)
	public Entity postPublication(User user, PostIn post) throws ForbiddenException, BadRequestException, UnauthorizedException {

        //  Provided user must be valid
        if(user == null) throw new UnauthorizedException("Invalid credentials !");

        //  Filter low performance cost verification parameters
        if(post == null) throw new BadRequestException("Post is absent: We cannot post void as a post that will cause a paradox and this will create a devastating black hole, so don't do that again seriously !");
        if(post.body != null && post.body.length() >= 280) throw new BadRequestException("Body post's too long: Body lenght exceeding the 280 caracters limit");
        if(post.pictureURL == null || post.pictureURL.isEmpty()) throw new BadRequestException("Picture post's is absent: Picture URL must be provided, this cannot be empty !");

        //  Might need a step to verify post infos to avoid injection or to verify provided pictureURL

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key userKey = KeyFactory.createKey("User", user.getId());

        //  Verify that the user is present in our datastore
        Query query = new Query("User").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        userKey));
		PreparedQuery pq = datastore.prepare(query);
        FetchOptions fo = FetchOptions.Builder.withLimit(1);

        if(pq.countEntities(fo) == 0) throw new UnauthorizedException("Not registered: Please register before trying to post something !");

        //  Good the user is registered
        //  Now let's create post entity

        Date currentDate = new Date();

        Key postEntityKey = KeyFactory.createKey("Post", String.format("%d%s", currentDate.getTime(), user.getId()));

		Entity postEntity = new Entity(postEntityKey);        
		postEntity.setProperty("creatorID", user.getId());
        postEntity.setProperty("body", post.body);
		postEntity.setProperty("pictureUrl", post.pictureURL);
		postEntity.setProperty("date", currentDate);

        //  Make sure that the user will no try to post the same post at the same time
		Transaction txn = datastore.beginTransaction();
		datastore.put(postEntity);

        createAndInitLikeCounters(datastore, postEntityKey);

		txn.commit();
		return postEntity;
	}


    /*
     *  followUser Method
     *      
     *      Used to make a person A follow a person B.
     *      It's uni-directionnal and irreversible (at least as the project subject says)
     *      Can only called one time successfully, the next call will return a HTTP 401 Error.
     * 
     *      NOTE 1 : The person A cannot follow him/herself, that is forbidden.
     * 
     *      NOTE 2 : It can only be called normally if signIn has already been called successfully once.
     */
    @ApiMethod(name = "followUser", httpMethod = HttpMethod.GET)
	public Entity followUser(@Named("userID") String followedUserID, User user) throws ForbiddenException, BadRequestException, UnauthorizedException, NotFoundException, InternalServerErrorException {

        //  Provided user must be valid
        if(user == null) throw new UnauthorizedException("Invalid credentials !");

        //  FollowedUserID verification
        //  Google doesn't provide a clear definition of what can or what cannot be a userID
        //  So might need to be adjusted in case of errors
        Pattern pattern = Pattern.compile("^[0-9]+$");
        Matcher matcher = pattern.matcher(followedUserID);
        
        if(followedUserID == null || !matcher.matches()) throw new BadRequestException("FollowedUserID is absent: either the user is a ghost or it is a mistake but the ID must be related to a valid and registred user account !");

        //  Check if the user will not follow himself, that will be strange
        if(followedUserID.equals(user.getId())) throw  new BadRequestException("Reflexing following: Someone cannot follow himself that will be strange !");


        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Key userKey = KeyFactory.createKey("User", user.getId());
        Key followedUserKey = KeyFactory.createKey("User", followedUserID);

        //  Verify that the user is present in our datastore
        Query query = new Query("User").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        userKey));
		PreparedQuery pq = datastore.prepare(query);
        FetchOptions fo = FetchOptions.Builder.withLimit(1);
        if(pq.countEntities(fo) == 0) throw new UnauthorizedException("Not registered: Please register before trying to post something !");

        //  Verify that the followedUser is present in our datastore
        query = new Query("User").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        followedUserKey));
		pq = datastore.prepare(query);
        if(pq.countEntities(fo) == 0) throw new NotFoundException("Not found: An user cannot follow an unregistered user !");

        //  Good the follow isn't already registered
        Date currentDate = new Date();
        Key followEntityKey = KeyFactory.createKey("Follow", String.format("%s%s", followedUserID, user.getId()));
        
        //  Make sure that the user will no try to follow the same user at the same time
		Transaction txn = datastore.beginTransaction();

        query = new Query("Follow").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                    FilterOperator.EQUAL,
                                                                    followEntityKey));
		pq = datastore.prepare(query);
        if(pq.countEntities(fo) > 0) throw new ForbiddenException("Already followed: You are already following user !");

        //  Now let's create follow entity
        Entity followEntity = new Entity(followEntityKey);
		followEntity.setProperty("userID", user.getId());
		followEntity.setProperty("followedID", followedUserID);
		followEntity.setProperty("date", currentDate);

        //  Know which counter we will increment
        Calendar calendar = Calendar.getInstance();
        int cntNb = (calendar.get(Calendar.SECOND) % 10) + 1;

        //  Save follow entity
        datastore.put(followEntity);

        //  Get follow counter
        query = new Query("FollowCounter").setAncestor(followedUserKey)
                                            .setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                            FilterOperator.EQUAL,
                                                                            KeyFactory.createKey(followedUserKey, "FollowCounter", cntNb)));
		pq = datastore.prepare(query);

        //  Must have found a counter
        Entity followCounter = pq.asSingleEntity();
        if(followCounter == null) throw new InternalServerErrorException("Fatal server error: Unexpected behavior, unable to follow the user !");

        //  Increase follow counter
        Long value = (Long) followCounter.getProperty("value") + 1;
		followCounter.setProperty("value", value);

        //  Just put everything back into the datastore
		datastore.put(followCounter);

		txn.commit();
		return followEntity;
	}


    /*
     *  likePost Method
     *      
     *      Used to make a person A like a post.
     *      It's irreversible (at least as the project subject says)
     *      Can only called one time successfully, the next call will return a HTTP 401 Error.
     * 
     *      NOTE: It can only be called normally if signIn has already been called successfully once.
     */
    @ApiMethod(name = "likePost", httpMethod = HttpMethod.GET)
	public Entity likePost(@Named("postID") String likedPostID, User user) throws ForbiddenException, BadRequestException, UnauthorizedException, NotFoundException, InternalServerErrorException {

        //  Provided user must be valid
        if(user == null) throw new UnauthorizedException("Invalid credentials !");

        //  Provided post id must be not null
        if(likedPostID == null) throw new BadRequestException("LikedPostID is absent: either the post doesn't exist or it is a mistake but the postID must be related to a valid and existing post !");

        //  Might need to process a regex on likedPostID
        Pattern pattern = Pattern.compile("^[0-9]+$");
        Matcher matcher = pattern.matcher(likedPostID);
        if(!matcher.matches()) throw new BadRequestException("LikedPostID is invalid: either it is a mistake but the postID must be related to a valid and existing post !");

        Key userKey = KeyFactory.createKey("User", user.getId());
        Key likedPostEntityKey = KeyFactory.createKey("Post", likedPostID);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        //  Verify that the user is present in our datastore
        Query query = new Query("User").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        userKey));
		PreparedQuery pq = datastore.prepare(query);
        FetchOptions fo = FetchOptions.Builder.withLimit(1);
        if(pq.countEntities(fo) == 0) throw new UnauthorizedException("Not registered: Please register before trying to post something !");

        //  Verify that the post is present in our datastore
        query = new Query("Post").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        likedPostEntityKey));
		pq = datastore.prepare(query);
        if(pq.countEntities(fo) == 0) throw new NotFoundException("Not found: Please like something that truely exist !");

        Key likeEntityKey = KeyFactory.createKey("Like", String.format("%s%s", likedPostID, user.getId()));

        //  Verify that the user didn't already liked this post in our datastore
        query = new Query("Like").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                FilterOperator.EQUAL,
                                                                likeEntityKey));
		pq = datastore.prepare(query);
        if(pq.countEntities(fo) > 0) throw new ForbiddenException("Already liked: Please like something else you already liked that post !");

        //  Good the post is exist
        //  Now let's create like entity
		Entity likeEntity = new Entity(likeEntityKey);
		likeEntity.setProperty("userID", user.getId());
		likeEntity.setProperty("postID", likedPostID);
		likeEntity.setProperty("date", new Date());

        //  Know which counter we will increment
        Calendar calendar = Calendar.getInstance();
        int cntNb = (calendar.get(Calendar.SECOND) % 10) + 1;

        //  Make sure that any othet user will no try to like the same post at the same time
        Transaction txn = datastore.beginTransaction();

        datastore.put(likeEntity);

        Key likeCounterEntityKey = KeyFactory.createKey(likedPostEntityKey, "LikeCounter",  cntNb);

        //  Get like counter
        query = new Query("LikeCounter").setAncestor(likedPostEntityKey)
                                        .setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        likeCounterEntityKey));
		pq = datastore.prepare(query);

        Entity likeCounter = pq.asSingleEntity();
        if(likeCounter == null) throw new InternalServerErrorException("Fatal server error: Unexpected behavior, unable to like this post !");

        //  Increase like counter
        Long value = (Long) likeCounter.getProperty("value") + 1;
		likeCounter.setProperty("value", value);

        datastore.put(likeCounter);

		txn.commit();

		return likeEntity;
	}


    private long getLikeNumber(DatastoreService datastore, Key postKey){
        Long value = 0L;

        Query query = new Query("LikeCounter").setAncestor(postKey);
        PreparedQuery preparedQuery = datastore.prepare(query);
        List<Entity> listCnt = preparedQuery.asList(FetchOptions.Builder.withLimit(10));

        for(Entity e : listCnt){
            value += (Long) e.getProperty("value");
        }

        return value;
    }

    private long getFollowNumber(DatastoreService datastore, Key userKey){
        Long value = 0L;

        Query query = new Query("FollowCounter").setAncestor(userKey);
        PreparedQuery preparedQuery = datastore.prepare(query);
        List<Entity> listCnt = preparedQuery.asList(FetchOptions.Builder.withLimit(10));

        for(Entity e : listCnt){
            value += (Long) e.getProperty("value");
        }

        return value;
    }


    /*
     *  likeState Method
     *      
     *      Used to know the number of likes of a post and if the user who requested it liked this post.
     * 
     *      NOTE: It can only be called normally if signIn has already been called successfully once.
     */
    @ApiMethod(name = "likeState", httpMethod = HttpMethod.GET)
	public Entity likeState(@Named("postID") String postID, User user) throws ForbiddenException, BadRequestException, UnauthorizedException, NotFoundException {

        //  Provided user must be valid
        if(user == null) throw new UnauthorizedException("Invalid credentials !");

        //  Provided post id must be not null
        if(postID == null) throw new BadRequestException("PostID is absent: either the post doesn't exist or it is a mistake but the postID must be related to a valid and existing post !");

        //  Might need to process a regex on postID
        Pattern pattern = Pattern.compile("^[0-9]+$");
        Matcher matcher = pattern.matcher(postID);
        if(!matcher.matches()) throw new BadRequestException("PostID is invalid: either it is a mistake but the postID must be related to a valid and existing post !");

        Key userKey = KeyFactory.createKey("User", user.getId());
        Key postEntityKey = KeyFactory.createKey("Post", postID);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        //  Verify that the user is present in our datastore
        Query query = new Query("User").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        userKey));
		PreparedQuery pq = datastore.prepare(query);
        FetchOptions fo = FetchOptions.Builder.withLimit(1);
        if(pq.countEntities(fo) == 0) throw new UnauthorizedException("Not registered: Please register before trying to fetch something !");

        //  Verify that the post is present in our datastore
        query = new Query("Post").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        postEntityKey));
		pq = datastore.prepare(query);
        if(pq.countEntities(fo) == 0) throw new NotFoundException("Not found: Please fetch something that truely exist !");

        //  Everything is okay
        //  Now we can continue by creating the responseEntity
        long nbLikes = getLikeNumber(datastore, postEntityKey);

        Key likeEntityKey = KeyFactory.createKey("Like", String.format("%s%s", postID, user.getId()));

        query = new Query("Like").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                FilterOperator.EQUAL,
                                                                likeEntityKey));
		pq = datastore.prepare(query);

        Entity response = new Entity("LikeState");
        response.setProperty("nbLikes", nbLikes);
        response.setProperty("userHasLiked", pq.countEntities(fo) > 0);

		return response;
	}
    

    /*
     *  followState Method
     *      
     *      Used to know the number of follow of a user and if the user who requested it is following this user.
     * 
     *      NOTE: It can only be called normally if signIn has already been called successfully once.
     */
    @ApiMethod(name = "followState", httpMethod = HttpMethod.GET)
	public Entity followState(@Named("userID") String userID, User user) throws ForbiddenException, BadRequestException, UnauthorizedException, NotFoundException {

        //  Provided user must be valid
        if(user == null) throw new UnauthorizedException("Invalid credentials !");

        //  Provided userID must be not null
        if(userID == null) throw new BadRequestException("UserID is absent: either the userID doesn't exist or it is a mistake but the userID must be related to a valid and registered user !");

        //  Might need to process a regex on userID
        Pattern pattern = Pattern.compile("^[0-9]+$");
        Matcher matcher = pattern.matcher(userID);
        if(!matcher.matches()) throw new BadRequestException("UserID is invalid: either it is a mistake but the UserID must be related to a valid and registered user !");

        Key userKey = KeyFactory.createKey("User", user.getId());
        Key userFollowKey = KeyFactory.createKey("User", userID);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        //  Verify that the user is present in our datastore
        Query query = new Query("User").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        userKey));
		PreparedQuery pq = datastore.prepare(query);
        FetchOptions fo = FetchOptions.Builder.withLimit(1);
        if(pq.countEntities(fo) == 0) throw new UnauthorizedException("Not registered: Please register before trying to fetch something !");

        //  Verify that the requested user is present in our datastore
        query = new Query("User").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        userFollowKey));
		pq = datastore.prepare(query);
        if(pq.countEntities(fo) == 0) throw new NotFoundException("Not found: Please fetch a user that truely exist !");

        //  Everything is okay
        //  Now we can continue by creating the responseEntity
        long nbFollow = getFollowNumber(datastore, userFollowKey);

        Key followEntityKey = KeyFactory.createKey("Follow", String.format("%s%s", userID, user.getId()));

        query = new Query("Follow").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                FilterOperator.EQUAL,
                                                                followEntityKey));
		pq = datastore.prepare(query);

        Entity response = new Entity("FollowState");
        response.setProperty("nbFollow", nbFollow);
        response.setProperty("userHasFollowed", pq.countEntities(fo) > 0);

		return response;
	}


    /*
     *  fullUserInfos Method
     *      
     *      Used to know informations about a user.
     * 
     *      NOTE: It can only be called normally if signIn has already been called successfully once.
     */
    @ApiMethod(name = "fullUserInfos", httpMethod = HttpMethod.GET)
	public Entity fullUserInfos(@Named("userID") String userID, User user) throws ForbiddenException, BadRequestException, UnauthorizedException, NotFoundException {

        //  Provided user must be valid
        if(user == null) throw new UnauthorizedException("Invalid credentials !");

        //  Provided userID must be not null
        if(userID == null) throw new BadRequestException("UserID is absent: either the userID doesn't exist or it is a mistake but the userID must be related to a valid and registered user !");

        //  Might need to process a regex on userID
        Pattern pattern = Pattern.compile("^[0-9]+$");
        Matcher matcher = pattern.matcher(userID);
        if(!matcher.matches()) throw new BadRequestException("UserID is invalid: either it is a mistake but the UserID must be related to a valid and registered user !");

        Key userKey = KeyFactory.createKey("User", user.getId());
        Key userRequestedKey = KeyFactory.createKey("User", userID);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        //  Verify that the user is present in our datastore
        Query query = new Query("User").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        userKey));
		PreparedQuery pq = datastore.prepare(query);
        FetchOptions fo = FetchOptions.Builder.withLimit(1);
        if(pq.countEntities(fo) == 0) throw new UnauthorizedException("Not registered: Please register before trying to fetch something !");

        //  Verify that the requested user is present in our datastore
        query = new Query("User").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        userRequestedKey));
		pq = datastore.prepare(query);
        if(pq.countEntities(fo) == 0) throw new NotFoundException("Not found: Please fetch a user that truely exist !");

        //  Everything is okay
        //  Now we can continue by creating the responseEntity

        Entity userRequested = pq.asSingleEntity();
        //  For privacy, we will remove this property from the served version
        userRequested.removeProperty("lastLogin");
        
        //  Add number of followers and if the user is currently liking this user
        userRequested.setProperty("nbFollow", getFollowNumber(datastore, userRequestedKey));

        //  Fetch and set property
        Key followEntityKey = KeyFactory.createKey("Follow", String.format("%s%s", userID, user.getId()));
        query = new Query("Follow").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                FilterOperator.EQUAL,
                                                                followEntityKey));
		pq = datastore.prepare(query);
        userRequested.setProperty("userHasFollowed", pq.countEntities(fo) > 0);

		return userRequested;
	}


    /*
     *  basicUserInfos Method
     *      
     *      Used to know basic informations about a user like pseudo, if the user is following and profile pictureURL.
     * 
     *      NOTE: It can only be called normally if signIn has already been called successfully once.
     */
    @ApiMethod(name = "basicUserInfos", httpMethod = HttpMethod.GET)
	public Entity basicUserInfos(@Named("userID") String userID, User user) throws ForbiddenException, BadRequestException, UnauthorizedException, NotFoundException {

        //  Provided user must be valid
        if(user == null) throw new UnauthorizedException("Invalid credentials !");

        //  Provided post id must be not null
        if(userID == null) throw new BadRequestException("UserID is absent: either the userID doesn't exist or it is a mistake but the userID must be related to a valid and registered user !");

        //  Might need to process a regex on userID
        Pattern pattern = Pattern.compile("^[0-9]+$");
        Matcher matcher = pattern.matcher(userID);
        if(!matcher.matches()) throw new BadRequestException("UserID is invalid: either it is a mistake but the UserID must be related to a valid and registered user !");

        Key userKey = KeyFactory.createKey("User", user.getId());
        Key userRequestedKey = KeyFactory.createKey("User", userID);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        //  Verify that the user is present in our datastore
        Query query = new Query("User").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        userKey));
		PreparedQuery pq = datastore.prepare(query);
        FetchOptions fo = FetchOptions.Builder.withLimit(1);
        if(pq.countEntities(fo) == 0) throw new UnauthorizedException("Not registered: Please register before trying to fetch something !");

        //  Verify that the requested user is present in our datastore
        query = new Query("User").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        userRequestedKey));
		pq = datastore.prepare(query);
        if(pq.countEntities(fo) == 0) throw new NotFoundException("Not found: Please fetch a user that truely exist !");

        //  Everything is okay
        //  Now we can continue by creating the responseEntity

        Entity userRequested = pq.asSingleEntity();
        //  For privacy, we will remove this property from the served version
        userRequested.removeProperty("firstName");
        userRequested.removeProperty("lastName");
        userRequested.removeProperty("lastLogin");

        //  Is user following the requested user ?
        //  Fetch and set property
        Key followEntityKey = KeyFactory.createKey("Follow", String.format("%s%s", userID, user.getId()));
        query = new Query("Follow").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                FilterOperator.EQUAL,
                                                                followEntityKey));
		pq = datastore.prepare(query);
        userRequested.setProperty("userHasFollowed", pq.countEntities(fo) > 0);

		return userRequested;
	}


    /*
     *  postInfos Method
     *      
     *      Used to know basic informations about a post like creator, content, number of likes and if the demanding user is liking it.
     * 
     *      NOTE: It can only be called normally if signIn has already been called successfully once.
     */
    @ApiMethod(name = "postInfos", httpMethod = HttpMethod.GET)
	public Entity postInfos(@Named("userID") String postID, User user) throws ForbiddenException, BadRequestException, UnauthorizedException, NotFoundException {

        //  Provided user must be valid
        if(user == null) throw new UnauthorizedException("Invalid credentials !");

        //  Provided postID must be not null
        if(postID == null) throw new BadRequestException("PostID is absent: either the postID doesn't exist or it is a mistake but the postID must be related to a valid and existing post !");

        //  Might need to process a regex on postID
        Pattern pattern = Pattern.compile("^[0-9]+$");
        Matcher matcher = pattern.matcher(postID);
        if(!matcher.matches()) throw new BadRequestException("PostID is invalid: either it is a mistake but the UserID must be related to a valid and existing post !");

        Key userKey = KeyFactory.createKey("User", user.getId());
        Key postRequestedKey = KeyFactory.createKey("Post", postID);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        //  Verify that the user is present in our datastore
        Query query = new Query("User").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        userKey));
		PreparedQuery pq = datastore.prepare(query);
        FetchOptions fo = FetchOptions.Builder.withLimit(1);
        if(pq.countEntities(fo) == 0) throw new UnauthorizedException("Not found: Please register before trying to fetch something !");

        //  Verify that the post is present in our datastore
        query = new Query("Post").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                        FilterOperator.EQUAL,
                                                                        postRequestedKey));
		pq = datastore.prepare(query);
        Entity postRequested = pq.asSingleEntity();
        if(postRequested == null) throw new NotFoundException("UserID is invalid: Please fetch a user that truely exist !");

        //  Everything is okay
        //  Now we will fetch likes and set properties around it
        Key likeEntityKey = KeyFactory.createKey("Like", String.format("%s%s", postID, user.getId()));
        query = new Query("Like").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                                FilterOperator.EQUAL,
                                                                likeEntityKey));
		pq = datastore.prepare(query);
        postRequested.setProperty("nbLikes", getLikeNumber(datastore, postRequestedKey));
        postRequested.setProperty("userHasLiked", pq.countEntities(fo) > 0);

		return postRequested;
	}
}