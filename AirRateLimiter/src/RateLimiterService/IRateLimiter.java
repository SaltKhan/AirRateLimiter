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
public interface IRateLimiter {
	
	/*
	 * Define the standard rate at which to rate limit requests.
	 * Also define the standard implementation's behaviour
	 */
	
	/***
	 * The standard rate at which to rate limit is 100 requests per 
	 * "TimeLimitSeconds_Standard" seconds
	 */
	static final public int RequestLimitHits_Standard = 100;
	
	/***
	 * The standard rate at which to rate limit is to accept 
	 * "RequestLimitHits_Standard" many requests per 3600 seconds.
	 */
	static final public int TimeLimitSeconds_Standard = 3600;
	
	/***
	 * The standard rate limiting implementation is to not store hostile IP
	 */
	static final public boolean StoreHostileIPs_Standard = false;
	
	/***
	 * The standard rate limiting implementation is to not limit by IP
	 */
	static final public boolean RateLimitByIP_Standard = false;
	
	/***
	 * The standard rate limiting implementation is to limit by User
	 */
	static final public boolean RateLimitByUser_Standard = true;
	
	/***
	 * The standard rate limiting implementation is to limit
	 * Identities relative to specific resources requested
	 */
	static final public boolean RateLimitByEndpoint_Standard = true;
	
	/***
	 * The standard rate limiting implementation is to permissively
	 * allow any one with "User Authorization" to be allowed, not
	 * just those that have been approved and stored already.
	 */
	static final public boolean ApprovedUsersOnly_Standard = false;
	
	/*
	 * Also define the common getters for these two properties that
	 * any implementation of the IRateLimiter would be expected to
	 * utilize while performing the rate limiting operations.
	 * Also return the instance of the IDataStore that is 
	 * used by the IRateLimiter.
	 */
	
	/***
	 * @return How many requests are allowed per time-frame
	 */
	public int GetRequestLimitHits();
	
	/***
	 * @return How long the time-frame is, in seconds, in which we allow the
	 * maximum number of requests.
	 */
	public int GetTimeLimitSeconds();
	
	/***
	 * Get the IDataStore instance against which the IRateLimiter stores data
	 */
	public IDataStore GetIRateLimitersIDataStoreInstance();
	
	/*
	 * The "main functionality" of the IRateLimiter
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
	public RateLimitedIdentity 
				GetRateLimitedIdentityFromRateLimiterContext(String clientIP,
															 String UserAuth,
															 String endpoint);
	
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
	public String 
		IsAttemptRateLimited(RateLimitedIdentity rateLimitedIdentity);
	
	/***
	 * Serves a simple Http429 to the handed output stream, with a message
	 * related to the Identity, as per the passed RateLimitedIdentity
	 * @param printWriter
	 * @param rateLimitedIdentity
	 */
	public void ServeHttp429PerAttempt(PrintWriter printWriter, 
									  RateLimitedIdentity rateLimitedIdentity);
	
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
	public void StoreNewHttpAuthorization(String UserAuth);
	
	/***
	 * Store user authorization strings formed as HTTP Basic Authorization to
	 * be validated against a list of approved users, if we 
	 * require users are validated
	 * @param username
	 * @param password
	 */
	public void StoreNewHttpBasicAuthorization(String username, 
								  			   String password);
	
	/***
	 * Forgets user authorization strings to be validated against a list of
	 * approved users, if we require users are validated
	 * @param UserAuth
	 */
	public void ForgetExistingHttpAuthorization(String UserAuth);
	
	/***
	 * Forgets user authorization strings formed as HTTP Basic Authorization to
	 * be validated against a list of approved users, if we 
	 * require users are validated
	 * @param username
	 * @param password
	 */
	public void ForgetExistingHttpBasicAuthorization(String username, 
								  			   		 String password);
	
	/***
	 * Used to serve a simple Http403 to indicate User Authorization received
	 * but invalid, or Http401 to indicate no User Authorization received!
	 * @param printWriter
	 * @param UserAuth
	 */
	public String ServeHttp40XPerUserAuth(PrintWriter printWriter, 
										  String UserAuth);
	
	/***
	 * The String message to serve when serving an Http401 response
	 */
	static final public String Http401Response = ("Very 401. Such auth. You " +
	                             "are required to submit HTTP Authorization!");
	
	/***
	 * The String message to serve when serving an Http403 response
	 */
	static final public String Http403Response = ("Very 403. Such auth. Your" +
	                 " Authorization is invalid, and your bloodline is weak.");
	
	/*
	 * Non-main functionalities: Check for a hostile IP
	 * And formalize the construction of the "end-point" string.
	 */
	
	/***
	 * Returns true if the IP address for an opened socket is known hostile
	 * @param clientSocket
	 */
	public boolean IsIPHostile(Socket clientSocket);
	
	/***
	 * Stores the IP of a Socket in the IDataStore instance as being hostile.
	 * @param clientSocket
	 */
	public void RecordIPAsHostile(Socket clientSocket);
	
	/***
	 * Forgets the IP of a Socket in the IDataStore instance as being hostile.
	 * @param clientSocket
	 */
	public void ForgetIPAsHostile(Socket clientSocket);
	
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
	public String FormEndpointStringFromVerbAndResource(String httpVerb, 
														String resource);
	
}
