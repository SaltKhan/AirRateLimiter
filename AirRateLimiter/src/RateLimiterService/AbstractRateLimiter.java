package RateLimiterService;

import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;

import RateLimiterService.RateLimitedIdentity.RateLimitedIdentityType;

/***
 * Defines the standard operations of a Rate Limiting module which interacts
 * with the "rate limiting identity model". Includes a range of generic "rate
 * limiting" organisational code, with the algorithm used to implement the rate
 * limiting functionality to be abstracted, and required of the implementing
 * class. In this way, the server class utilises an instance of the abstract
 * rate limiter, using functions that aren't abstract, which themselves rely
 * on internally abstracted functions.
 */
public abstract class AbstractRateLimiter {
	
	/***
	 * @return The behaviour definition for the rate limiter
	 */
	abstract protected RateLimitingBehaviour getRateLimitingBehaviour();
	
	/*
	 * Getters for the contents of the RateLimitingBehaviour instance
	 */
	
	/***
	 * @return How many requests are allowed per time-frame
	 */
	final public int requestLimitHits(){
		return this.getRateLimitingBehaviour().RequestLimitHits;
	}
	
	/***
	 * @return How long the time-frame is, in seconds, in which we allow the
	 * maximum number of requests.
	 */
	final public int timeLimitSeconds(){
		return this.getRateLimitingBehaviour().TimeLimitSeconds;
	}
	
	/***
	 * @return true if hostile IPs are being stored against the IDataStore,
	 * otherwise false
	 */
	final public boolean storingHostileIPs() {
		return this.getRateLimitingBehaviour().StoreHostileIPs;
	}
	
	/***
	 * @return true if we are rate limiting by IPs, otherwise false
	 */
	final public boolean rateLimitingByIP() {
		return this.getRateLimitingBehaviour().RateLimitByIP;
	}
	
	/***
	 * @return true if we are rate limiting by HTTP User Authorization,
	 * otherwise false
	 */
	final public boolean rateLimitingByUser() {
		return this.getRateLimitingBehaviour().RateLimitByUser;
	}

	/***
	 * @return true if we are rate limiting by end-points, otherwise false
	 */
	final public boolean rateLimitingByEndpoint() {
		return this.getRateLimitingBehaviour().RateLimitByEndpoint;
	}
	
	/***
	 * @return true if we are only allowing stored pre-approved HTTP User
	 * Authorization strings, otherwise false
	 */
	final public boolean allowingApprovedUsersOnly() {
		return this.getRateLimitingBehaviour().ApprovedUsersOnly;
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
	final public RateLimitedIdentity getRateLimitedIdentityFromRateLimiterContext(String clientIP, String UserAuth, String endpoint){
		if(rateLimitingByEndpoint()) {
			return nullOrNewRateLimitedEndpoint(clientIP,UserAuth,endpoint);
		} else if(rateLimitingByUser() && !UserAuth.isEmpty()) {
			return NewRateLimitedUser(UserAuth);
		} else if(rateLimitingByIP()) {
			return NewRateLimitedIP(clientIP);
		} else {
			return null;
		}
	}
	
	/***
	 * @param clientIP
	 * @param UserAuth
	 * @param endpoint
	 * @return A new rate limited endpoint identity, or null if the identity is empty.
	 */
	private RateLimitedIdentity nullOrNewRateLimitedEndpoint(String clientIP, String UserAuth, String endpoint) {
		String identity = GetEndpointIdentity(clientIP,UserAuth);
		if(identity.isEmpty()) {
			return null;
		} else {
			return NewRateLimitedEndpoint(identity,endpoint);
		}
	}
	
	/***
	 * Takes a RateLimitedIdentity, and queries the appropriate lookup table 
	 * to check if an Identity is rate limited. If the Identity
	 * is not rate limited, then it will record the attempt and return an empty
	 * string. If the attempt is rate limited, will yield an appropriate
	 * failure message to be served internally on the server, which gates
	 * sending a 429 on the presence of an empty or not empty string.
	 * @param rateLimitedIdentity
	 * @return
	 */
	final public String IsAttemptRateLimited(RateLimitedIdentity RLIdentity) {
		boolean requestWasRateLimited = !RecordNewAttempt(RLIdentity,requestLimitHits(),timeLimitSeconds());
		if(requestWasRateLimited) {
			switch(RLIdentity.GetRateLimitedIdentityType()) {
				case IP:
					return ("Found rate limited IP: "+RLIdentity.GetIdentity());
				case User:
					return ("Found rate limited User: "+RLIdentity.GetIdentity());
				case Endpoint:
					return ("Found rate limited Identity: "+RLIdentity.GetIdentity()+"; per resource "+RLIdentity.GetEndpoint());
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
	final public void ServeHttp429PerAttempt(PrintWriter printWriter, RateLimitedIdentity RLIdentity) {
		LocalDateTime nextAllowed = CheckWhenNextRequestAllowed(RLIdentity);
		ServeHttpErrorResponse(printWriter,429,TryAgainMessage(nextAllowed));
	}
	
	/***
	 * Uses the "next" time when a request will be allowed through the rate
	 * limiter to craft the "try again in X seconds" message.
	 * @param next
	 * @return
	 */
	private String TryAgainMessage(LocalDateTime next) {
		long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), next);
		return "Rate limit exceeded. Try again in "+seconds+" seconds";
	}
	
	/***
	 * Checks what the next time that a request will be allowed is.
	 * @param RLIdentity
	 * @return
	 */
	private LocalDateTime CheckWhenNextRequestAllowed(RateLimitedIdentity RLIdentity) {
		return CheckWhenNextRequestAllowed(RLIdentity,requestLimitHits(),timeLimitSeconds());
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
	final public void StoreNewHttpAuthorization(String UserAuth) {
		StoreUserAuth(UserAuth);
	}
	
	/***
	 * Store user authorization strings formed as HTTP Basic Authorization to
	 * be validated against a list of approved users, if we 
	 * require users are validated
	 * @param username
	 * @param password
	 */
	final public void StoreNewHttpBasicAuthorization(String username, String password) {
		byte[] basicAuthBytes = (username+":"+password).getBytes();
		byte[] encodedBasic = Base64.getEncoder().encode(basicAuthBytes);
		StoreUserAuth("Basic " + new String(encodedBasic));
	}
	
	/***
	 * Forgets user authorization strings to be validated against a list of
	 * approved users, if we require users are validated
	 * @param UserAuth
	 */
	final public void ForgetExistingHttpAuthorization(String UserAuth) {
		ForgetUserAuth(UserAuth);
	}
	
	/***
	 * Forgets user authorization strings formed as HTTP Basic Authorization to
	 * be validated against a list of approved users, if we 
	 * require users are validated
	 * @param username
	 * @param password
	 */
	final public void ForgetExistingHttpBasicAuthorization(String username, String password) {
		byte[] basicAuthBytes = (username+":"+password).getBytes();
		byte[] encodedBasic = Base64.getEncoder().encode(basicAuthBytes);
		ForgetUserAuth("Basic " + new String(encodedBasic));
	}
	
	/***
	 * Used to serve a simple Http403 to indicate User Authorization received
	 * but invalid, or Http401 to indicate no User Authorization received!
	 * @param printWriter
	 * @param UserAuth
	 */
	final public String ServeHttp40XPerUserAuth(PrintWriter printWriter, String UserAuth) {
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
	static final public String Http401Response = "Very 401. Such auth. You are required to submit HTTP Authorization!";
	
	/***
	 * The String message to serve when serving an Http403 response
	 */
	static final public String Http403Response = "Very 403. Such auth. Your Authorization is invalid, and your bloodline is weak.";
	
	/***
	 * If rate limiting HTTP User Authorization Strings, return the 
	 * Authorization header, otherwise return the IP of the opened socket
	 * @param clientIP
	 * @param UserAuth
	 * @return
	 */
	private String GetEndpointIdentity(String clientIP, String UserAuth) {
		if(rateLimitingByUser() && !UserAuth.isEmpty()) {
			return UserAuth;
		} else if(rateLimitingByIP()) {
			return clientIP;
		} else {
			return "";
		}
	}
	
	/***
	 * Used to require that an implementing subclass has a member variable 
	 * that is an ArrayList<String>, and it should be FINAL, as an abstract
	 * class is not allowed final objects.
	 * @return
	 */
	abstract protected ArrayList<String> getValidUserAuths();
	
	/***
	 * Store a user Authorization string in a list of known users
	 * @param UserAuth
	 */
	final public void StoreUserAuth(String UserAuth){
		AddToArrayList(getValidUserAuths(),UserAuth);
	}
	
	/***
	 * Forget a user Authorization string from a list of known users
	 * @param UserAuth
	 */
	final public void ForgetUserAuth(String UserAuth){
		RemoveFromArrayList(getValidUserAuths(),UserAuth);
	}
	
	/***
	 * Query the data store whether it contains a given user's Authorization
	 * @param UserAuth
	 * @return
	 */
	final public boolean IsUserAuthValid(String UserAuth){
		return DoesArrayListContainValue(getValidUserAuths(),UserAuth);
	}
	
	/*
	 * Non-main functionalities; Hostile IP functionality
	 */
	
	/***
	 * Returns true if the IP address for an opened socket is known hostile
	 * @param clientSocket
	 */
	final public boolean IsIPHostile(Socket clientSocket) {
		String IP = clientSocket.getInetAddress().getHostAddress();
		return (storingHostileIPs() && this.containsHostileIP(IP));
	}


	/***
	 * Stores the IP of a Socket in the IDataStore instance as being hostile.
	 * @param clientSocket
	 */
	final public void RecordIPAsHostile(Socket clientSocket) {
		if(storingHostileIPs()) {
			String IP = clientSocket.getInetAddress().getHostAddress();
			recordHostileIP(IP);
		}
	}
	
	/***
	 * Forgets the IP of a Socket in the IDataStore instance as being hostile.
	 * @param clientSocket
	 */
	final public void ForgetIPAsHostile(Socket clientSocket) {
		if(storingHostileIPs()) {
			String IP = clientSocket.getInetAddress().getHostAddress();
			removeHostileIP(IP);
		}
	}
	
	/***
	 * Used to require that an implementing subclass has a member variable 
	 * that is an ArrayList<String>, and it should be FINAL, as an abstract
	 * class is not allowed final objects.
	 * @return
	 */
	abstract protected ArrayList<String> getHostileIPs();
	
	/***
	 * Query the data store for hostile IP
	 * @param IP
	 * @return
	 */
	final public boolean containsHostileIP(String IP) {
		return DoesArrayListContainValue(getHostileIPs(),IP);
	}
	
	/***
	 * Records a new hostile IP
	 * @param IP
	 */
	final public void recordHostileIP(String IP) {
		AddToArrayList(getHostileIPs(),IP);
	}
	
	/***
	 * Removes a tracked hostile IP
	 * @param IP
	 */
	final public void removeHostileIP(String IP) {
		RemoveFromArrayList(getHostileIPs(),IP);
	}
	
	/*
	 * Helpers
	 */
	
	private boolean userAuthorizationExpectedButMissing(String UserAuth) {
		return (UserAuth.isEmpty() && rateLimitingByUser());
	}
	
	private boolean userAuthorizationPresentButInvalid(String UserAuth) {
		return (!UserAuth.isEmpty() && !UserAuthIsValid(UserAuth));
	}

	private boolean UserAuthIsValid(String UserAuth) {
		return (!allowingApprovedUsersOnly() || IsUserAuthValid(UserAuth));
	}
	
	// Serve Http response helpers
	
	private void ServeHttpErrorResponse(PrintWriter printWriter, int HttpCode, String craftedErrorMessage) {
		printWriter.println("HTTP/1.1 "+HttpCode);
		printWriter.println();
		printWriter.println(craftedErrorMessage);
		printWriter.flush();
	}
	
	/* STATIC METHODS TO GET NEW RateLimitedIdentity INSTANCES
	 * Functions that will generate new RateLimitedIdentity as per the
	 * required enum to reference the identity's type.
	 */
	
	/***
	 * Define a method to return an inner instance that encapsulates 
	 * the "Rate limited identity" when that identity is an IP
	 * @param identity
	 * @param endpoint
	 * @return
	 */
	final public static RateLimitedIdentity NewRateLimitedIP(String IP) {
		return new RateLimitedIdentity(IP,null,RateLimitedIdentityType.IP);
	}
	
	/***
	 * Define a method to return an inner instance that encapsulates 
	 * the "Rate limited identity" when that identity is a User
	 * @param identity
	 * @param endpoint
	 * @return
	 */
	final public static RateLimitedIdentity NewRateLimitedUser(String User) {
		return new RateLimitedIdentity(User,null,RateLimitedIdentityType.User);
	}
	
	/***
	 * Define a method to return an inner instance that encapsulates the 
	 * "Rate limited identity" when that identity is an End-point Identity
	 * @param identity
	 * @param endpoint
	 * @return
	 */
	final public static RateLimitedIdentity NewRateLimitedEndpoint(String identity,String endpoint) {
		return new RateLimitedIdentity(identity,endpoint,RateLimitedIdentityType.Endpoint);
	}

	/*
	 * Functions that take a RateLimitedIdentity to record a new attempt
	 * or check when the next request by that identity will be allowed.
	 * The RecordNewAttempt and CheckWhenNextRequestAllowed are the two to be
	 * overridden by an implementing subclass to define the rate limiting
	 * behaviour / functionality.
	 */
	
	/***
	 * Query the data store for availability to record a 
	 * new attempt from a rateLimitedIdentity, if it is
	 * available to create a new record, record this attempt
	 * @param rateLimitedIdentity
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	abstract public boolean RecordNewAttempt(RateLimitedIdentity rateLimitedIdentity, int maxAttempts, int maxSeconds);
	
	/***
	 * Query the data store to check when the next available request 
	 * by an identity will be allowed. If the next attempt is allowed now, 
	 * will return the current time
	 * @param rateLimitedIdentity
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	abstract public LocalDateTime CheckWhenNextRequestAllowed(RateLimitedIdentity rateLimitedIdentity, int maxAttempts, int maxSeconds);

	
	/*
	 * Other helper functions moved from the rate limiter into this abstract class
	 */
	
	/***
	 * Checks the presence of a value in an ArrayList
	 * @param list
	 * @param Value
	 * @return
	 */
	protected <T> boolean DoesArrayListContainValue(ArrayList<T> list, T Value){
		return list.contains(Value);
	}
	
	/***
	 * Add a value to an ArrayList
	 * @param list
	 * @param Value
	 */
	protected <T> void AddToArrayList(ArrayList<T> list, T Value){
		if(!DoesArrayListContainValue(list,Value)) {
			list.add(Value);
		}
	}
	
	/***
	 * Remove a value from an ArrayList
	 * @param list
	 * @param Value
	 */
	protected <T> void RemoveFromArrayList(ArrayList<T> list, T Value){
		if(DoesArrayListContainValue(list,Value)) {
			list.remove(Value);
		}
	}
	
}
