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
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.UnauthorizedException;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

import tinygram.entities.PostIn;

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

    private String getProfilePicture(User user) {
        if(user == null) return null;
        return "http://profiles.google.com/s2/photos/profile/" + user.getId();
    }

    @ApiMethod(name = "signIn", httpMethod = HttpMethod.GET)
	public Entity signIn(User user, @Named("pseudo") String pseudo, @Named("firstName") String fname, @Named("lastName") String lname) throws BadRequestException, UnauthorizedException {

        //  Filter low performance cost verification parameters
        if(user == null) throw new UnauthorizedException("Invalid credentials");
        if(pseudo == null || pseudo.isEmpty()) throw new BadRequestException("Invalid parameters: pseudo cannot be null or empty !");
        if(fname == null || fname.isEmpty()) throw new BadRequestException("Invalid firstName: firstname cannot be null or empty !");
        if(lname == null || lname.isEmpty()) throw new BadRequestException("Invalid lastName: lastname cannot be null or empty !");

        //  Avoid too long informations
        if(pseudo.length() > 16) throw new BadRequestException("User informations too long: Pseudonym might be too long !");
        if(fname.length() > 51 || lname.length() > 51) throw new BadRequestException("User informations too long: firstName or lastName might be too long !");

        //  Might need a step to verify user infos to avoid injection
        
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
                                                                        pseudo));
		pq = datastore.prepare(query);
        if(pq.countEntities(fo) > 0) throw new UnauthorizedException("Pseudonym already taken, PLEASE choose another one !");

        //  Generate user entity that will be returned and stored
        Entity userEntity = new Entity("User", user.getId());
        userEntity.setProperty("userID", user.getId());
        userEntity.setProperty("pseudo", pseudo);
        userEntity.setProperty("firstName", fname);
        userEntity.setProperty("lastName", lname);
        userEntity.setProperty("pictureUrl", getProfilePicture(user));
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


    @ApiMethod(name = "postPublication", httpMethod = HttpMethod.GET)
	public Entity postPublication(PostIn post, User user) throws ForbiddenException, BadRequestException, UnauthorizedException {

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