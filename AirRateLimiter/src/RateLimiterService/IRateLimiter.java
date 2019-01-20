package RateLimiterService;

import java.io.PrintWriter;
import java.net.Socket;

public interface IRateLimiter {
	
	/***
	 * Returns true if the Rate Limiter expects User Auth
	 * @return
	 */
	public boolean RateLimitsByUser();
	
	/***
	 * Returns true if the user authorization is valid per the DataStore
	 * @param UserAuth
	 * @return
	 */
	public boolean UserAuthIsValid(String UserAuth);
	
	/***
	 * Store a new user in the list of approved users
	 * @param UserAuth
	 */
	public void StoreNewUserAuth(String UserAuth);
	
	/***
	 * Stores a Basic Auth as a new user to the list of approved users
	 * @param username
	 * @param password
	 */
	public void StoreNewBasicAuth(String username, String password);
	
	/***
	 * Used to serve a simple Http429 with a message related to the IP
	 * @param printWriter
	 * @param clientIP
	 */
	public void ServeHttp429PerIP(PrintWriter printWriter, String clientIP);
	
	/***
	 * Used to serve a simple Http429 with a message related to the User
	 * @param printWriter
	 * @param auth
	 */
	public void ServeHttp429PerUser(PrintWriter printWriter, String auth);
	
	/***
	 * Used to serve a simple Http401 to indicate no User Auth received!
	 * @param printWriter
	 */
	public void ServeHttp401PerUser(PrintWriter printWriter);
	
	/***
	 * Used to serve a simple Http403 to indicate User Auth received but invalid!
	 * @param printWriter
	 */
	public void ServeHttp403PerUser(PrintWriter printWriter);
	
	/***
	 * Used to serve a simple Http429 with a message related to the Identity (IP or User)
	 * @param printWriter
	 * @param clientIP
	 * @param auth
	 * @param httpVerb
	 * @param resource
	 */
	public void ServeHttp429PerEndpoint(PrintWriter printWriter, String clientIP, String auth, String httpVerb, String resource);
	
	/***
	 * Returns if the IP for the opened socket is known hostile
	 * @param clientSocket
	 * @return
	 */
	public boolean IsIPHostile(Socket clientSocket);
	
	/***
	 * Returns if we have served too many requests by an IP, and records the attempt
	 * @param clientIP
	 * @return
	 */
	public boolean IsIPRateLimited(String clientIP);
	
	/***
	 * Returns if we have served too many requests by a User, and records the attempt
	 * @param auth
	 * @return
	 */
	public boolean IsUserRateLimited(String auth);
	
	/***
	 * Returns if we have served too many requests by an Identity accessing an endpoint, and records the attempt
	 * @param clientIP
	 * @param auth
	 * @param httpVerb
	 * @param resource
	 * @return
	 */
	public boolean IsIdentityRateLimitedToEndPoint(String clientIP, String auth, String httpVerb, String resource);
	
}
