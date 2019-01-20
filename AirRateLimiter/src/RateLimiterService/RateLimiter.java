package RateLimiterService;

import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import DataStore.IDataStore;

public class RateLimiter implements IRateLimiter {

	final private boolean storeHostileIPs;
	final private boolean rateLimitByIP;
	final private boolean rateLimitByUser;
	final private boolean rateLimitByEndpoint;
	final private boolean approvedUsersOnly;
	final private int RequestLimitHits;
	final private int TimeLimitSeconds;
	final private IDataStore dataStore;
	
	public RateLimiter(IDataStore dataStore, int RequestLimitHits, int TimeLimitSeconds, boolean storeHostileIPs, boolean rateLimitByIP, boolean rateLimitByUser, boolean rateLimitByEndpoint, boolean approvedUsersOnly, boolean demandsUserAuth) {
		this.RequestLimitHits = RequestLimitHits;
		this.TimeLimitSeconds = TimeLimitSeconds;
		this.storeHostileIPs = storeHostileIPs;
		this.rateLimitByIP = rateLimitByIP;
		this.rateLimitByUser = rateLimitByUser;
		this.rateLimitByEndpoint = rateLimitByEndpoint;
		this.approvedUsersOnly = approvedUsersOnly;
		this.dataStore = dataStore;
	}
	
	public RateLimiter(IDataStore dataStore, boolean storeHostileIPs, boolean rateLimitByIP, boolean rateLimitByUser, boolean rateLimitByEndpoint, boolean approvedUsersOnly, boolean demandsUserAuth) {
		this.RequestLimitHits = 100;
		this.TimeLimitSeconds = 3600;
		this.storeHostileIPs = storeHostileIPs;
		this.rateLimitByIP = rateLimitByIP;
		this.rateLimitByUser = rateLimitByUser;
		this.rateLimitByEndpoint = rateLimitByEndpoint;
		this.approvedUsersOnly = approvedUsersOnly;
		this.dataStore = dataStore;
	}
	
	public RateLimiter(IDataStore dataStore) {
		this.RequestLimitHits = 100;
		this.TimeLimitSeconds = 3600;
		this.storeHostileIPs = false;
		this.rateLimitByIP = false;
		this.rateLimitByUser = true;
		this.rateLimitByEndpoint = true;
		this.approvedUsersOnly = false;
		this.dataStore = dataStore;
	}
	
	public RateLimiter(IDataStore dataStore, int RequestLimitHits, int TimeLimitSeconds, boolean approvedUsersOnly) {
		this.RequestLimitHits = RequestLimitHits;
		this.TimeLimitSeconds = TimeLimitSeconds;
		this.storeHostileIPs = false;
		this.rateLimitByIP = false;
		this.rateLimitByUser = true;
		this.rateLimitByEndpoint = true;
		this.approvedUsersOnly = approvedUsersOnly;
		this.dataStore = dataStore;
	}
	
	@Override
	public boolean IsIPHostile(Socket clientSocket) {
		return (this.storeHostileIPs && this.dataStore.containsHostileIP(clientSocket.getInetAddress().getHostAddress()));
	}

	@Override
	public boolean IsIPRateLimited(String clientIP) {
		return (this.rateLimitByIP && !dataStore.RecordNewIPAttempt(clientIP,RequestLimitHits,TimeLimitSeconds));
	}

	@Override
	public boolean IsUserRateLimited(String auth) {
		return (this.rateLimitByUser && !auth.isEmpty() && !dataStore.RecordNewUserAttempt(auth,RequestLimitHits,TimeLimitSeconds));
	}

	@Override
	public boolean IsIdentityRateLimitedToEndPoint(String clientIP, String auth, String httpVerb, String resource) {
		String identity;
		if(this.rateLimitByUser) {
			//We can assume this will be called after checking that if we limit by user, auth is not empty
			identity = auth;
		} else {
			identity = clientIP;
		}
		return (this.rateLimitByEndpoint && !dataStore.RecordNewEndpointAttempt(identity,(httpVerb+"|"+resource),RequestLimitHits,TimeLimitSeconds));
	}

	@Override
	public void ServeHttp429PerIP(PrintWriter printWriter, String clientIP) {
		printWriter.println("HTTP/1.1 429");
		printWriter.println();
		LocalDateTime next = dataStore.CheckWhenNextRequestAllowedByIP(clientIP,RequestLimitHits,TimeLimitSeconds);
		printWriter.println("Rate limit exceeded. Try again in "+ChronoUnit.SECONDS.between(LocalDateTime.now(), next)+" seconds");
		printWriter.flush();
	}

	@Override
	public void ServeHttp429PerUser(PrintWriter printWriter, String auth) {
		printWriter.println("HTTP/1.1 429");
		printWriter.println();
		LocalDateTime next = dataStore.CheckWhenNextRequestAllowedByUser(auth,RequestLimitHits,TimeLimitSeconds);
		printWriter.println("Rate limit exceeded. Try again in "+ChronoUnit.SECONDS.between(LocalDateTime.now(), next)+" seconds");
		printWriter.flush();
	}

	@Override
	public void ServeHttp429PerEndpoint(PrintWriter printWriter, String clientIP, String auth, String httpVerb, String resource) {
		printWriter.println("HTTP/1.1 429");
		printWriter.println();
		String identity;
		if(this.rateLimitByUser) {
			//We can assume this will be called after checking that if we limit by user, auth is not empty
			identity = auth;
		} else {
			identity = clientIP;
		}
		LocalDateTime next = dataStore.CheckWhenNextEndpointRequestAllowedByIdentity(identity,(httpVerb+"|"+resource),RequestLimitHits,TimeLimitSeconds);
		printWriter.println("Rate limit exceeded. Try again in "+ChronoUnit.SECONDS.between(LocalDateTime.now(), next)+" seconds");
		printWriter.flush();
	}

	@Override
	public boolean RateLimitsByUser() {
		return this.rateLimitByUser;
	}

	@Override
	public void ServeHttp401PerUser(PrintWriter printWriter) {
		printWriter.println("HTTP/1.1 401");
		printWriter.println();
		printWriter.println("Very 401. Such auth. You are required to submit Authorization!");
		printWriter.flush();
	}

	@Override
	public void ServeHttp403PerUser(PrintWriter printWriter) {
		printWriter.println("HTTP/1.1 403");
		printWriter.println();
		printWriter.println("Very 403. Such auth. Your Authorization is invalid, and your bloodline is weak.");
		printWriter.flush();
	}

	@Override
	public boolean UserAuthIsValid(String UserAuth) {
		return (!this.approvedUsersOnly || dataStore.IsUserAuthValid(UserAuth));
	}

	@Override
	public void StoreNewUserAuth(String UserAuth) {
		dataStore.StoreUserAuth(UserAuth);
	}

	@Override
	public void StoreNewBasicAuth(String username, String password) {
		dataStore.StoreUserAuth("Basic " + new String(Base64.getEncoder().encode((username+":"+password).getBytes())));
	}

}
