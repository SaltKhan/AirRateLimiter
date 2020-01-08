package RateLimiterService;

/***
 * Define a public inner class to be passed from a RateLimiter 
 * implementation to the IDataStore implementation
 */
public class RateLimitedIdentity{
	
	public enum RateLimitedIdentityType {
		IP,
		User,
		Endpoint;
	}
	
	private final String identity;
	private final String endpoint;
	private final RateLimitedIdentityType rateLimitedIdentityType;
	
	RateLimitedIdentity(String identity, String endpoint, RateLimitedIdentityType rateLimitedIdentityType){
		this.identity = identity;
		this.endpoint = endpoint;
		this.rateLimitedIdentityType = rateLimitedIdentityType;
	}
	
	public String GetIdentity() {
		return this.identity;
	}
	
	public String GetEndpoint() {
		return this.endpoint;
	}
	
	public RateLimitedIdentityType GetRateLimitedIdentityType() {
		return this.rateLimitedIdentityType;
	}
	
	public boolean IsIdentityAnEndpointAttempt() {
		return (this.rateLimitedIdentityType == RateLimitedIdentityType.Endpoint);
	}
	
};
