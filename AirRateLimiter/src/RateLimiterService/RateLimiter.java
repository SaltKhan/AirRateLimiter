package RateLimiterService;

import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import DataStore.IDataStore;
import DataStore.IDataStore.RateLimitedIdentity;

/***
 * Implements the expectations of the AbstractRateLimiter
 */
public class RateLimiter extends AbstractRateLimiter {

	final private boolean storeHostileIPs;
	final private boolean rateLimitByIP;
	final private boolean rateLimitByUser;
	final private boolean rateLimitByEndpoint;
	final private boolean approvedUsersOnly;
	final private int RequestLimitHits;
	final private int TimeLimitSeconds;
	final private IDataStore dataStore;
	
	/*
	 * Constructors
	 */
	
	/***
	 * The most generic constructor. Assign everything
	 * @param dataStore
	 * @param RequestLimitHits
	 * @param TimeLimitSeconds
	 * @param storeHostileIPs
	 * @param rateLimitByIP
	 * @param rateLimitByUser
	 * @param rateLimitByEndpoint
	 * @param approvedUsersOnly
	 */
	public RateLimiter(IDataStore dataStore, 
					   int RequestLimitHits, 
					   int TimeLimitSeconds, 
					   boolean storeHostileIPs, 
					   boolean rateLimitByIP, 
					   boolean rateLimitByUser, 
					   boolean rateLimitByEndpoint, 
					   boolean approvedUsersOnly) {
		this.RequestLimitHits = RequestLimitHits;
		this.TimeLimitSeconds = TimeLimitSeconds;
		this.storeHostileIPs = storeHostileIPs;
		this.rateLimitByIP = rateLimitByIP;
		this.rateLimitByUser = rateLimitByUser;
		this.rateLimitByEndpoint = rateLimitByEndpoint;
		this.approvedUsersOnly = approvedUsersOnly;
		this.dataStore = dataStore;
	}
	
	/***
	 * Constructor generic to everything other 
	 * than predefined rate at which to limit
	 * @param dataStore
	 * @param storeHostileIPs
	 * @param rateLimitByIP
	 * @param rateLimitByUser
	 * @param rateLimitByEndpoint
	 * @param approvedUsersOnly
	 * @param demandsUserAuth
	 */
	public RateLimiter(IDataStore dataStore, 
					   boolean storeHostileIPs, 
					   boolean rateLimitByIP, 
					   boolean rateLimitByUser, 
					   boolean rateLimitByEndpoint, 
					   boolean approvedUsersOnly, 
					   boolean demandsUserAuth) {
		this.RequestLimitHits = AbstractRateLimiter.RequestLimitHits_Standard;
		this.TimeLimitSeconds = AbstractRateLimiter.TimeLimitSeconds_Standard;
		this.storeHostileIPs = storeHostileIPs;
		this.rateLimitByIP = rateLimitByIP;
		this.rateLimitByUser = rateLimitByUser;
		this.rateLimitByEndpoint = rateLimitByEndpoint;
		this.approvedUsersOnly = approvedUsersOnly;
		this.dataStore = dataStore;
	}
	
	/***
	 * Make a rate limiter which limits on End-points per User identities,
	 * but allow generic assignment of the approvedUsers and 
	 * the metrics by which the rate limiter operates.
	 * @param dataStore
	 * @param RequestLimitHits
	 * @param TimeLimitSeconds
	 * @param approvedUsersOnly
	 */
	public RateLimiter(IDataStore dataStore, 
					   int RequestLimitHits, 
					   int TimeLimitSeconds, 
					   boolean approvedUsersOnly) {
		this.RequestLimitHits = RequestLimitHits;
		this.TimeLimitSeconds = TimeLimitSeconds;
		this.storeHostileIPs = AbstractRateLimiter.StoreHostileIPs_Standard;
		this.rateLimitByIP = AbstractRateLimiter.RateLimitByIP_Standard;
		this.rateLimitByUser = AbstractRateLimiter.RateLimitByUser_Standard;
		this.rateLimitByEndpoint = AbstractRateLimiter.RateLimitByEndpoint_Standard;
		this.approvedUsersOnly = approvedUsersOnly;
		this.dataStore = dataStore;
	}
	
	/***
	 * Least generic constructor. Makes a RateLimiter with the standard
	 * rate at which to limit, which limits on End-points per User Identities
	 * Must assign an IDataStore.
	 * @param dataStore
	 */
	public RateLimiter(IDataStore dataStore) {
		this.RequestLimitHits = AbstractRateLimiter.RequestLimitHits_Standard;
		this.TimeLimitSeconds = AbstractRateLimiter.TimeLimitSeconds_Standard;
		this.storeHostileIPs = AbstractRateLimiter.StoreHostileIPs_Standard;
		this.rateLimitByIP = AbstractRateLimiter.RateLimitByIP_Standard;
		this.rateLimitByUser = AbstractRateLimiter.RateLimitByUser_Standard;
		this.rateLimitByEndpoint = AbstractRateLimiter.RateLimitByEndpoint_Standard;
		this.approvedUsersOnly = AbstractRateLimiter.ApprovedUsersOnly_Standard;
		this.dataStore = dataStore;
	}
	
	/*
	 * Getters
	 */
	
	/***
	 * @return true if hostile IPs are being stored against the IDataStore,
	 * otherwise false
	 */
	public boolean GetStoreHostileIPs() {
		return this.storeHostileIPs;
	}
	
	/***
	 * @return true if we are rate limiting by IPs, otherwise false
	 */
	public boolean GetRateLimitByIP() {
		return this.rateLimitByIP;
	}
	
	/***
	 * @return true if we are rate limiting by HTTP User Authorization,
	 * otherwise false
	 */
	public boolean GetRateLimitsByUser() {
		return this.rateLimitByUser;
	}

	/***
	 * @return true if we are rate limiting by end-points, otherwise false
	 */
	public boolean GetRateLimitByEndpoint() {
		return this.rateLimitByEndpoint;
	}
	
	/***
	 * @return true if we are only allowing stored pre-approved HTTP User
	 * Authorization strings, otherwise false
	 */
	public boolean GetApprovedUsersOnly() {
		return this.approvedUsersOnly;
	}
	
	/*
	 * AbstractRateLimiter overrides: The three getters that are stipulated
	 */
	
	@Override
	public int GetRequestLimitHits() {
		return this.RequestLimitHits;
	}
	
	@Override
	public int GetTimeLimitSeconds() {
		return this.TimeLimitSeconds;
	}
	
	@Override
	public IDataStore GetIRateLimitersIDataStoreInstance() {
		return this.dataStore;
	}
	
	/*
	 * AbstractRateLimiter overrides: The main functionality
	 */
	
	@Override
	public RateLimitedIdentity GetRateLimitedIdentityFromRateLimiterContext(
															 String clientIP,
													         String UserAuth,
															 String endpoint) {
		if(this.rateLimitByEndpoint) {
			String identity = GetEndpointIdentity(clientIP,UserAuth);
			if(identity.isEmpty()) {
				return null;
			} else {
				return IDataStore.NewRateLimitedEndpoint(identity,endpoint);
			}
		} else if(this.rateLimitByUser && !UserAuth.isEmpty()) {
			return IDataStore.NewRateLimitedUser(UserAuth);
		} else if(this.rateLimitByIP) {
			return IDataStore.NewRateLimitedIP(clientIP);
		} else {
			return null;
		}
	}
	
	@Override
	public String IsAttemptRateLimited(RateLimitedIdentity RLIdentity) {
		boolean requestWasRateLimited = !dataStore.RecordNewAttempt(RLIdentity,
															  RequestLimitHits,
															 TimeLimitSeconds);
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

	@Override
	public void ServeHttp429PerAttempt(PrintWriter printWriter, 
									   RateLimitedIdentity RLIdentity) {
		LocalDateTime nextAllowed = CheckWhenNextRequestAllowed(RLIdentity);
		ServeHttpResponse(printWriter,429,TryAgainMessage(nextAllowed));
	}
	
	/*
	 * AbstractRateLimiter overrides: Storing User Authorization functionality
	 */
	
	@Override
	public void StoreNewHttpAuthorization(String UserAuth) {
		dataStore.StoreUserAuth(UserAuth);
	}

	@Override
	public void StoreNewHttpBasicAuthorization(String username, 
											   String password) {
		byte[] basicAuthBytes = (username+":"+password).getBytes();
		byte[] encodedBasic = Base64.getEncoder().encode(basicAuthBytes);
		dataStore.StoreUserAuth("Basic " + new String(encodedBasic));
	}
	
	@Override
	public void ForgetExistingHttpAuthorization(String UserAuth) {
		dataStore.ForgetUserAuth(UserAuth);
	}
	
	@Override
	public void ForgetExistingHttpBasicAuthorization(String username, 
			   										 String password) {
		byte[] basicAuthBytes = (username+":"+password).getBytes();
		byte[] encodedBasic = Base64.getEncoder().encode(basicAuthBytes);
		dataStore.ForgetUserAuth("Basic " + new String(encodedBasic));
	}
	
	@Override
	public String ServeHttp40XPerUserAuth(PrintWriter printWriter, 
										  String UserAuth) {
		if(userAuthorizationExpectedButMissing(UserAuth)) {
			ServeHttpResponse(printWriter,401,AbstractRateLimiter.Http401Response);
			return AbstractRateLimiter.Http401Response;
		} else if(userAuthorizationPresentButInvalid(UserAuth)) {
			ServeHttpResponse(printWriter,403,AbstractRateLimiter.Http403Response);
			return AbstractRateLimiter.Http403Response;
		} else {
			return "";
		}
	}
	
	/*
	 * AbstractRateLimiter overrides: Hostile IP functionality
	 */
	
	@Override
	public boolean IsIPHostile(Socket clientSocket) {
		String IP = clientSocket.getInetAddress().getHostAddress();
		return (this.storeHostileIPs && this.dataStore.containsHostileIP(IP));
	}

	@Override
	public void RecordIPAsHostile(Socket clientSocket) {
		if(this.storeHostileIPs) {
			String IP = clientSocket.getInetAddress().getHostAddress();
			dataStore.recordHostileIP(IP);
		}
	}
	
	@Override
	public void ForgetIPAsHostile(Socket clientSocket) {
		if(this.storeHostileIPs) {
			String IP = clientSocket.getInetAddress().getHostAddress();
			dataStore.removeHostileIP(IP);
		}
	}
	
	/*
	 * AbstractRateLimiter overrides: "Other" functionality
	 */
	
	@Override
	public String FormEndpointStringFromVerbAndResource(String httpVerb, String resource) {
		return httpVerb+"|"+resource;
	}
	
	/*
	 * Helpers
	 */
	
	
	private boolean userAuthorizationExpectedButMissing(String UserAuth) {
		return (UserAuth.isEmpty() && this.rateLimitByUser);
	}
	
	private boolean userAuthorizationPresentButInvalid(String UserAuth) {
		return (!UserAuth.isEmpty() && !UserAuthIsValid(UserAuth));
	}
	
	/***
	 * If rate limiting HTTP User Authorization Strings, return the 
	 * Authorization header, otherwise return the IP of the opened socket
	 * @param clientIP
	 * @param UserAuth
	 * @return
	 */
	private String GetEndpointIdentity(String clientIP, String UserAuth) {
		if(this.rateLimitByUser && !UserAuth.isEmpty()) {
			return UserAuth;
		} else if(this.rateLimitByIP) {
			return clientIP;
		} else {
			return "";
		}
	}
	
	// Serve Http response helpers
	
	private void ServeHttpResponse(PrintWriter printWriter, 
						           int HttpCode, 
						           String craftedErrorMessage) {
		printWriter.println("HTTP/1.1 "+HttpCode);
		printWriter.println();
		printWriter.println(craftedErrorMessage);
		printWriter.flush();
	}
	
	// Serve Http 429 helper for crafting the "try again after ..." message
	
	private String TryAgainMessage(LocalDateTime next) {
		long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), next);
		return "Rate limit exceeded. Try again in "+seconds+" seconds";
	}
	
	
	private LocalDateTime CheckWhenNextRequestAllowed(
											RateLimitedIdentity RLIdentity) {
		return dataStore.CheckWhenNextRequestAllowed(RLIdentity,
													 RequestLimitHits,
													 TimeLimitSeconds);
	}
	
	private boolean UserAuthIsValid(String UserAuth) {
		return (!this.approvedUsersOnly || dataStore.IsUserAuthValid(UserAuth));
	}

}
