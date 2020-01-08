package DataStoreTest;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import DataStore.IDataStore;
import DataStore.IDataStore.RateLimitedIdentity;
import DataStore.IDataStore.RateLimitingMap;
import RateLimiterService.DataStore;

/***
 * Test the DataStore implementation of the IDataStore interface;
 * Only test the overridden functions
 *
 */
public class DataStoreTest extends IDataStoreTestBase {
	
	/***
	 * @return An instance of the DataStore implementation of the IDataStore,
	 * returned as a reference to an IDataStore implementation so as to limit
	 * testing to the surface of the DataStore exposed by the IDataStore
	 */
	public IDataStore NewTestDataStore() {
		return new DataStore();
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
		IDataStore ds = NewTestDataStore();
		// Test with the identity not initialized in the IDataStore
		assertFalse(ds.RecordNewAttempt(identity, 0, anHour));
		// Initialized the identity in the IDataStore
		assertTrue(ds.RecordNewAttempt(identity, 1, anHour));
		// Test with the identity initialized in the IDataStore
		assertFalse(ds.RecordNewAttempt(identity, 0, anHour));
	}
	
	private void AssertCanStoreOnlyTheProvidedNumberOfAttempts(
												RateLimitedIdentity identity,
												int HowManyAttempts) {
		System.out.println("Begin test for RecordNewAttempt; "+
				   "AssertCanStoreOnlyTheProvidedNumberOfAttempts; "+
				   identity.GetRateLimitedIdentityType().toString());
		IDataStore ds = NewTestDataStore();
		// Can record "HowManyAttempts" attempts
		for(int k = 0; k < HowManyAttempts; k++) {
			assertTrue(ds.RecordNewAttempt(identity, HowManyAttempts, anHour));
		}
		// Can't record any more than that!
		assertFalse(ds.RecordNewAttempt(identity, HowManyAttempts, anHour));
		// But we can record another one if we increase the amount!
		assertTrue(ds.RecordNewAttempt(identity, HowManyAttempts+1, anHour));
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
		//there will only be 1 attempt actually held in the RateLimitingMap.
		IDataStore ds = NewTestDataStore();
		//Firstly, assert we can add 1 more than the amount of allowed attempts
		for(int k = 0; k <= HowManyAttempts; k++) {
			assertTrue(ds.RecordNewAttempt(identity, HowManyAttempts, 0));
		}
		//Now, we expect there to be only 1 recorded attempt held in memory..
		//Assert that we can't add a new attempt if we are only allowing the 1
		//that is currently already stored.
		assertFalse(ds.RecordNewAttempt(identity, 1, anHour));
		//Assert that we CAN add a new attempt if we are allowing 2!
		assertTrue(ds.RecordNewAttempt(identity, 2, anHour));
	}
	
	@Test
	void CheckWhenNextRequestAllowedTest() {
		
	}
	
	@Test
	void UserAuthFunctionsTest() {
		IDataStore ds = NewTestDataStore();
		assertFalse(ds.IsUserAuthValid(testUser));
		ds.StoreUserAuth(testUser);
		assertTrue(ds.IsUserAuthValid(testUser));
		ds.ForgetUserAuth(testUser);
		assertFalse(ds.IsUserAuthValid(testUser));
	}
	
	@Test
	void HostileIPFunctionsTest() {
		IDataStore ds = NewTestDataStore();
		assertFalse(ds.containsHostileIP(testIP));
		ds.recordHostileIP(testIP);
		assertTrue(ds.containsHostileIP(testIP));
		ds.removeHostileIP(testIP);
		assertFalse(ds.containsHostileIP(testIP));
	}
	
}
