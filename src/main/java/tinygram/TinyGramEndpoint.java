package tinygram;

import java.util.Date;
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
import com.google.api.server.spi.response.UnauthorizedException;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
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

public class TinyGramEndpoint {

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
        if(userInfos.pseudo.length() > 16) throw new BadRequestException("User informations too long: Pseudonym might be too long !");
        if(userInfos.fname.length() > 51 || userInfos.lname.length() > 51) throw new BadRequestException("User informations too long: firstName or lastName might be too long !");

        //  Check if the provided picture profile URL is from google
        Pattern pattern = Pattern.compile("^https:\\/\\/lh[0-9]\\.googleusercontent\\.com\\/a\\/[a-zA-Z0-9]+=.*$");
        Matcher matcher = pattern.matcher(userInfos.pictureURL);

        if(!matcher.matches()) throw new BadRequestException("Invalid profile picture URL: Profile picture URL must be a picture of a google account !");
        
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        //  Verify that the user is not already present
        Query query = new Query("User").setFilter(new FilterPredicate("userID",
                                                                        FilterOperator.EQUAL,
                                                                        user.getId()));

		PreparedQuery pq = datastore.prepare(query);
        FetchOptions fo = FetchOptions.Builder.withLimit(0);
        if(pq.countEntities(fo) > 0) throw new UnauthorizedException("User already registered !");

        //  Verify that the pseudonym is not already taken by an other user
        query = new Query("User").setFilter(new FilterPredicate("pseudo",
                                                                        FilterOperator.EQUAL,
                                                                        userInfos.pseudo));
		pq = datastore.prepare(query);
        if(pq.countEntities(fo) > 0) throw new UnauthorizedException("Pseudonym already taken, PLEASE choose another one !");

        //  Generate user entity that will be returned and stored
        Entity userEntity = new Entity("User", user.getId());
        userEntity.setProperty("userID", user.getId());
        userEntity.setProperty("pseudo", userInfos.pseudo);
        userEntity.setProperty("firstName", userInfos.fname);
        userEntity.setProperty("lastName", userInfos.lname);
        userEntity.setProperty("pictureUrl", userInfos.pictureURL);
        userEntity.setProperty("lastLogin", new Date());

        datastore.put(userEntity);

		return userEntity;
	}

    @ApiMethod(name = "connect", httpMethod = HttpMethod.GET)
	public Entity connect(User user) throws UnauthorizedException {

        //  Provided user must be valid
        if(user == null) throw new UnauthorizedException("Invalid credentials");
        
        //  Search the user entity in our datastore
        Query query = new Query("User").setFilter(new FilterPredicate("userID",
                                                                        FilterOperator.EQUAL,
                                                                        user.getId()));
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


    @ApiMethod(name = "postPublication", httpMethod = HttpMethod.POST)
	public Entity postPublication(User user, PostIn post) throws ForbiddenException, BadRequestException, UnauthorizedException {

        //  Provided user must be valid
        if(user == null) throw new UnauthorizedException("Invalid credentials !");

        //  Filter low performance cost verification parameters
        if(post == null) throw new BadRequestException("Post is absent: We cannot post void as a post that will cause a paradox and this will create a devastating black hole, so don't do that again seriously !");
        if(post.body != null && post.body.length() > 280) throw new BadRequestException("Body post's too long: Body lenght exceeding the 280 caracters limit");
        if(post.pictureURL == null || post.pictureURL.isEmpty()) throw new BadRequestException("Picture post's is absent: Picture URL must be provided, this cannot be empty !");

        //  Might need a step to verify post infos to avoid injection or to verify provided pictureURL

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        //  Verify that the user is present in our datastore
        Query query = new Query("User").setFilter(new FilterPredicate("userID",
                                                                        FilterOperator.EQUAL,
                                                                        user.getId()));
		PreparedQuery pq = datastore.prepare(query);
        FetchOptions fo = FetchOptions.Builder.withLimit(1);
        if(pq.countEntities(fo) == 0) throw new UnauthorizedException("Not registered: Please register before trying to post something !");

        //  Good the user is registered
        //  Now let's create post entity
		Entity e = new Entity("Post");
		e.setProperty("postID", e.getKey());
		e.setProperty("creatorID", user.getId());
        e.setProperty("pictureUrl", post.body);
		e.setProperty("pictureUrl", post.pictureURL);
		e.setProperty("date", new Date());

        //  Make sure that the user will no try to post the same post at the same time
		Transaction txn = datastore.beginTransaction();
		datastore.put(e);
		txn.commit();
		return e;
	}

    @ApiMethod(name = "followUser", httpMethod = HttpMethod.GET)
	public Entity followUser(User user, @Named("FollowedUserID") String followedUserID) throws ForbiddenException, BadRequestException, UnauthorizedException {

        //  Provided user must be valid
        if(user == null) throw new UnauthorizedException("Invalid credentials !");

        //  FollowedUserID verification
        //  Google doesn't provide a clear definition of what can or what cannot be a userID
        //  So might need to be adjusted in case of errors
        Pattern pattern = Pattern.compile("^[0-9]+$");
        Matcher matcher = pattern.matcher(followedUserID);
        
        if(followedUserID == null || !matcher.matches()) throw new BadRequestException("FollowedUserID is absent: either the user is a ghost or it is a mistake but the ID must be related to a valid and registred user account !");


        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        //  Verify that the user is present in our datastore
        Query query = new Query("User").setFilter(new FilterPredicate("userID",
                                                                        FilterOperator.EQUAL,
                                                                        user.getId()));
		PreparedQuery pq = datastore.prepare(query);
        FetchOptions fo = FetchOptions.Builder.withLimit(1);
        if(pq.countEntities(fo) == 0) throw new UnauthorizedException("Not registered: Please register before trying to post something !");

        //  Verify that the followrdUser is present in our datastore
        query = new Query("User").setFilter(new FilterPredicate("userID",
                                                                        FilterOperator.EQUAL,
                                                                        followedUserID));
		pq = datastore.prepare(query);
        if(pq.countEntities(fo) == 0) throw new UnauthorizedException("FollowedUser not registered: An user cannot follow an unregistered user !");

        //  Good the user is registered
        //  Now let's create post entity
		Entity followEntity = new Entity("Follow");
		followEntity.setProperty("userID", user.getId());
		followEntity.setProperty("followedID", followedUserID);
		followEntity.setProperty("date", new Date());

        //  Make sure that the user will no try to post the same post at the same time
		Transaction txn = datastore.beginTransaction();
		datastore.put(followEntity);
		txn.commit();
		return followEntity;
	}
}