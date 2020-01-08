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
	
}
