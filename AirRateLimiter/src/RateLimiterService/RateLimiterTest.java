package RateLimiterService;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import DataStore.DataStore;
import DataStore.IDataStore;

class RateLimiterTest {
	
	@Test
	void TestIsIPRateLimited() throws InterruptedException {
		//Try 5 attempts every 2 seconds
		int maxAttempts = 5;
		int maxSeconds = 2;
		IDataStore dataStore = new DataStore();
		IRateLimiter rateLimiter = new RateLimiter(dataStore, maxAttempts, maxSeconds, false, true, false, false, false, false);
		//Clearly google's dns is interested in this!
		String IP = "8.8.8.8";
		boolean[] results = new boolean[maxAttempts+1];
		//Hit is "Attempts + 1" times
		for(int k = 0; k <= maxAttempts; k++) {
			results[k] = rateLimiter.IsIPRateLimited(IP);
		}
		//Check with the dataStore that its "next available request" time is after right now.
		LocalDateTime nextService = dataStore.CheckWhenNextRequestAllowedByIP(IP,maxAttempts,maxSeconds);
		assertTrue(nextService.isAfter(LocalDateTime.now()));
		//Check that the first "Attempts" many attempts weren't rejected
		for(int k = 0; k < maxAttempts; k++) {
			assertFalse(results[k]);
		}
		//Check that the last attempt was rejected
		assertTrue(results[maxAttempts]);
		//Wait for ~ the time limit
		Thread.sleep(maxSeconds*1010);
		//Repeat the test!
		for(int k = 0; k <= maxAttempts; k++) {
			results[k] = rateLimiter.IsIPRateLimited(IP);
		}
		nextService = dataStore.CheckWhenNextRequestAllowedByIP(IP,maxAttempts,maxSeconds);
		assertTrue(nextService.isAfter(LocalDateTime.now()));
		for(int k = 0; k < maxAttempts; k++) {
			assertFalse(results[k]);
		}
		assertTrue(results[maxAttempts]);
	}

	@Test
	void TestIsUserRateLimited() throws InterruptedException {
		//Try 5 attempts every 2 seconds
		int maxAttempts = 5;
		int maxSeconds = 2;
		IDataStore dataStore = new DataStore();
		IRateLimiter rateLimiter = new RateLimiter(dataStore, maxAttempts, maxSeconds, false, false, true, false, false, false);
		String UserAuth = "Basic " + new String(Base64.getEncoder().encode(("Bob:1234").getBytes()));
		boolean[] results = new boolean[maxAttempts+1];
		//Hit is "Attempts + 1" times
		for(int k = 0; k <= maxAttempts; k++) {
			results[k] = rateLimiter.IsUserRateLimited(UserAuth);
		}
		//Check with the dataStore that its "next available request" time is after right now.
		LocalDateTime nextService = dataStore.CheckWhenNextRequestAllowedByUser(UserAuth,maxAttempts,maxSeconds);
		assertTrue(nextService.isAfter(LocalDateTime.now()));
		//Check that the first "Attempts" many attempts weren't rejected
		for(int k = 0; k < maxAttempts; k++) {
			assertFalse(results[k]);
		}
		//Check that the last attempt was rejected
		assertTrue(results[maxAttempts]);
		//Wait for ~ the time limit
		Thread.sleep(maxSeconds*1010);
		//Repeat the test!
		for(int k = 0; k <= maxAttempts; k++) {
			results[k] = rateLimiter.IsUserRateLimited(UserAuth);
		}
		nextService = dataStore.CheckWhenNextRequestAllowedByUser(UserAuth,maxAttempts,maxSeconds);
		assertTrue(nextService.isAfter(LocalDateTime.now()));
		for(int k = 0; k < maxAttempts; k++) {
			assertFalse(results[k]);
		}
		assertTrue(results[maxAttempts]);
	}
	
	@Test
	void TestIsIPRateLimitedToEndpoint() throws InterruptedException {
		//Try 5 attempts every 2 seconds
		int maxAttempts = 5;
		int maxSeconds = 2;
		IDataStore dataStore = new DataStore();
		IRateLimiter rateLimiter = new RateLimiter(dataStore, maxAttempts, maxSeconds, false, false, false, true, false, false);
		String IP = "8.8.8.8";
		String UserAuth = "Basic " + new String(Base64.getEncoder().encode(("Bob:1234").getBytes()));
		String verb = "GET";
		String resource = "gg/m8";
		boolean[] results = new boolean[maxAttempts+1];
		//Hit is "Attempts + 1" times
		for(int k = 0; k <= maxAttempts; k++) {
			results[k] = rateLimiter.IsIdentityRateLimitedToEndPoint(IP,UserAuth,verb,resource);
		}
		//Check with the dataStore that its "next available request" time is after right now.
		LocalDateTime nextService = dataStore.CheckWhenNextEndpointRequestAllowedByIdentity(UserAuth,(verb+"|"+resource),maxAttempts,maxSeconds);
		assertFalse(nextService.isAfter(LocalDateTime.now()));
		nextService = dataStore.CheckWhenNextEndpointRequestAllowedByIdentity(IP,(verb+"|"+resource),maxAttempts,maxSeconds);
		assertTrue(nextService.isAfter(LocalDateTime.now()));
		//Check that the first "Attempts" many attempts weren't rejected
		for(int k = 0; k < maxAttempts; k++) {
			assertFalse(results[k]);
		}
		//Check that the last attempt was rejected
		assertTrue(results[maxAttempts]);
		//Wait for ~ the time limit
		Thread.sleep(maxSeconds*1010);
		//Repeat the test!
		for(int k = 0; k <= maxAttempts; k++) {
			results[k] = rateLimiter.IsIdentityRateLimitedToEndPoint(IP,UserAuth,verb,resource);;
		}
		nextService = dataStore.CheckWhenNextEndpointRequestAllowedByIdentity(UserAuth,(verb+"|"+resource),maxAttempts,maxSeconds);
		assertFalse(nextService.isAfter(LocalDateTime.now()));
		nextService = dataStore.CheckWhenNextEndpointRequestAllowedByIdentity(IP,(verb+"|"+resource),maxAttempts,maxSeconds);
		assertTrue(nextService.isAfter(LocalDateTime.now()));
		for(int k = 0; k < maxAttempts; k++) {
			assertFalse(results[k]);
		}
		assertTrue(results[maxAttempts]);
	}
	
	@Test
	void TestIsUserRateLimitedToEndpoint() throws InterruptedException {
		//Try 5 attempts every 2 seconds
		int maxAttempts = 5;
		int maxSeconds = 2;
		IDataStore dataStore = new DataStore();
		IRateLimiter rateLimiter = new RateLimiter(dataStore, maxAttempts, maxSeconds, false, false, true, true, false, false);
		String IP = "8.8.8.8";
		String UserAuth = "Basic " + new String(Base64.getEncoder().encode(("Bob:1234").getBytes()));
		String verb = "GET";
		String resource = "gg/m8";
		boolean[] results = new boolean[maxAttempts+1];
		//Hit is "Attempts + 1" times
		for(int k = 0; k <= maxAttempts; k++) {
			results[k] = rateLimiter.IsIdentityRateLimitedToEndPoint(IP,UserAuth,verb,resource);
		}
		//Check with the dataStore that its "next available request" time is after right now.
		LocalDateTime nextService = dataStore.CheckWhenNextEndpointRequestAllowedByIdentity(IP,(verb+"|"+resource),maxAttempts,maxSeconds);
		assertFalse(nextService.isAfter(LocalDateTime.now()));
		nextService = dataStore.CheckWhenNextEndpointRequestAllowedByIdentity(UserAuth,(verb+"|"+resource),maxAttempts,maxSeconds);
		assertTrue(nextService.isAfter(LocalDateTime.now()));
		//Check that the first "Attempts" many attempts weren't rejected
		for(int k = 0; k < maxAttempts; k++) {
			assertFalse(results[k]);
		}
		//Check that the last attempt was rejected
		assertTrue(results[maxAttempts]);
		//Wait for ~ the time limit
		Thread.sleep(maxSeconds*1010);
		//Repeat the test!
		for(int k = 0; k <= maxAttempts; k++) {
			results[k] = rateLimiter.IsIdentityRateLimitedToEndPoint(IP,UserAuth,verb,resource);;
		}
		nextService = dataStore.CheckWhenNextEndpointRequestAllowedByIdentity(IP,(verb+"|"+resource),maxAttempts,maxSeconds);
		assertFalse(nextService.isAfter(LocalDateTime.now()));
		nextService = dataStore.CheckWhenNextEndpointRequestAllowedByIdentity(UserAuth,(verb+"|"+resource),maxAttempts,maxSeconds);
		assertTrue(nextService.isAfter(LocalDateTime.now()));
		for(int k = 0; k < maxAttempts; k++) {
			assertFalse(results[k]);
		}
		assertTrue(results[maxAttempts]);
	}
	
}
