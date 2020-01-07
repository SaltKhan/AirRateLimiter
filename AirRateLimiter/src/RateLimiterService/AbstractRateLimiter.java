package RateLimiterService;

import java.io.PrintWriter;
import java.net.Socket;

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
	public abstract RateLimitedIdentity GetRateLimitedIdentityFromRateLimiterContext(String clientIP, String UserAuth, String endpoint);  //TODO
	
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
	public abstract String IsAttemptRateLimited(RateLimitedIdentity rateLimitedIdentity);  //TODO
	
	/***
	 * Serves a simple Http429 to the handed output stream, with a message
	 * related to the Identity, as per the passed RateLimitedIdentity
	 * @param printWriter
	 * @param rateLimitedIdentity
	 */
	public abstract void ServeHttp429PerAttempt(PrintWriter printWriter, RateLimitedIdentity rateLimitedIdentity); //TODO
	
	/*
	 * Non-main functionalities: Check for a hostile IP
	 * And formalize the construction of the "end-point" string.
	 */
	
	/***
	 * Returns true if the IP address for an opened socket is known hostile
	 * @param clientSocket
	 */
	public abstract boolean IsIPHostile(Socket clientSocket); //TODO
	
	
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
	public abstract String FormEndpointStringFromVerbAndResource(String httpVerb, String resource); //TODO
	
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
	abstract public void StoreNewHttpAuthorization(String UserAuth);
	
	/***
	 * Store user authorization strings formed as HTTP Basic Authorization to
	 * be validated against a list of approved users, if we 
	 * require users are validated
	 * @param username
	 * @param password
	 */
	abstract public void StoreNewHttpBasicAuthorization(String username, String password);
	
	/***
	 * Forgets user authorization strings to be validated against a list of
	 * approved users, if we require users are validated
	 * @param UserAuth
	 */
	abstract public void ForgetExistingHttpAuthorization(String UserAuth);
	
	/***
	 * Forgets user authorization strings formed as HTTP Basic Authorization to
	 * be validated against a list of approved users, if we 
	 * require users are validated
	 * @param username
	 * @param password
	 */
	abstract public void ForgetExistingHttpBasicAuthorization(String username, String password);
	
	/***
	 * Used to serve a simple Http403 to indicate User Authorization received
	 * but invalid, or Http401 to indicate no User Authorization received!
	 * @param printWriter
	 * @param UserAuth
	 */
	abstract public String ServeHttp40XPerUserAuth(PrintWriter printWriter, String UserAuth); //TODO
	
	/***
	 * The String message to serve when serving an Http401 response
	 */
	static final public String Http401Response = ("Very 401. Such auth. You are required to submit HTTP Authorization!");
	
	/***
	 * The String message to serve when serving an Http403 response
	 */
	static final public String Http403Response = ("Very 403. Such auth. Your Authorization is invalid, and your bloodline is weak.");
	
	/*
	 * Non-main functionalities: Check for a hostile IP
	 * And formalize the construction of the "end-point" string.
	 */

	/***
	 * Stores the IP of a Socket in the IDataStore instance as being hostile.
	 * @param clientSocket
	 */
	abstract public void RecordIPAsHostile(Socket clientSocket);
	
	/***
	 * Forgets the IP of a Socket in the IDataStore instance as being hostile.
	 * @param clientSocket
	 */
	abstract public void ForgetIPAsHostile(Socket clientSocket);

}
