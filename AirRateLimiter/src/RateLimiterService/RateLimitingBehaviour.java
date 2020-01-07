package RateLimiterService;

/***
 * Define the standard rate at which to rate limit requests.
 * Also define the standard implementation's behaviour
 */
public class RateLimitingBehaviour {
	
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
	
	/***
	 * The set rate at which to rate limit is "RequestLimitHits" requests per 
	 * "TimeLimitSeconds" seconds
	 */
	final public int RequestLimitHits;
	
	/***
	 * The set rate at which to rate limit is to accept 
	 * "RequestLimitHits" many requests per "TimeLimitSeconds" seconds.
	 */
	final public int TimeLimitSeconds;
	
	/***
	 * The set rate limiting implementation regarding hostile IP
	 */
	final public boolean StoreHostileIPs;
	
	/***
	 * The set rate limiting implementation regarding limiting by IP
	 */
	final public boolean RateLimitByIP;
	
	/***
	 * The set rate limiting implementation regarding limiting by User
	 */
	final public boolean RateLimitByUser;
	
	/***
	 * The set rate limiting implementation regarding limiting
	 * Identities relative to specific resources requested
	 */
	final public boolean RateLimitByEndpoint;
	
	/***
	 * The set rate limiting implementation is to permissively
	 * allow any one with "User Authorization" to be allowed, not
	 * just those that have been approved and stored already.
	 */
	final public boolean ApprovedUsersOnly;
	
	/***
	 * Initialise a rate limiting behaviour with all the standard options
	 */
	public RateLimitingBehaviour() {
		this.RequestLimitHits = RequestLimitHits_Standard;
		this.TimeLimitSeconds = TimeLimitSeconds_Standard;
		this.StoreHostileIPs = StoreHostileIPs_Standard;
		this.RateLimitByIP = RateLimitByIP_Standard;
		this.RateLimitByUser = RateLimitByUser_Standard;
		this.RateLimitByEndpoint = RateLimitByEndpoint_Standard;
		this.ApprovedUsersOnly = ApprovedUsersOnly_Standard;
	}
	
	/***
	 * Initialise a rate limiting behaviour with the standard "limit by"
	 * options, with different requests/seconds ratio.
	 */
	public RateLimitingBehaviour(int RequestLimitHits, int TimeLimitSeconds) {
		this.RequestLimitHits = RequestLimitHits;
		this.TimeLimitSeconds = TimeLimitSeconds;
		this.StoreHostileIPs = StoreHostileIPs_Standard;
		this.RateLimitByIP = RateLimitByIP_Standard;
		this.RateLimitByUser = RateLimitByUser_Standard;
		this.RateLimitByEndpoint = RateLimitByEndpoint_Standard;
		this.ApprovedUsersOnly = ApprovedUsersOnly_Standard;
	}
	
	/***
	 * Initialise a rate limiting behaviour with the standard "limit by"
	 * options, with different requests/seconds ratio.
	 */
	public RateLimitingBehaviour(int RequestLimitHits, int TimeLimitSeconds, boolean ApprovedUsersOnly) {
		this.RequestLimitHits = RequestLimitHits;
		this.TimeLimitSeconds = TimeLimitSeconds;
		this.StoreHostileIPs = StoreHostileIPs_Standard;
		this.RateLimitByIP = RateLimitByIP_Standard;
		this.RateLimitByUser = RateLimitByUser_Standard;
		this.RateLimitByEndpoint = RateLimitByEndpoint_Standard;
		this.ApprovedUsersOnly = ApprovedUsersOnly;
	}
	
	/***
	 * Initialise a customisable rate limiting behaviour.
	 * @param RequestLimitHits
	 * @param TimeLimitSeconds
	 * @param StoreHostileIPs
	 * @param RateLimitByIP
	 * @param RateLimitByUser
	 * @param RateLimitByEndpoint
	 * @param ApprovedUsersOnly
	 */
	public RateLimitingBehaviour(int RequestLimitHits, 
			int TimeLimitSeconds, 
			boolean StoreHostileIPs, 
			boolean RateLimitByIP, 
			boolean RateLimitByUser, 
			boolean RateLimitByEndpoint, 
			boolean ApprovedUsersOnly) {
		this.RequestLimitHits = RequestLimitHits;
		this.TimeLimitSeconds = TimeLimitSeconds;
		this.StoreHostileIPs = StoreHostileIPs;
		this.RateLimitByIP = RateLimitByIP;
		this.RateLimitByUser = RateLimitByUser;
		this.RateLimitByEndpoint = RateLimitByEndpoint;
		this.ApprovedUsersOnly = ApprovedUsersOnly;
	}
	
	/***
	 * Initialise a customisable rate limiting behaviour.
	 * @param StoreHostileIPs
	 * @param RateLimitByIP
	 * @param RateLimitByUser
	 * @param RateLimitByEndpoint
	 * @param ApprovedUsersOnly
	 */
	public RateLimitingBehaviour(boolean StoreHostileIPs, 
			boolean RateLimitByIP, 
			boolean RateLimitByUser, 
			boolean RateLimitByEndpoint, 
			boolean ApprovedUsersOnly) {
		this.RequestLimitHits = RequestLimitHits_Standard;
		this.TimeLimitSeconds = TimeLimitSeconds_Standard;
		this.StoreHostileIPs = StoreHostileIPs;
		this.RateLimitByIP = RateLimitByIP;
		this.RateLimitByUser = RateLimitByUser;
		this.RateLimitByEndpoint = RateLimitByEndpoint;
		this.ApprovedUsersOnly = ApprovedUsersOnly;
	}

}
