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

	final private RateLimitingBehaviour rateLimitingBehaviour;
	
	final private IDataStore dataStore;
	
	/*
	 * Constructors
	 */
	
	/***
	 * The most generic constructor. Assign everything
	 * @param GetDataStoreInstance()
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
		this.rateLimitingBehaviour = new RateLimitingBehaviour(RequestLimitHits,
				TimeLimitSeconds,
				storeHostileIPs,
				rateLimitByIP,
				rateLimitByUser,
				rateLimitByEndpoint,
				approvedUsersOnly);
		this.dataStore = dataStore;
	}
	
	/***
	 * Constructor generic to everything other 
	 * than predefined rate at which to limit
	 * @param GetDataStoreInstance()
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
		this.rateLimitingBehaviour = new RateLimitingBehaviour(storeHostileIPs,
				rateLimitByIP,
				rateLimitByUser,
				rateLimitByEndpoint,
				approvedUsersOnly);
		this.dataStore = dataStore;
	}
	
	/***
	 * Make a rate limiter which limits on End-points per User identities,
	 * but allow generic assignment of the approvedUsers and 
	 * the metrics by which the rate limiter operates.
	 * @param GetDataStoreInstance()
	 * @param RequestLimitHits
	 * @param TimeLimitSeconds
	 * @param approvedUsersOnly
	 */
	public RateLimiter(IDataStore dataStore, int RequestLimitHits, int TimeLimitSeconds, boolean approvedUsersOnly) {
		this.rateLimitingBehaviour = new RateLimitingBehaviour(RequestLimitHits,TimeLimitSeconds,approvedUsersOnly);
		this.dataStore = dataStore;
	}
	
	/***
	 * Least generic constructor. Makes a RateLimiter with the standard
	 * rate at which to limit, which limits on End-points per User Identities
	 * Must assign an IDataStore.
	 * @param GetDataStoreInstance()
	 */
	public RateLimiter(IDataStore dataStore) {
		this.rateLimitingBehaviour = new RateLimitingBehaviour();
		this.dataStore = dataStore;
	}
	
	/*
	 * Getter overrides
	 */
	
	@Override
	public RateLimitingBehaviour GetRateLimitingBehaviour() {
		return this.rateLimitingBehaviour;
	}
	
	
	@Override
	public IDataStore GetDataStoreInstance() {
		return this.dataStore;
	}
	
	/*
	 * AbstractRateLimiter overrides: The main functionality
	 */
	
	@Override
	public String IsAttemptRateLimited(RateLimitedIdentity RLIdentity) {
		boolean requestWasRateLimited = !GetDataStoreInstance().RecordNewAttempt(RLIdentity,
															  GetRequestLimitHits(),
															  GetTimeLimitSeconds());
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
		ServeHttpErrorResponse(printWriter,429,TryAgainMessage(nextAllowed));
	}
	
	/*
	 * AbstractRateLimiter overrides: Storing User Authorization functionality
	 */
	
	@Override
	public void StoreNewHttpAuthorization(String UserAuth) {
		GetDataStoreInstance().StoreUserAuth(UserAuth);
	}

	@Override
	public void StoreNewHttpBasicAuthorization(String username, 
											   String password) {
		byte[] basicAuthBytes = (username+":"+password).getBytes();
		byte[] encodedBasic = Base64.getEncoder().encode(basicAuthBytes);
		GetDataStoreInstance().StoreUserAuth("Basic " + new String(encodedBasic));
	}
	
	@Override
	public void ForgetExistingHttpAuthorization(String UserAuth) {
		GetDataStoreInstance().ForgetUserAuth(UserAuth);
	}
	
	@Override
	public void ForgetExistingHttpBasicAuthorization(String username, 
			   										 String password) {
		byte[] basicAuthBytes = (username+":"+password).getBytes();
		byte[] encodedBasic = Base64.getEncoder().encode(basicAuthBytes);
		GetDataStoreInstance().ForgetUserAuth("Basic " + new String(encodedBasic));
	}
	
	@Override
	public String ServeHttp40XPerUserAuth(PrintWriter printWriter, 
										  String UserAuth) {
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

}
