package RateLimiterServiceTest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import RateLimiterService.AbstractRateLimiter;
import RateLimiterService.RateLimitedIdentity;
import RateLimiterService.RateLimitedIdentity.RateLimitedIdentityType;
import RateLimiterService.RateLimiter;
import RateLimiterService.RateLimitingBehaviour;

/***
 * Test the RateLimiter implementation of the AbstractRateLimiter interface;
 * Only test the overridden functions (at this time, there are no statics)
 */
class RateLimiterTest extends AbstractRateLimiterTestBase {
	
	/*** Tests the
	 * int GetRequestLimitHits();
	 */
	@Test
	void GetRequestLimitHitsTest() {
		AbstractRateLimiter[] allRateLimiters = MakeAllIRateLimiters();
		for(AbstractRateLimiter abstractRateLimiter : allRateLimiters) {
			assertTrue(abstractRateLimiter.requestLimitHits() == RequestLimitHits_Test);
		}
		allRateLimiters = MakeAllIRateLimiters(RateLimitingBehaviour.RequestLimitHits_Standard,RateLimitingBehaviour.TimeLimitSeconds_Standard);
		for(AbstractRateLimiter abstractRateLimiter : allRateLimiters) {
			assertTrue(abstractRateLimiter.requestLimitHits() == RateLimitingBehaviour.RequestLimitHits_Standard);
		}
	}

	/*** Tests the
	 * int GetTimeLimitSeconds();
	 */
	@Test
	void GetTimeLimitSecondsTest() {
		AbstractRateLimiter[] allRateLimiters = MakeAllIRateLimiters();
		for(AbstractRateLimiter abstractRateLimiter : allRateLimiters) {
			assertTrue(abstractRateLimiter.timeLimitSeconds() == TimeLimitSeconds_Test);
		}
		allRateLimiters = MakeAllIRateLimiters(RateLimitingBehaviour.RequestLimitHits_Standard,RateLimitingBehaviour.TimeLimitSeconds_Standard);
		for(AbstractRateLimiter abstractRateLimiter : allRateLimiters) {
			assertTrue(abstractRateLimiter.timeLimitSeconds() == RateLimitingBehaviour.TimeLimitSeconds_Standard);
		}
	}

	/*** Tests the
	 * IDataStore GetIRateLimitersIDataStoreInstance();
	 */
	@Test
	void GetIRateLimitersIDataStoreInstanceTest() {
		AbstractRateLimiter[] allRateLimiters = MakeAllIRateLimiters();
		for(AbstractRateLimiter abstractRateLimiter : allRateLimiters) {
			assertTrue(abstractRateLimiter != null);
		}
	}

	/*** Tests the
	 * RateLimitedIdentity GetRateLimitedIdentityFromRateLimiterContext(String clientIP,String UserAuth,String endpoint);
	 */
	@Test
	void GetRateLimitedIdentityFromRateLimiterContextTest() {
		AbstractRateLimiter[] allRateLimiters = MakeAllIPLimitingIRateLimiters();
		for(AbstractRateLimiter abstractRateLimiter : allRateLimiters) {
			assertTrue(NaiveFullIdentity(abstractRateLimiter) == RateLimitedIdentityType.IP);
			assertTrue(NaivePartialIdentity(abstractRateLimiter) == RateLimitedIdentityType.IP);
		}
		allRateLimiters = MakeAllUserLimitingIRateLimiters();
		for(AbstractRateLimiter abstractRateLimiter : allRateLimiters) {
			assertTrue(NaiveFullIdentity(abstractRateLimiter) == RateLimitedIdentityType.User);
			assertTrue(NaivePartialIdentity(abstractRateLimiter) == null);
		}
		allRateLimiters = MakeAllEndpointByIPLimitingIRateLimiters();
		for(AbstractRateLimiter abstractRateLimiter : allRateLimiters) {
			assertTrue(NaiveFullIdentity(abstractRateLimiter) == RateLimitedIdentityType.Endpoint);
			assertTrue(NaivePartialIdentity(abstractRateLimiter) == RateLimitedIdentityType.Endpoint);
		}
		allRateLimiters = MakeAllEndpointByUserLimitingIRateLimiters();
		for(AbstractRateLimiter abstractRateLimiter : allRateLimiters) {
			assertTrue(NaiveFullIdentity(abstractRateLimiter) == RateLimitedIdentityType.Endpoint);
			assertTrue(NaivePartialIdentity(abstractRateLimiter) == null);
		}
	}

	/*** Tests the
	 * String IsAttemptRateLimited(RateLimitedIdentity rateLimitedIdentity);
	 * @throws InterruptedException 
	 */
	@Test
	void IsAttemptRateLimitedTest() throws InterruptedException {
		HashMap<RateLimitedIdentity,AbstractRateLimiter[]> allCases = GetAllAttemptRateCasesByIdentityType();
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
		AbstractRateLimiter[] allRateLimitersThatCheckUserAuthorization = MakeAllUserAuthorizationCheckingIRateLimiters();
		//TODO
		AbstractRateLimiter[] allRateLimitersThatDontCheckUserAuthorization = MakeAllUserAuthorizationNotCheckedIRateLimiters();
		//TODO
	}

	/*** Tests the
	 * void StoreNewHttpBasicAuthorization(String username,String password);
	 * void ForgetExistingHttpBasicAuthorization(String username,String password);
	 */
	@Test
	void StoreAndForgetNewHttpBasicAuthorizationTest() {
		AbstractRateLimiter[] allRateLimitersThatCheckUserAuthorization = MakeAllUserAuthorizationCheckingIRateLimiters();
		//TODO
		AbstractRateLimiter[] allRateLimitersThatDontCheckUserAuthorization = MakeAllUserAuthorizationNotCheckedIRateLimiters();
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
		AbstractRateLimiter[] allRateLimitersThatCheckHostileIP = MakeAllIPHostileCheckingIRateLimiters();
		//TODO
		AbstractRateLimiter[] allRateLimitersThatDontCheckHostileIP = MakeAllIPHostileNotCheckedIRateLimiters();
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
	
	private RateLimitedIdentityType NaiveFullIdentity(AbstractRateLimiter abstractRateLimiter){
		return abstractRateLimiter.getRateLimitedIdentityFromRateLimiterContext("clientIP", "UserAuth", "endpoint").GetRateLimitedIdentityType();
	}
	
	private RateLimitedIdentityType NaivePartialIdentity(AbstractRateLimiter abstractRateLimiter){
		RateLimitedIdentity identity = abstractRateLimiter.getRateLimitedIdentityFromRateLimiterContext("clientIP", "", "endpoint");
		if(identity == null) {
			return null;
		} else {
			return identity.GetRateLimitedIdentityType();
		}
	}
	
	private HashMap<RateLimitedIdentity,AbstractRateLimiter[]> GetAllAttemptRateCasesByIdentityType() {
		HashMap<RateLimitedIdentity,AbstractRateLimiter[]> allCases = new HashMap<RateLimitedIdentity,AbstractRateLimiter[]>();
		allCases.put(AbstractRateLimiter.NewRateLimitedIP(IP_Test), MakeAllIPLimitingIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test));
		allCases.put(AbstractRateLimiter.NewRateLimitedUser(UserAuth_Test), MakeAllUserLimitingIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test));
		allCases.put(AbstractRateLimiter.NewRateLimitedEndpoint(IP_Test, (verb_Test+"|"+resource_Test)), MakeAllEndpointByIPLimitingIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test));
		allCases.put(AbstractRateLimiter.NewRateLimitedEndpoint(UserAuth_Test, (verb_Test+"|"+resource_Test)), MakeAllEndpointByUserLimitingIRateLimiters(RequestLimitHits_Test,TimeLimitSeconds_Test));
		return allCases;
	}
	
	private void IsAttemptRateLimitedOuter(AbstractRateLimiter[] iRateLimiters, RateLimitedIdentity identity) throws InterruptedException {
		Thread[] threads = new Thread[2*iRateLimiters.length];
		int iter = 0;
		for(AbstractRateLimiter abstractRateLimiter : iRateLimiters) {
			threads[iter] = new Thread(() -> {
				try {
					IsAttemptRateLimitedInner(abstractRateLimiter,identity);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
			threads[iter].start();
			iter++;
			threads[iter] = new Thread(() -> {
				try {
					IsAttemptRateLimitedInner(abstractRateLimiter,abstractRateLimiter.getRateLimitedIdentityFromRateLimiterContext(IP_Test_2,UserAuth_Test_2,(verb_Test+"|"+resource_Test)));
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
	
	private void IsAttemptRateLimitedInner(AbstractRateLimiter abstractRateLimiter, RateLimitedIdentity identity) throws InterruptedException {
		boolean[] results = new boolean[RequestLimitHits_Test+1];
		//Hit is "Attempts + 1" times
		for(int k = 0; k <= RequestLimitHits_Test; k++) {
			results[k] = (abstractRateLimiter.IsAttemptRateLimited(identity) != "");
		}
		//Check with the dataStore that its "next available request" time is after right now.
		LocalDateTime nextService = abstractRateLimiter.CheckWhenNextRequestAllowed(identity,RequestLimitHits_Test,TimeLimitSeconds_Test);
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
			results[k] = (abstractRateLimiter.IsAttemptRateLimited(identity) != "");
		}
		nextService = abstractRateLimiter.CheckWhenNextRequestAllowed(identity,RequestLimitHits_Test,TimeLimitSeconds_Test);
		assertTrue(nextService.isAfter(LocalDateTime.now()));
		for(int k = 0; k < RequestLimitHits_Test; k++) {
			assertFalse(results[k]);
		}
		assertTrue(results[RequestLimitHits_Test]);
	}
	
	/***
	 * @return An instance of the DataStore implementation of the IDataStore,
	 * returned as a reference to an IDataStore implementation so as to limit
	 * testing to the surface of the DataStore exposed by the IDataStore
	 */
	public AbstractRateLimiter NewTestAbstractRateLimiter() {
		return new RateLimiter();
	}
	
	@Test
	void RecordNewAttemptTest_IP() {
		RateLimitedIdentity identity = NewTestRateLimitedIP();
		RecordNewAttemptTest_Generic(identity);
		
	}
	
	@Test
	void RecordNewAttemptTest_User() {
		RateLimitedIdentity identity = NewTestRateLimitedUser();
		RecordNewAttemptTest_Generic(identity);
	}
	
	@Test
	void RecordNewAttemptTest_Endpoint() {
		RateLimitedIdentity identity = NewTestRateLimitedEndpoint();
		RecordNewAttemptTest_Generic(identity);
	}
	
	private void RecordNewAttemptTest_Generic(RateLimitedIdentity identity) {
		AssertCantRecordAttemptWhenAllowingZeroAttempts(identity);
		AssertCanStoreOnlyTheProvidedNumberOfAttempts(identity,manyAttempts);
		AssertOldAttemptsGetClearedWhenTheyExpire(identity,manyAttempts);
	}
	
	private void AssertCantRecordAttemptWhenAllowingZeroAttempts(
												RateLimitedIdentity identity) {
		System.out.println("Begin test for RecordNewAttempt; "+
						   "AssertCantRecordAttemptWhenAllowingZeroAttempts; "+
						   identity.GetRateLimitedIdentityType().toString());
		AbstractRateLimiter arl = NewTestAbstractRateLimiter();
		// Test with the identity not initialized in the IDataStore
		assertFalse(arl.RecordNewAttempt(identity, 0, anHour));
		// Initialized the identity in the IDataStore
		assertTrue(arl.RecordNewAttempt(identity, 1, anHour));
		// Test with the identity initialized in the IDataStore
		assertFalse(arl.RecordNewAttempt(identity, 0, anHour));
	}
	
	private void AssertCanStoreOnlyTheProvidedNumberOfAttempts(
												RateLimitedIdentity identity,
												int HowManyAttempts) {
		System.out.println("Begin test for RecordNewAttempt; "+
				   "AssertCanStoreOnlyTheProvidedNumberOfAttempts; "+
				   identity.GetRateLimitedIdentityType().toString());
		AbstractRateLimiter arl = NewTestAbstractRateLimiter();
		// Can record "HowManyAttempts" attempts
		for(int k = 0; k < HowManyAttempts; k++) {
			assertTrue(arl.RecordNewAttempt(identity, HowManyAttempts, anHour));
		}
		// Can't record any more than that!
		assertFalse(arl.RecordNewAttempt(identity, HowManyAttempts, anHour));
		// But we can record another one if we increase the amount!
		assertTrue(arl.RecordNewAttempt(identity, HowManyAttempts+1, anHour));
	}
	
	private void AssertOldAttemptsGetClearedWhenTheyExpire(
												RateLimitedIdentity identity,
												int HowManyAttempts) {
		System.out.println("Begin test for RecordNewAttempt; "+
				   "AssertOldAttemptsGetClearedWhenTheyExpire; "+
				   identity.GetRateLimitedIdentityType().toString());
		//Rather than test this with hacky sleeps or stop watch implementations
		//this will test the expected functionality that recording of a new 
		//attempt will clear all attempts older than necessary to keep, 
		//which means that after adding an arbitrary amount with zero timeout
		//there will only be 1 attempt actually held in the FixedWindowRateLimitingMap.
		AbstractRateLimiter arl = NewTestAbstractRateLimiter();
		//Firstly, assert we can add 1 more than the amount of allowed attempts
		for(int k = 0; k <= HowManyAttempts; k++) {
			assertTrue(arl.RecordNewAttempt(identity, HowManyAttempts, 0));
		}
		//Now, we expect there to be only 1 recorded attempt held in memory..
		//Assert that we can't add a new attempt if we are only allowing the 1
		//that is currently already stored.
		assertFalse(arl.RecordNewAttempt(identity, 1, anHour));
		//Assert that we CAN add a new attempt if we are allowing 2!
		assertTrue(arl.RecordNewAttempt(identity, 2, anHour));
	}
	
	@Test
	void CheckWhenNextRequestAllowedTest() {
		
	}
	
	@Test
	void UserAuthFunctionsTest() {
		AbstractRateLimiter arl = NewTestAbstractRateLimiter();
		assertFalse(arl.IsUserAuthValid(testUser));
		arl.StoreUserAuth(testUser);
		assertTrue(arl.IsUserAuthValid(testUser));
		arl.ForgetUserAuth(testUser);
		assertFalse(arl.IsUserAuthValid(testUser));
	}
	
	@Test
	void HostileIPFunctionsTest() {
		AbstractRateLimiter arl = NewTestAbstractRateLimiter();
		assertFalse(arl.containsHostileIP(testIP));
		arl.recordHostileIP(testIP);
		assertTrue(arl.containsHostileIP(testIP));
		arl.removeHostileIP(testIP);
		assertFalse(arl.containsHostileIP(testIP));
	}
	
}
