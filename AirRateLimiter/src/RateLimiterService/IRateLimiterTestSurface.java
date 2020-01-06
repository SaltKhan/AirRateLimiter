package RateLimiterService;

import java.io.PrintWriter;
import java.net.Socket;

import DataStore.IDataStore;

public interface IRateLimiterTestSurface {
	
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
	 * any implementation of the AbstractRateLimiter would be expected to
	 * utilize while performing the rate limiting operations.
	 * Also return the instance of the IDataStore that is 
	 * used by the AbstractRateLimiter.
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
	 * Get the IDataStore instance against which the AbstractRateLimiter stores data
	 */
	public IDataStore GetIRateLimitersIDataStoreInstance();
	

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
	public void StoreNewHttpBasicAuthorization(String username, String password);
	
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
	public void ForgetExistingHttpBasicAuthorization(String username, String password);
	
	/***
	 * Used to serve a simple Http403 to indicate User Authorization received
	 * but invalid, or Http401 to indicate no User Authorization received!
	 * @param printWriter
	 * @param UserAuth
	 */
	public String ServeHttp40XPerUserAuth(PrintWriter printWriter, String UserAuth); //TODO
	
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
	public void RecordIPAsHostile(Socket clientSocket);
	
	/***
	 * Forgets the IP of a Socket in the IDataStore instance as being hostile.
	 * @param clientSocket
	 */
	public void ForgetIPAsHostile(Socket clientSocket);

}
