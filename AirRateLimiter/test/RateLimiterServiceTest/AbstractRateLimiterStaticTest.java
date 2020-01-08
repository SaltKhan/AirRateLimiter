package RateLimiterServiceTest;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import RateLimiterService.RateLimitedIdentity;
import RateLimiterService.RateLimitedIdentity.RateLimitedIdentityType;
import RateLimiterService.RateLimitingMap;

import java.time.LocalDateTime;

/***
 * Test the static methods of the IDataStore, independent of an implementation
 *
 */
public class AbstractRateLimiterStaticTest extends AbstractRateLimiterTestBase{
	
	@Test
	void NewRateLimitedIPTest() {
		RateLimitedIdentity IPIdentity = NewTestRateLimitedIP(testIP);
		assertTrue(IPIdentity.GetRateLimitedIdentityType() == RateLimitedIdentityType.IP);
		assertTrue(IPIdentity.GetIdentity() == testIP);
		assertTrue(IPIdentity.GetEndpoint() == null);
		assertFalse(IPIdentity.IsIdentityAnEndpointAttempt());
	}
	
	@Test
	void NewRateLimitedUserTest() {
		RateLimitedIdentity UserIdentity = NewTestRateLimitedUser(testUser);
		assertTrue(UserIdentity.GetRateLimitedIdentityType() == 
								 RateLimitedIdentityType.User);
		assertTrue(UserIdentity.GetIdentity() == testUser);
		assertTrue(UserIdentity.GetEndpoint() == null);
		assertFalse(UserIdentity.IsIdentityAnEndpointAttempt());
	}
	
	@Test
	void NewRateLimitedEndpointTest() {
		RateLimitedIdentity EndpointIdentity = 
						 NewTestRateLimitedEndpoint(testIdentity,testEndpoint);
		assertTrue(EndpointIdentity.GetRateLimitedIdentityType() == 
						 		 RateLimitedIdentityType.Endpoint);
		assertTrue(EndpointIdentity.GetIdentity() == testIdentity);
		assertTrue(EndpointIdentity.GetEndpoint() == testEndpoint);
		assertTrue(EndpointIdentity.IsIdentityAnEndpointAttempt());
	}
	
	@Test
	void RateLimitingMapTest_MakingANewQueueReturnsTheTip() {
		RateLimitingMap rlMap = NewTestRateLimitingMap();
		LocalDateTime fromMaking = rlMap.MakeNewQueueWithNowAtTip(key);
		assertTrue(fromMaking != null);
	}
	
	@Test
	void RateLimitingMapTest_MappingToAQueue() {
		RateLimitingMap rlMap = NewTestRateLimitingMap();
		assertFalse(rlMap.MapsFromKey(key));
		rlMap.MakeNewQueueWithNowAtTip(key);
		assertTrue(rlMap.MapsFromKey(key));
	}
	
	@Test
	void RateLimitingMapTest_PeekingGivesTheTip() {
		RateLimitingMap rlMap = NewTestRateLimitingMap();
		LocalDateTime fromMaking = rlMap.MakeNewQueueWithNowAtTip(key);
		LocalDateTime fromPeeking = rlMap.PeekQueueTip(key);
		assertTrue(fromMaking.isEqual(fromPeeking));
	}
	
	@Test
	void RateLimitingMapTest_AddingAddsAndPollingRemovesAndGivesTheTip() {
		RateLimitingMap rlMap = NewTestRateLimitingMap();
		LocalDateTime fromMaking = rlMap.MakeNewQueueWithNowAtTip(key);
		//Now try adding and polling
		LocalDateTime fromAdding = rlMap.AddCurrentTimeToExistingQueue(key);
		// The first tip is from being made
		LocalDateTime fromPolling = rlMap.PollQueueTip(key);
		assertTrue(fromPolling.isEqual(fromMaking));
		// The second tip is from adding
		fromPolling = rlMap.PollQueueTip(key);
		assertTrue(fromPolling.isEqual(fromAdding));
		// The third tip doesn't exist
		fromPolling = rlMap.PollQueueTip(key);
		assertTrue(fromPolling == null);
	}
	
	@Test
	void RateLimitingMapTest_GettingAndPuttingTheQueue() {
		RateLimitingMap rlMap = NewTestRateLimitingMap();
		rlMap.MakeNewQueueWithNowAtTip(key);
		assertTrue(rlMap.GetQueue(key) != null);
		assertTrue(rlMap.GetQueue(anotherKey) == null);
		rlMap.PutQueue(anotherKey, rlMap.GetQueue(key));
		assertTrue(rlMap.GetQueue(key).equals(rlMap.GetQueue(anotherKey)));
	}

}
