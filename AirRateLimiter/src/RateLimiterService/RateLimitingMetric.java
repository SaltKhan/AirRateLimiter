package RateLimiterService;

public class RateLimitingMetric {
	
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
	 * The set rate at which to rate limit is "RequestLimitHits" requests per 
	 * "TimeLimitSeconds" seconds
	 */
	final public int RequestLimitHits;
	
	/***
	 * The set rate at which to rate limit is to accept 
	 * "RequestLimitHits" many requests per "TimeLimitSeconds" seconds.
	 */
	final public int TimeLimitSeconds;
	
	public RateLimitingMetric() {
		RequestLimitHits = RequestLimitHits_Standard;
		TimeLimitSeconds = TimeLimitSeconds_Standard;
	}
	
	public RateLimitingMetric(int RequestLimitHits, int TimeLimitSeconds) {
		this.RequestLimitHits = RequestLimitHits;
		this.TimeLimitSeconds = TimeLimitSeconds;
	}

}
