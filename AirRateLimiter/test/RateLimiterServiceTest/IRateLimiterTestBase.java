package RateLimiterServiceTest;

import java.util.ArrayList;
import java.util.Base64;

import DataStore.DataStore;
import DataStore.IDataStore;
import RateLimiterService.IRateLimiter;
import RateLimiterService.RateLimiter;

/***
 * Base class to provide spin-up of SUT objects related to the IRateLimiter
 */
public class IRateLimiterTestBase {
	
	/***
	 * How many requests to we want to allow to be serviced per 
	 * "TimeLimitSeconds_Test" seconds
	 */
	static final public int RequestLimitHits_Test = 5;
	
	/***
	 * How many seconds do we want to use as the window in which to allow
	 * "RequestLimitHits_Test" requests
	 */
	static final public int TimeLimitSeconds_Test = 2;
	
	static final public String IP_Test = "8.8.8.8";
	static final public String UserAuth_Test = "Basic " + new String(Base64.getEncoder().encode(("Bob:1234").getBytes()));
	static final public String IP_Test_2 = "4.4.4.4";
	static final public String UserAuth_Test_2 = "Basic " + new String(Base64.getEncoder().encode(("Alice:5678").getBytes()));
	static final public String verb_Test = "GET";
	static final public String resource_Test = "gg/m8";
	
	/* The following "NewTestRateLimiter" overloads are simply
	 * invocations of the 4 constructors of the RateLimiter
	 */
	
	/***
	 * Returns instance of the RateLimiter implementation of the IRateLimiter,
	 * returned as a reference to an IRateLimiter implementation so as to limit
	 * testing to the surface of the RateLimiter exposed by the IRateLimiter.
	 * This is the most interactive constructor available
	 */
	public static IRateLimiter NewTestRateLimiter(IDataStore dataStore, 
			   int RequestLimitHits, 
			   int TimeLimitSeconds, 
			   boolean storeHostileIPs, 
			   boolean rateLimitByIP, 
			   boolean rateLimitByUser, 
			   boolean rateLimitByEndpoint, 
			   boolean approvedUsersOnly) {
		return new RateLimiter(dataStore, 
				   			RequestLimitHits, 
				   			TimeLimitSeconds, 
				   			storeHostileIPs, 
				   			rateLimitByIP, 
				   			rateLimitByUser, 
				   			rateLimitByEndpoint, 
				   			approvedUsersOnly);
	}
	
	/***
	 * Returns instance of the RateLimiter implementation of the IRateLimiter,
	 * returned as a reference to an IRateLimiter implementation so as to limit
	 * testing to the surface of the RateLimiter exposed by the IRateLimiter.
	 * This is the second most interactive constructor available
	 */
	public static IRateLimiter NewTestRateLimiter(IDataStore dataStore, 
			   boolean storeHostileIPs, 
			   boolean rateLimitByIP, 
			   boolean rateLimitByUser, 
			   boolean rateLimitByEndpoint, 
			   boolean approvedUsersOnly, 
			   boolean demandsUserAuth) {
		return new RateLimiter(dataStore,
				   			storeHostileIPs, 
				   			rateLimitByIP, 
				   			rateLimitByUser, 
				   			rateLimitByEndpoint, 
				   			approvedUsersOnly, 
				   			demandsUserAuth);
	}
	
	/***
	 * Returns instance of the RateLimiter implementation of the IRateLimiter,
	 * returned as a reference to an IRateLimiter implementation so as to limit
	 * testing to the surface of the RateLimiter exposed by the IRateLimiter.
	 * This is the second least interactive constructor available
	 */
	public static IRateLimiter NewTestRateLimiter(IDataStore dataStore, 
			   							int RequestLimitHits, 
			   							int TimeLimitSeconds, 
			   							boolean approvedUsersOnly) {
		return new RateLimiter(dataStore, 
				   			RequestLimitHits, 
				   			TimeLimitSeconds, 
				   			approvedUsersOnly);
	}
	
	/***
	 * Returns instance of the RateLimiter implementation of the IRateLimiter,
	 * returned as a reference to an IRateLimiter implementation so as to limit
	 * testing to the surface of the RateLimiter exposed by the IRateLimiter.
	 * This is the least interactive constructor available
	 */
	public static IRateLimiter NewTestRateLimiter(IDataStore dataStore) {
		return new RateLimiter(dataStore);
	}
	
	/* The following "Make<<behavior>:?><Limiting:?>IRateLimiter" methods 
	 * provide an abstracted way to produce an instance of the RateLimiter 
	 * while specifically enforcing some particular characteristics or set of
	 * inputs that define the behavior of the RateLimiter, giving access to
	 * provide inputs for all inputs in the generic constructor not required
	 * to enforce the specific behavior expected of the returned RateLimiter
	 */
	
	/***
	 * Provides selection to all constructor variables of the RateLimiter aside
	 * from those that are used to set the rate limiting operation to only rate
	 * limit against the IP
	 * @param maxAttempts
	 * @param maxSeconds
	 * @param storeHostileIPs
	 * @param approvedUsersOnly
	 * @return
	 */
	static public IRateLimiter MakeIPLimitingIRateLimiter(int maxAttempts, int maxSeconds, boolean storeHostileIPs, boolean approvedUsersOnly) {
		IDataStore dataStore = new DataStore();
		return NewTestRateLimiter(dataStore, maxAttempts, maxSeconds, storeHostileIPs, true, false, false, approvedUsersOnly);
	}
	
	/***
	 * Provides selection to all constructor variables of the RateLimiter aside
	 * from those that are used to set the rate limiting operation to only rate
	 * limit against the User Authorization
	 * @param maxAttempts
	 * @param maxSeconds
	 * @param storeHostileIPs
	 * @param approvedUsersOnly
	 * @return
	 */
	static public IRateLimiter MakeUserLimitingIRateLimiter(int maxAttempts, int maxSeconds, boolean storeHostileIPs, boolean approvedUsersOnly) {
		IDataStore dataStore = new DataStore();
		return NewTestRateLimiter(dataStore, maxAttempts, maxSeconds, storeHostileIPs, false, true, false, approvedUsersOnly);
	}

	/***
	 * Provides selection to all constructor variables of the RateLimiter aside
	 * from those that are used to set the rate limiting operation to only rate
	 * limit against the End-point being accessed, recorded against IP
	 * @param maxAttempts
	 * @param maxSeconds
	 * @param storeHostileIPs
	 * @param approvedUsersOnly
	 * @return
	 */
	static public IRateLimiter MakeEndpointByIPLimitingIRateLimiter(int maxAttempts, int maxSeconds, boolean storeHostileIPs, boolean approvedUsersOnly) {
		IDataStore dataStore = new DataStore();
		return NewTestRateLimiter(dataStore, maxAttempts, maxSeconds, storeHostileIPs, true, false, true, approvedUsersOnly);
	}

	/***
	 * Provides selection to all constructor variables of the RateLimiter aside
	 * from those that are used to set the rate limiting operation to only rate
	 * limit against the End-point being accessed, recorded against Users
	 * @param maxAttempts
	 * @param maxSeconds
	 * @param storeHostileIPs
	 * @param approvedUsersOnly
	 * @return
	 */
	static public IRateLimiter MakeEndpointByUserLimitingIRateLimiter(int maxAttempts, int maxSeconds, boolean storeHostileIPs, boolean approvedUsersOnly) {
		IDataStore dataStore = new DataStore();
		return NewTestRateLimiter(dataStore, maxAttempts, maxSeconds, storeHostileIPs, false, true, true, approvedUsersOnly);
	}

	/***
	 * Provides selection to all constructor variables of the RateLimiter aside
	 * from those that are used to enforce only allowing authorized users
	 * @param maxAttempts
	 * @param maxSeconds
	 * @param storeHostileIPs
	 * @param rateLimitByIP
	 * @param rateLimitByUser
	 * @param rateLimitByEndpoint
	 * @return
	 */
	static public IRateLimiter MakeUserAuthorizationCheckingIRateLimiter(int maxAttempts, int maxSeconds, boolean storeHostileIPs, boolean rateLimitByIP, boolean rateLimitByUser, boolean rateLimitByEndpoint) {
		IDataStore dataStore = new DataStore();
		return NewTestRateLimiter(dataStore, maxAttempts, maxSeconds, storeHostileIPs, rateLimitByIP, rateLimitByUser, rateLimitByEndpoint, true);
	}

	/***
	 * Provides selection to all constructor variables of the RateLimiter aside
	 * from those that are used to enforce checking for hostile IPs
	 * @param maxAttempts
	 * @param maxSeconds
	 * @param rateLimitByIP
	 * @param rateLimitByUser
	 * @param rateLimitByEndpoint
	 * @param approvedUsersOnly
	 * @return
	 */
	static public IRateLimiter MakeIPHostileCheckingIRateLimiter(int maxAttempts, int maxSeconds, boolean rateLimitByIP, boolean rateLimitByUser, boolean rateLimitByEndpoint, boolean approvedUsersOnly) {
		IDataStore dataStore = new DataStore();
		return NewTestRateLimiter(dataStore, maxAttempts, maxSeconds, true, rateLimitByIP, rateLimitByUser, rateLimitByEndpoint, approvedUsersOnly);
	}
	
	/***
	 * Provides selection to all constructor variables of the RateLimiter aside
	 * from those that are used to enforce only allowing authorized users
	 * @param maxAttempts
	 * @param maxSeconds
	 * @param storeHostileIPs
	 * @param rateLimitByIP
	 * @param rateLimitByUser
	 * @param rateLimitByEndpoint
	 * @return
	 */
	static public IRateLimiter MakeUserAuthorizationNotCheckedIRateLimiter(int maxAttempts, int maxSeconds, boolean storeHostileIPs, boolean rateLimitByIP, boolean rateLimitByUser, boolean rateLimitByEndpoint) {
		IDataStore dataStore = new DataStore();
		return NewTestRateLimiter(dataStore, maxAttempts, maxSeconds, storeHostileIPs, rateLimitByIP, rateLimitByUser, rateLimitByEndpoint, false);
	}

	/***
	 * Provides selection to all constructor variables of the RateLimiter aside
	 * from those that are used to enforce checking for hostile IPs
	 * @param maxAttempts
	 * @param maxSeconds
	 * @param rateLimitByIP
	 * @param rateLimitByUser
	 * @param rateLimitByEndpoint
	 * @param approvedUsersOnly
	 * @return
	 */
	static public IRateLimiter MakeIPHostileNotCheckedIRateLimiter(int maxAttempts, int maxSeconds, boolean rateLimitByIP, boolean rateLimitByUser, boolean rateLimitByEndpoint, boolean approvedUsersOnly) {
		IDataStore dataStore = new DataStore();
		return NewTestRateLimiter(dataStore, maxAttempts, maxSeconds, false, rateLimitByIP, rateLimitByUser, rateLimitByEndpoint, approvedUsersOnly);
	}

	/* The following "MakeAll<<behavior>:?><Limiting:?>IRateLimiters" methods 
	 * provide an abstracted way to produce all instances of the RateLimiter 
	 * which conform to the expected behavior
	 */
	
	/***
	 * Returns a collection of IRateLimiters produced by calling 
	 * the most generic NewTestRateLimiter(...)
	 * with every possible combination of inputs for the 2 boolean flags that
	 * are used to define all other operations besides those pertinent to the
	 * function being called.
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	static public IRateLimiter[] MakeAllIRateLimiters(int maxAttempts, int maxSeconds) {
		final int howManyBooleans = 5;
		IRateLimiter[] iRateLimiters = new IRateLimiter[twoToThePowerOf(howManyBooleans)];
		for(int k = 0; k < twoToThePowerOf(howManyBooleans); k++) {
			IDataStore dataStore = new DataStore();
			iRateLimiters[k] = NewTestRateLimiter(dataStore,maxAttempts,maxSeconds,iteratorFlagSet(k,0),iteratorFlagSet(k,1),iteratorFlagSet(k,2),iteratorFlagSet(k,3),iteratorFlagSet(k,4));
		}
		return iRateLimiters;
	}
	
	/***
	 * Returns a collection of IRateLimiters produced by calling 
	 * MakeIPLimitingIRateLimiter
	 * with every possible combination of inputs for the 2 boolean flags that
	 * are used to define all other operations besides those pertinent to the
	 * function being called.
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	static public IRateLimiter[] MakeAllIPLimitingIRateLimiters(int maxAttempts, int maxSeconds) {
		final int howManyBooleans = 2;
		IRateLimiter[] iRateLimiters = new IRateLimiter[twoToThePowerOf(howManyBooleans)];
		for(int k = 0; k < twoToThePowerOf(howManyBooleans); k++) {
			iRateLimiters[k] = MakeIPLimitingIRateLimiter(maxAttempts,maxSeconds,iteratorFlagSet(k,0),iteratorFlagSet(k,1));
		}
		return iRateLimiters;
	}

	/***
	 * Returns a collection of IRateLimiters produced by calling 
	 * MakeUserLimitingIRateLimiter
	 * with every possible combination of inputs for the 2 boolean flags that
	 * are used to define all other operations besides those pertinent to the
	 * function being called.
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	static public IRateLimiter[] MakeAllUserLimitingIRateLimiters(int maxAttempts, int maxSeconds) {
		final int howManyBooleans = 2;
		IRateLimiter[] iRateLimiters = new IRateLimiter[twoToThePowerOf(howManyBooleans)];
		for(int k = 0; k < twoToThePowerOf(howManyBooleans); k++) {
			iRateLimiters[k] = MakeUserLimitingIRateLimiter(maxAttempts,maxSeconds,iteratorFlagSet(k,0),iteratorFlagSet(k,1));
		}
		return iRateLimiters;
	}

	/***
	 * Returns a collection of IRateLimiters produced by calling 
	 * MakeEndpointByIPLimitingIRateLimiter
	 * with every possible combination of inputs for the 2 boolean flags that
	 * are used to define all other operations besides those pertinent to the
	 * function being called.
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	static public IRateLimiter[] MakeAllEndpointByIPLimitingIRateLimiters(int maxAttempts, int maxSeconds) {
		final int howManyBooleans = 2;
		IRateLimiter[] iRateLimiters = new IRateLimiter[twoToThePowerOf(howManyBooleans)];
		for(int k = 0; k < twoToThePowerOf(howManyBooleans); k++) {
			iRateLimiters[k] = MakeEndpointByIPLimitingIRateLimiter(maxAttempts,maxSeconds,iteratorFlagSet(k,0),iteratorFlagSet(k,1));
		}
		return iRateLimiters;
	}

	/***
	 * Returns a collection of IRateLimiters produced by calling 
	 * MakeEndpointByUserLimitingIRateLimiter
	 * with every possible combination of inputs for the 2 boolean flags that
	 * are used to define all other operations besides those pertinent to the
	 * function being called.
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	static public IRateLimiter[] MakeAllEndpointByUserLimitingIRateLimiters(int maxAttempts, int maxSeconds) {
		final int howManyBooleans = 2;
		IRateLimiter[] iRateLimiters = new IRateLimiter[twoToThePowerOf(howManyBooleans)];
		for(int k = 0; k < twoToThePowerOf(howManyBooleans); k++) {
			iRateLimiters[k] = MakeEndpointByUserLimitingIRateLimiter(maxAttempts,maxSeconds,iteratorFlagSet(k,0),iteratorFlagSet(k,1));
		}
		return iRateLimiters;
	}
	
	/***
	 * Returns a collection of IRateLimiters produced by calling 
	 * MakeUserAuthorizationCheckingIRateLimiter
	 * with every possible combination of inputs for the 4 boolean flags that
	 * are used to define all other operations besides those pertinent to the
	 * function being called.
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	static public IRateLimiter[] MakeAllUserAuthorizationCheckingIRateLimiters(int maxAttempts, int maxSeconds) {
		final int howManyBooleans = 4;
		IRateLimiter[] iRateLimiters = new IRateLimiter[twoToThePowerOf(howManyBooleans)];
		for(int k = 0; k < twoToThePowerOf(howManyBooleans); k++) {
			iRateLimiters[k] = MakeUserAuthorizationCheckingIRateLimiter(maxAttempts,maxSeconds,iteratorFlagSet(k,0),iteratorFlagSet(k,1),iteratorFlagSet(k,2),iteratorFlagSet(k,3));
		}
		return iRateLimiters;
	}
	
	/***
	 * Returns a collection of IRateLimiters produced by calling 
	 * MakeIPHostileCheckingIRateLimiter
	 * with every possible combination of inputs for the 4 boolean flags that
	 * are used to define all other operations besides those pertinent to the
	 * function being called.
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	static public IRateLimiter[] MakeAllIPHostileCheckingIRateLimiters(int maxAttempts, int maxSeconds) {
		final int howManyBooleans = 4;
		IRateLimiter[] iRateLimiters = new IRateLimiter[twoToThePowerOf(howManyBooleans)];
		for(int k = 0; k < twoToThePowerOf(howManyBooleans); k++) {
			iRateLimiters[k] = MakeIPHostileCheckingIRateLimiter(maxAttempts,maxSeconds,iteratorFlagSet(k,0),iteratorFlagSet(k,1),iteratorFlagSet(k,2),iteratorFlagSet(k,3));
		}
		return iRateLimiters;
	}
	
	/***
	 * Returns a collection of IRateLimiters produced by calling 
	 * MakeUserAuthorizationNotCheckedIRateLimiter
	 * with every possible combination of inputs for the 4 boolean flags that
	 * are used to define all other operations besides those pertinent to the
	 * function being called.
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	static public IRateLimiter[] MakeAllUserAuthorizationNotCheckedIRateLimiters(int maxAttempts, int maxSeconds) {
		final int howManyBooleans = 4;
		IRateLimiter[] iRateLimiters = new IRateLimiter[twoToThePowerOf(howManyBooleans)];
		for(int k = 0; k < twoToThePowerOf(howManyBooleans); k++) {
			iRateLimiters[k] = MakeUserAuthorizationNotCheckedIRateLimiter(maxAttempts,maxSeconds,iteratorFlagSet(k,0),iteratorFlagSet(k,1),iteratorFlagSet(k,2),iteratorFlagSet(k,3));
		}
		return iRateLimiters;
	}
	
	/***
	 * Returns a collection of IRateLimiters produced by calling 
	 * MakeIPHostileNotCheckedIRateLimiter
	 * with every possible combination of inputs for the 4 boolean flags that
	 * are used to define all other operations besides those pertinent to the
	 * function being called.
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	static public IRateLimiter[] MakeAllIPHostileNotCheckedIRateLimiters(int maxAttempts, int maxSeconds) {
		final int howManyBooleans = 4;
		IRateLimiter[] iRateLimiters = new IRateLimiter[twoToThePowerOf(howManyBooleans)];
		for(int k = 0; k < twoToThePowerOf(howManyBooleans); k++) {
			iRateLimiters[k] = MakeIPHostileNotCheckedIRateLimiter(maxAttempts,maxSeconds,iteratorFlagSet(k,0),iteratorFlagSet(k,1),iteratorFlagSet(k,2),iteratorFlagSet(k,3));
		}
		return iRateLimiters;
	}
	
	/* The following "MakeAll<<behavior>:?><Limiting:?>IRateLimiters" overloads 
	 * provide an abstracted way to produce all instances of the RateLimiter 
	 * which conform to the expected behavior, and conform to the static test
	 * variables defining the request limits and time limits
	 */
	
	/***
	 * Produces the standard collection of 
	 * MakeAllIRateLimiters(int,int)
	 * with the arguments provided by the standard values defined in the test
	 * @return
	 */
	static public IRateLimiter[] MakeAllIRateLimiters() {
		return MakeAllIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test);
	}
	
	/***
	 * Produces the standard collection of 
	 * MakeAllIPLimitingIRateLimiters(int,int)
	 * with the arguments provided by the standard values defined in the test
	 * @return
	 */
	static public IRateLimiter[] MakeAllIPLimitingIRateLimiters() {
		return MakeAllIPLimitingIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test);
	}
	
	/***
	 * Produces the standard collection of 
	 * MakeAllUserLimitingIRateLimiters(int,int)
	 * with the arguments provided by the standard values defined in the test
	 * @return
	 */
	static public IRateLimiter[] MakeAllUserLimitingIRateLimiters() {
		return MakeAllUserLimitingIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test);
	}
	
	/***
	 * Produces the standard collection of 
	 * MakeAllEndpointByIPLimitingIRateLimiters(int,int)
	 * with the arguments provided by the standard values defined in the test
	 * @return
	 */
	static public IRateLimiter[] MakeAllEndpointByIPLimitingIRateLimiters() {
		return MakeAllEndpointByIPLimitingIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test);
	}
	
	/***
	 * Produces the standard collection of 
	 * MakeAllEndpointByUserLimitingIRateLimiters(int,int)
	 * with the arguments provided by the standard values defined in the test
	 * @return
	 */
	static public IRateLimiter[] MakeAllEndpointByUserLimitingIRateLimiters() {
		return MakeAllEndpointByUserLimitingIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test);
	}
	
	/***
	 * Produces the standard collection of 
	 * MakeAllUserAuthorizationCheckingIRateLimiters(int,int)
	 * with the arguments provided by the standard values defined in the test
	 * @return
	 */
	static public IRateLimiter[] MakeAllUserAuthorizationCheckingIRateLimiters() {
		return MakeAllUserAuthorizationCheckingIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test);
	}
	
	/***
	 * Produces the standard collection of 
	 * MakeAllIPHostileCheckingIRateLimiters(int,int)
	 * with the arguments provided by the standard values defined in the test
	 * @return
	 */
	static public IRateLimiter[] MakeAllIPHostileCheckingIRateLimiters() {
		return MakeAllIPHostileCheckingIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test);
	}
	
	/***
	 * Produces the standard collection of 
	 * MakeAllUserAuthorizationNotCheckedIRateLimiters(int,int)
	 * with the arguments provided by the standard values defined in the test
	 * @return
	 */
	static public IRateLimiter[] MakeAllUserAuthorizationNotCheckedIRateLimiters() {
		return MakeAllUserAuthorizationNotCheckedIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test);
	}
	
	/***
	 * Produces the standard collection of 
	 * MakeAllIPHostileNotCheckedIRateLimiters(int,int)
	 * with the arguments provided by the standard values defined in the test
	 * @return
	 */
	static public IRateLimiter[] MakeAllIPHostileNotCheckedIRateLimiters() {
		return MakeAllIPHostileNotCheckedIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test);
	}
	
	/* Auxiliary helper functions to assist the
	 * "MakeAll<<behavior>:?><Limiting:?>IRateLimiters"(int,int)
	 * methods in looping over a range of ints, used as flags
	 */
	
	/***
	 * Raises 2 to the power of an exponent
	 * @param exponent
	 * @return
	 */
	static public int twoToThePowerOf(int exponent) {
		return ((int) Math.pow(2, exponent));
	}
	
	/***
	 * Checks for the bitwise flags being on in an iterator value
	 * @param iterator
	 * @param flag
	 * @return
	 */
	static public boolean iteratorFlagSet(int iterator, int flag) {
		return ((iterator & twoToThePowerOf(flag)) != 0);
	}
	
}
