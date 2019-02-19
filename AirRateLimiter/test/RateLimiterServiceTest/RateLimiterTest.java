package RateLimiterServiceTest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import DataStore.DataStore;
import DataStore.IDataStore;
import DataStore.IDataStore.RateLimitedIdentity;
import RateLimiterService.IRateLimiter;
import RateLimiterService.RateLimiter;

/***
 * Test the RateLimiter implementation of the IRateLimiter interface;
 * Only test the overridden functions (at this time, there are no statics)
 */
class RateLimiterTest extends IRateLimiterTestBase {
	
	/*** Tests the
	 * int GetRequestLimitHits();
	 */
	@Test
	void GetRequestLimitHitsTest() {
		IRateLimiter[] allRateLimiters = MakeAllIRateLimiters();
		for(IRateLimiter iRateLimiter : allRateLimiters) {
			assertTrue(iRateLimiter.GetRequestLimitHits() == RequestLimitHits_Test);
		}
		allRateLimiters = MakeAllIRateLimiters(IRateLimiter.RequestLimitHits_Standard,IRateLimiter.TimeLimitSeconds_Standard);
		for(IRateLimiter iRateLimiter : allRateLimiters) {
			assertTrue(iRateLimiter.GetRequestLimitHits() == IRateLimiter.RequestLimitHits_Standard);
		}
	}

	/*** Tests the
	 * int GetTimeLimitSeconds();
	 */
	@Test
	void GetTimeLimitSecondsTest() {
		IRateLimiter[] allRateLimiters = MakeAllIRateLimiters();
		for(IRateLimiter iRateLimiter : allRateLimiters) {
			assertTrue(iRateLimiter.GetTimeLimitSeconds() == TimeLimitSeconds_Test);
		}
		allRateLimiters = MakeAllIRateLimiters(IRateLimiter.RequestLimitHits_Standard,IRateLimiter.TimeLimitSeconds_Standard);
		for(IRateLimiter iRateLimiter : allRateLimiters) {
			assertTrue(iRateLimiter.GetTimeLimitSeconds() == IRateLimiter.TimeLimitSeconds_Standard);
		}
	}

	/*** Tests the
	 * IDataStore GetIRateLimitersIDataStoreInstance();
	 */
	@Test
	void GetIRateLimitersIDataStoreInstanceTest() {
		IRateLimiter[] allRateLimiters = MakeAllIRateLimiters();
		for(IRateLimiter iRateLimiter : allRateLimiters) {
			assertTrue(iRateLimiter.GetIRateLimitersIDataStoreInstance() != null);
		}
	}

	/*** Tests the
	 * RateLimitedIdentity GetRateLimitedIdentityFromRateLimiterContext(String clientIP,String UserAuth,String endpoint);
	 */
	@Test
	void GetRateLimitedIdentityFromRateLimiterContextTest() {
		IRateLimiter[] allRateLimiters = MakeAllIPLimitingIRateLimiters();
		for(IRateLimiter iRateLimiter : allRateLimiters) {
			assertTrue(NaiveFullIdentity(iRateLimiter) == RateLimitedIdentity.RateLimitedIdentityType.IP);
			assertTrue(NaivePartialIdentity(iRateLimiter) == RateLimitedIdentity.RateLimitedIdentityType.IP);
		}
		allRateLimiters = MakeAllUserLimitingIRateLimiters();
		for(IRateLimiter iRateLimiter : allRateLimiters) {
			assertTrue(NaiveFullIdentity(iRateLimiter) == RateLimitedIdentity.RateLimitedIdentityType.User);
			assertTrue(NaivePartialIdentity(iRateLimiter) == null);
		}
		allRateLimiters = MakeAllEndpointByIPLimitingIRateLimiters();
		for(IRateLimiter iRateLimiter : allRateLimiters) {
			assertTrue(NaiveFullIdentity(iRateLimiter) == RateLimitedIdentity.RateLimitedIdentityType.Endpoint);
			assertTrue(NaivePartialIdentity(iRateLimiter) == RateLimitedIdentity.RateLimitedIdentityType.Endpoint);
		}
		allRateLimiters = MakeAllEndpointByUserLimitingIRateLimiters();
		for(IRateLimiter iRateLimiter : allRateLimiters) {
			assertTrue(NaiveFullIdentity(iRateLimiter) == RateLimitedIdentity.RateLimitedIdentityType.Endpoint);
			assertTrue(NaivePartialIdentity(iRateLimiter) == null);
		}
	}

	/*** Tests the
	 * String IsAttemptRateLimited(RateLimitedIdentity rateLimitedIdentity);
	 * @throws InterruptedException 
	 */
	@Test
	void IsAttemptRateLimitedTest() throws InterruptedException {
		HashMap<RateLimitedIdentity,IRateLimiter[]> allCases = GetAllAttemptRateCasesByIdentityType();
		for(RateLimitedIdentity identity : allCases.keySet()) {
			IsAttemptRateLimitedOuter(allCases.get(identity),identity);
		}
	}

	/*** Tests the
	 * void ServeHttp429PerAttempt(PrintWriter printWriter,RateLimitedIdentity rateLimitedIdentity);
	 */
	@Test
	void ServeHttp429PerAttemptTest() {
		//TODO
	}

	/*** Tests the
	 * void StoreNewHttpAuthorization(String UserAuth);
	 * void ForgetExistingHttpAuthorization(String UserAuth);
	 */
	@Test
	void StoreAndForgetNewHttpAuthorizationTest() {
		IRateLimiter[] allRateLimitersThatCheckUserAuthorization = MakeAllUserAuthorizationCheckingIRateLimiters();
		//TODO
		IRateLimiter[] allRateLimitersThatDontCheckUserAuthorization = MakeAllUserAuthorizationNotCheckedIRateLimiters();
		//TODO
	}

	/*** Tests the
	 * void StoreNewHttpBasicAuthorization(String username,String password);
	 * void ForgetExistingHttpBasicAuthorization(String username,String password);
	 */
	@Test
	void StoreAndForgetNewHttpBasicAuthorizationTest() {
		IRateLimiter[] allRateLimitersThatCheckUserAuthorization = MakeAllUserAuthorizationCheckingIRateLimiters();
		//TODO
		IRateLimiter[] allRateLimitersThatDontCheckUserAuthorization = MakeAllUserAuthorizationNotCheckedIRateLimiters();
		//TODO
	}

	/*** Tests the
	 * String ServeHttp40XPerUserAuth(PrintWriter printWriter, String UserAuth);
	 */
	@Test
	void ServeHttp40XPerUserAuthTest() {
		//TODO
	}

	/*** Tests the
	 * boolean IsIPHostile(Socket clientSocket);
	 * void RecordIPAsHostile(Socket clientSocket);
	 * void ForgetIPAsHostile(Socket clientSocket);
	 */
	@Test
	void RecordCheckAndForgetIPAsHostileTest() {
		IRateLimiter[] allRateLimitersThatCheckHostileIP = MakeAllIPHostileCheckingIRateLimiters();
		//TODO
		IRateLimiter[] allRateLimitersThatDontCheckHostileIP = MakeAllIPHostileNotCheckedIRateLimiters();
		//TODO
	}

	/*** Tests the
	 * String FormEndpointStringFromVerbAndResource(String httpVerb,String resource);
	 */
	@Test
	void FormEndpointStringFromVerbAndResourceTest() {
		//TODO
	}
	
	@Test
	void TestHelperTest() {
		assertTrue(twoToThePowerOf(0) == 1);
		assertTrue(twoToThePowerOf(1) == 2);
		assertTrue(twoToThePowerOf(2) == 4);
		assertTrue(twoToThePowerOf(3) == 8);
		
		for(int k = 0; k < 8; k++) {
			int zeroFlag = k%2;
			assertTrue(iteratorFlagSet(k,0) == (zeroFlag != 0));
			int oneFlag  = (k-zeroFlag)%4;
			assertTrue(iteratorFlagSet(k,1) == (oneFlag != 0));
			int twoFlag  = (k-zeroFlag-oneFlag)%8;
			assertTrue(iteratorFlagSet(k,2) == (twoFlag != 0));
			int threeFlag  = (k-zeroFlag-oneFlag-twoFlag)%16;
			assertTrue(iteratorFlagSet(k,3) == (threeFlag != 0));
			System.out.println(" ");
		}
	}
	
	private RateLimitedIdentity.RateLimitedIdentityType NaiveFullIdentity(IRateLimiter iRateLimiter){
		return iRateLimiter.GetRateLimitedIdentityFromRateLimiterContext("clientIP", "UserAuth", "endpoint").GetRateLimitedIdentityType();
	}
	
	private RateLimitedIdentity.RateLimitedIdentityType NaivePartialIdentity(IRateLimiter iRateLimiter){
		RateLimitedIdentity identity = iRateLimiter.GetRateLimitedIdentityFromRateLimiterContext("clientIP", "", "endpoint");
		if(identity == null) {
			return null;
		} else {
			return identity.GetRateLimitedIdentityType();
		}
	}
	
	private HashMap<RateLimitedIdentity,IRateLimiter[]> GetAllAttemptRateCasesByIdentityType() {
		HashMap<RateLimitedIdentity,IRateLimiter[]> allCases = new HashMap<RateLimitedIdentity,IRateLimiter[]>();
		allCases.put(IDataStore.NewRateLimitedIP(IP_Test), MakeAllIPLimitingIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test));
		allCases.put(IDataStore.NewRateLimitedUser(UserAuth_Test), MakeAllUserLimitingIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test));
		allCases.put(IDataStore.NewRateLimitedEndpoint(IP_Test, (verb_Test+"|"+resource_Test)), MakeAllEndpointByIPLimitingIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test));
		allCases.put(IDataStore.NewRateLimitedEndpoint(UserAuth_Test, (verb_Test+"|"+resource_Test)), MakeAllEndpointByUserLimitingIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test));
		return allCases;
	}
	
	private void IsAttemptRateLimitedOuter(IRateLimiter[] iRateLimiters, RateLimitedIdentity identity) throws InterruptedException {
		Thread[] threads = new Thread[2*iRateLimiters.length];
		int iter = 0;
		for(IRateLimiter iRateLimiter : iRateLimiters) {
			threads[iter] = new Thread(() -> {
				try {
					IsAttemptRateLimitedInner(iRateLimiter,identity);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
			threads[iter].start();
			iter++;
			threads[iter] = new Thread(() -> {
				try {
					IsAttemptRateLimitedInner(iRateLimiter,iRateLimiter.GetRateLimitedIdentityFromRateLimiterContext(IP_Test_2,UserAuth_Test_2,(verb_Test+"|"+resource_Test)));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
			threads[iter].start();
			iter++;
		}
		for(Thread thread : threads) {
			thread.join();
		}
	}
	
	private void IsAttemptRateLimitedInner(IRateLimiter iRateLimiter, RateLimitedIdentity identity) throws InterruptedException {
		IDataStore dataStore = iRateLimiter.GetIRateLimitersIDataStoreInstance();
		boolean[] results = new boolean[RequestLimitHits_Test+1];
		//Hit is "Attempts + 1" times
		for(int k = 0; k <= RequestLimitHits_Test; k++) {
			results[k] = (iRateLimiter.IsAttemptRateLimited(identity) != "");
		}
		//Check with the dataStore that its "next available request" time is after right now.
		LocalDateTime nextService = dataStore.CheckWhenNextRequestAllowed(identity,RequestLimitHits_Test,TimeLimitSeconds_Test);
		assertTrue(nextService.isAfter(LocalDateTime.now()));
		//Check that the first "Attempts" many attempts weren't rejected
		for(int k = 0; k < RequestLimitHits_Test; k++) {
			assertFalse(results[k]);
		}
		//Check that the last attempt was rejected
		assertTrue(results[RequestLimitHits_Test]);
		//Wait for ~ the time limit
		Thread.sleep(TimeLimitSeconds_Test*1010);
		//Repeat the test!
		for(int k = 0; k <= RequestLimitHits_Test; k++) {
			results[k] = (iRateLimiter.IsAttemptRateLimited(identity) != "");
		}
		nextService = dataStore.CheckWhenNextRequestAllowed(identity,RequestLimitHits_Test,TimeLimitSeconds_Test);
		assertTrue(nextService.isAfter(LocalDateTime.now()));
		for(int k = 0; k < RequestLimitHits_Test; k++) {
			assertFalse(results[k]);
		}
		assertTrue(results[RequestLimitHits_Test]);
	}
	
}
