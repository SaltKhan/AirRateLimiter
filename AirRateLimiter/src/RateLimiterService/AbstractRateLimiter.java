package RateLimiterService;

import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import DataStore.IDataStore;
import DataStore.IDataStore.RateLimitedIdentity;

/***
 * Defines the standard operations of a Rate Limiting module which interacts
 * with the "rate limiting identity model" defined in the IDataStore, as
 * a prescriptive model of what an IDataStore implementation anticipates 
 * receiving. Also provides a way of assigning or recording and checking
 * against stored records for approved users, or hostile IP.
 */
public abstract class AbstractRateLimiter {
	
	/*
	 * The two components required of any rate limiter.
	 */
	
	/***
	 * @return The behaviour definition for the rate limiter
	 */
	abstract public RateLimitingBehaviour GetRateLimitingBehaviour();
	
	/***
	 * @return The IDataStore instance against which the AbstractRateLimiter stores data
	 */
	abstract public IDataStore GetDataStoreInstance();
	
	/*
	 * Getters
	 */
	
	/***
	 * @return How many requests are allowed per time-frame
	 */
	final public int GetRequestLimitHits(){
		return this.GetRateLimitingBehaviour().RequestLimitHits;
	}
	
	/***
	 * @return How long the time-frame is, in seconds, in which we allow the
	 * maximum number of requests.
	 */
	final public int GetTimeLimitSeconds(){
		return this.GetRateLimitingBehaviour().TimeLimitSeconds;
	}
	
	/***
	 * @return true if hostile IPs are being stored against the IDataStore,
	 * otherwise false
	 */
	final public boolean GetStoreHostileIPs() {
		return this.GetRateLimitingBehaviour().StoreHostileIPs;
	}
	
	/***
	 * @return true if we are rate limiting by IPs, otherwise false
	 */
	final public boolean GetRateLimitByIP() {
		return this.GetRateLimitingBehaviour().RateLimitByIP;
	}
	
	/***
	 * @return true if we are rate limiting by HTTP User Authorization,
	 * otherwise false
	 */
	final public boolean GetRateLimitByUser() {
		return this.GetRateLimitingBehaviour().RateLimitByUser;
	}

	/***
	 * @return true if we are rate limiting by end-points, otherwise false
	 */
	final public boolean GetRateLimitByEndpoint() {
		return this.GetRateLimitingBehaviour().RateLimitByEndpoint;
	}
	
	/***
	 * @return true if we are only allowing stored pre-approved HTTP User
	 * Authorization strings, otherwise false
	 */
	final public boolean GetApprovedUsersOnly() {
		return this.GetRateLimitingBehaviour().ApprovedUsersOnly;
	}
	
	/*
	 * The "main functionality" of the AbstractRateLimiter
	 * dealing with RateLimitedIdentity instances
	 * to rate limit or serve Http429 errors
	 */
	
	/***
	 * Forms the appropriate RateLimitedIdentity according to the RateLimiter's
	 * flags dictating whether we rate limit against IPs, Users, or either IP 
	 * or User against specific resource end-points. Observes this in priority,
	 * returning an end-point identity if we rate limit by identities otherwise
	 * a User identity if we rate limit against Users, or an IP if we rate
	 * limit against IPs, otherwise will return null.
	 * @param clientIP
	 * @param UserAuth
	 * @param endpoint
	 * @return
	 */
	final public RateLimitedIdentity GetRateLimitedIdentityFromRateLimiterContext(String clientIP, String UserAuth, String endpoint){
		if(GetRateLimitByEndpoint()) {
			String identity = GetEndpointIdentity(clientIP,UserAuth);
			if(identity.isEmpty()) {
				return null;
			} else {
				return IDataStore.NewRateLimitedEndpoint(identity,endpoint);
			}
		} else if(GetRateLimitByUser() && !UserAuth.isEmpty()) {
			return IDataStore.NewRateLimitedUser(UserAuth);
		} else if(GetRateLimitByIP()) {
			return IDataStore.NewRateLimitedIP(clientIP);
		} else {
			return null;
		}
	}
	
	/***
	 * Takes a RateLimitedIdentity, and queries the appropriate lookup table in
	 * the IDataStore to check if an Identity is rate limited. If the Identity
	 * is not rate limited, then it will record the attempt and return an empty
	 * string. If the attempt is rate limited, will yield an appropriate
	 * failure message to be served internally on the server, which gates
	 * sending a 429 on the presence of an empty or not empty string.
	 * @param rateLimitedIdentity
	 * @return
	 */
	public String IsAttemptRateLimited(RateLimitedIdentity RLIdentity) {
		boolean requestWasRateLimited = !GetDataStoreInstance().RecordNewAttempt(RLIdentity,GetRequestLimitHits(),GetTimeLimitSeconds());
		if(requestWasRateLimited) {
			switch(RLIdentity.GetRateLimitedIdentityType()) {
				case IP:
					return ("Found rate limited IP: " +
						    RLIdentity.GetIdentity());
				case User:
					return ("Found rate limited User: " +
						    RLIdentity.GetIdentity());
				case Endpoint:
					return ("Found rate limited Identity: " +
							RLIdentity.GetIdentity() +
							"; per resource " +
							RLIdentity.GetEndpoint());
				default:
					return "Request was Rate Limited but without Type";
		}
		} else {
			return "";
		}
	}
	
	/***
	 * Serves a simple Http429 to the handed output stream, with a message
	 * related to the Identity, as per the passed RateLimitedIdentity
	 * @param printWriter
	 * @param rateLimitedIdentity
	 */
	public void ServeHttp429PerAttempt(PrintWriter printWriter, RateLimitedIdentity RLIdentity) {
		LocalDateTime nextAllowed = CheckWhenNextRequestAllowed(RLIdentity);
		ServeHttpErrorResponse(printWriter,429,TryAgainMessage(nextAllowed));
	}
	
	/***
	 * Creating a rate limited identity relies on a consistent formatting for
	 * the "end-point" string, observed as the gluing of the HttpVerb and 
	 * the resource string being accessed. Assumes that the resource string
	 * has been cleaned by the Server class to represent an actual resource,
	 * i.e. clipped it to a known end-point that will be handled, so as to not
	 * store path arguments or query strings.
	 * @param httpVerb
	 * @param resource
	 * @return
	 */
	final public String FormEndpointStringFromVerbAndResource(String httpVerb, String resource) {
		return httpVerb+"|"+resource;
	}
	
	/*
	 * Non-main functionalities; Storing user authentication
	 * and serving Http401 or Http403 responses. Based on UserAuth 
	 * strings obtained through HTTP headers
	 */
	
	/***
	 * Store user authorization strings to be validated against a list of
	 * approved users, if we require users are validated
	 * @param UserAuth
	 */
	public void StoreNewHttpAuthorization(String UserAuth) {
		GetDataStoreInstance().StoreUserAuth(UserAuth);
	}
	
	/***
	 * Store user authorization strings formed as HTTP Basic Authorization to
	 * be validated against a list of approved users, if we 
	 * require users are validated
	 * @param username
	 * @param password
	 */
	public void StoreNewHttpBasicAuthorization(String username, String password) {
		byte[] basicAuthBytes = (username+":"+password).getBytes();
		byte[] encodedBasic = Base64.getEncoder().encode(basicAuthBytes);
		GetDataStoreInstance().StoreUserAuth("Basic " + new String(encodedBasic));
	}
	
	/***
	 * Forgets user authorization strings to be validated against a list of
	 * approved users, if we require users are validated
	 * @param UserAuth
	 */
	public void ForgetExistingHttpAuthorization(String UserAuth) {
		GetDataStoreInstance().ForgetUserAuth(UserAuth);
	}
	
	/***
	 * Forgets user authorization strings formed as HTTP Basic Authorization to
	 * be validated against a list of approved users, if we 
	 * require users are validated
	 * @param username
	 * @param password
	 */
	public void ForgetExistingHttpBasicAuthorization(String username, String password) {
		byte[] basicAuthBytes = (username+":"+password).getBytes();
		byte[] encodedBasic = Base64.getEncoder().encode(basicAuthBytes);
		GetDataStoreInstance().ForgetUserAuth("Basic " + new String(encodedBasic));
	}
	
	/***
	 * Used to serve a simple Http403 to indicate User Authorization received
	 * but invalid, or Http401 to indicate no User Authorization received!
	 * @param printWriter
	 * @param UserAuth
	 */
	public String ServeHttp40XPerUserAuth(PrintWriter printWriter, String UserAuth) {
		if(userAuthorizationExpectedButMissing(UserAuth)) {
			ServeHttpErrorResponse(printWriter,401,AbstractRateLimiter.Http401Response);
			return AbstractRateLimiter.Http401Response;
		} else if(userAuthorizationPresentButInvalid(UserAuth)) {
			ServeHttpErrorResponse(printWriter,403,AbstractRateLimiter.Http403Response);
			return AbstractRateLimiter.Http403Response;
		} else {
			return "";
		}
	}
	
	/***
	 * The String message to serve when serving an Http401 response
	 */
	static final public String Http401Response = ("Very 401. Such auth. You are required to submit HTTP Authorization!");
	
	/***
	 * The String message to serve when serving an Http403 response
	 */
	static final public String Http403Response = ("Very 403. Such auth. Your Authorization is invalid, and your bloodline is weak.");
	
	/***
	 * If rate limiting HTTP User Authorization Strings, return the 
	 * Authorization header, otherwise return the IP of the opened socket
	 * @param clientIP
	 * @param UserAuth
	 * @return
	 */
	private String GetEndpointIdentity(String clientIP, String UserAuth) {
		if(GetRateLimitByUser() && !UserAuth.isEmpty()) {
			return UserAuth;
		} else if(GetRateLimitByIP()) {
			return clientIP;
		} else {
			return "";
		}
	}
	
	/*
	 * AbstractRateLimiter overrides: Hostile IP functionality
	 */
	
	/***
	 * Returns true if the IP address for an opened socket is known hostile
	 * @param clientSocket
	 */
	final public boolean IsIPHostile(Socket clientSocket) {
		String IP = clientSocket.getInetAddress().getHostAddress();
		return (GetStoreHostileIPs() && this.GetDataStoreInstance().containsHostileIP(IP));
	}


	/***
	 * Stores the IP of a Socket in the IDataStore instance as being hostile.
	 * @param clientSocket
	 */
	final public void RecordIPAsHostile(Socket clientSocket) {
		if(GetStoreHostileIPs()) {
			String IP = clientSocket.getInetAddress().getHostAddress();
			GetDataStoreInstance().recordHostileIP(IP);
		}
	}
	
	/***
	 * Forgets the IP of a Socket in the IDataStore instance as being hostile.
	 * @param clientSocket
	 */
	final public void ForgetIPAsHostile(Socket clientSocket) {
		if(GetStoreHostileIPs()) {
			String IP = clientSocket.getInetAddress().getHostAddress();
			GetDataStoreInstance().removeHostileIP(IP);
		}
	}
	
	/*
	 * Helpers
	 */
	
	protected boolean userAuthorizationExpectedButMissing(String UserAuth) {
		return (UserAuth.isEmpty() && GetRateLimitByUser());
	}
	
	protected boolean userAuthorizationPresentButInvalid(String UserAuth) {
		return (!UserAuth.isEmpty() && !UserAuthIsValid(UserAuth));
	}
	
	// Serve Http response helpers
	
	protected void ServeHttpErrorResponse(PrintWriter printWriter, int HttpCode, String craftedErrorMessage) {
		printWriter.println("HTTP/1.1 "+HttpCode);
		printWriter.println();
		printWriter.println(craftedErrorMessage);
		printWriter.flush();
	}
	
	// Serve Http 429 helper for crafting the "try again after ..." message
	
	protected String TryAgainMessage(LocalDateTime next) {
		long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), next);
		return "Rate limit exceeded. Try again in "+seconds+" seconds";
	}
	
	
	protected LocalDateTime CheckWhenNextRequestAllowed(RateLimitedIdentity RLIdentity) {
		return GetDataStoreInstance().CheckWhenNextRequestAllowed(RLIdentity,GetRequestLimitHits(),GetTimeLimitSeconds());
	}
	
	protected boolean UserAuthIsValid(String UserAuth) {
		return (!GetApprovedUsersOnly() || GetDataStoreInstance().IsUserAuthValid(UserAuth));
	}
	
}
