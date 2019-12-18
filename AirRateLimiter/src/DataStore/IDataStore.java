package DataStore;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import DataStore.IDataStore.RateLimitedIdentity;
import DataStore.IDataStore.RateLimitingMap;
import DataStore.IDataStore.RateLimitedIdentity.RateLimitedIdentityType;

/***
 * Mock implementation of a data store, housed in application memory. This
 * interface follows an instance injection approach, where, for the purposes
 * of mocking a data store in application memory, the same instance of this
 * interfaces' actualisation should be injected into every component that 
 * requires some capacity to "query the data store" such that they all share
 * the same data store.
 */
public interface IDataStore {
	
	/*
	 * Define the standard model of a "rate limited identity"
	 */
	
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
		
		RateLimitedIdentity(String identity, String endpoint, 
							RateLimitedIdentityType rateLimitedIdentityType){
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
			return (this.rateLimitedIdentityType == 
											 RateLimitedIdentityType.Endpoint);
		}
		
	};
	
	/* STATIC METHODS TO GET NEW RateLimitedIdentity INSTANCES
	 * Functions that will generate new RateLimitedIdentity as per the
	 * required enum to reference the identity's type.
	 */
	
	/***
	 * Define a method to return an inner instance that encapsulates 
	 * the "Rate limited identity" when that identity is an IP
	 * @param identity
	 * @param endpoint
	 * @return
	 */
	public static RateLimitedIdentity NewRateLimitedIP(String IP) {
		return new RateLimitedIdentity(IP,null,RateLimitedIdentityType.IP);
	}
	
	/***
	 * Define a method to return an inner instance that encapsulates 
	 * the "Rate limited identity" when that identity is a User
	 * @param identity
	 * @param endpoint
	 * @return
	 */
	public static RateLimitedIdentity NewRateLimitedUser(String User) {
		return new RateLimitedIdentity(User,null,RateLimitedIdentityType.User);
	}
	
	/***
	 * Define a method to return an inner instance that encapsulates the 
	 * "Rate limited identity" when that identity is an End-point Identity
	 * @param identity
	 * @param endpoint
	 * @return
	 */
	public static RateLimitedIdentity NewRateLimitedEndpoint(String identity,
															 String endpoint) {
		return new RateLimitedIdentity(identity,endpoint,
				   					   RateLimitedIdentityType.Endpoint);
	}
	
	/***
	 * Define a class to abstract the occurrence of thread-safe maps which 
	 * point to instances of thread-safe queues. Abstracted on type.
	 * @param <K>
	 * @param <V>
	 */
	public class QueueMap<K,V>{
		
		private final ConcurrentHashMap<K,ConcurrentLinkedQueue<V>> map;
		
		QueueMap(){
			map = new ConcurrentHashMap<K,ConcurrentLinkedQueue<V>>();
		}
		
		/***
		 * Replaces Map's "containsKey"
		 * @param key
		 * @return
		 */
		public boolean MapsFromKey(K key) {
			return map.containsKey(key);
		}
		
		/***
		 * Replace Map's "Get"
		 * @param key
		 * @return
		 */
		public ConcurrentLinkedQueue<V> GetQueue(K key) {
			return map.get(key);
		}
		
		/***
		 * Replaces Map's "Put"
		 * @param key
		 * @param queue
		 */
		public void PutQueue(K key, ConcurrentLinkedQueue<V> queue) {
			map.put(key, queue);
		}
		
		/***
		 * Peek the tip value of the queue for a given key
		 * @param key
		 * @return
		 */
		public V PeekQueueTip(K key) {
			return map.get(key).peek();
		}
		
		/***
		 * Poll the tip value of the queue for a given key
		 * @param key
		 * @return
		 */
		public V PollQueueTip(K key) {
			return map.get(key).poll();
		}
		
		/***
		 * Checks if the queue mapped to by some key already contains a value.
		 * @param key
		 * @param value
		 * @return
		 */
		public boolean QueueContainsNonDiscreteValue(K key, V value) {
			return map.get(key).contains(value);
		}
		
		/***
		 * Add a new item to the queue for a given key
		 * @param key
		 * @param item
		 */
		public void AddToQueue(K key, V item) {
			map.get(key).add(item);
		}
		
	}
	
	/***
	 * Define a class to abstract the instantiation of a thread-safe mapping
	 * from Strings to a thread-safe queue of LocalDateTime objects
	 *
	 */
	public class RateLimitingMap{
		
		private final QueueMap<String,LocalDateTime> queueMap;
		public static final int deduplicationThresholdPerMilliSecond = 1000;
		
		RateLimitingMap(){
			queueMap = new QueueMap<String,LocalDateTime>();
		}
		
		/***
		 * Check if the given String maps to an existing queue
		 * @param key
		 * @return
		 */
		public boolean MapsFromKey(String key) {
			return queueMap.MapsFromKey(key);
		}
		
		/***
		 * Get the queue for the given key
		 * @param key
		 * @return
		 */
		public ConcurrentLinkedQueue<LocalDateTime> GetQueue(String key) {
			return queueMap.GetQueue(key);
		}
		
		/***
		 * Put a new given queue against a given key
		 * @param key
		 * @param queue
		 */
		public void PutQueue(String key, 
							 ConcurrentLinkedQueue<LocalDateTime> queue) {
			queueMap.PutQueue(key, queue);
		}
		
		/***
		 * Peek the tip value of the queue for a given key
		 * @param key
		 * @return
		 */
		public LocalDateTime PeekQueueTip(String key) {
			return queueMap.PeekQueueTip(key);
		}
		
		/***
		 * Poll the tip value of the queue for a given key
		 * @param key
		 * @return
		 */
		public LocalDateTime PollQueueTip(String key) {
			return queueMap.PollQueueTip(key);
		}
		
		/***
		 * Add a new item to the queue for a given key
		 * @param key
		 * @param localDateTime
		 */
		private void AddToQueue(String key, LocalDateTime localDateTime) {
			queueMap.AddToQueue(key,localDateTime);
		}
		
		/***
		 * Makes a new queue with now as the first (tip) entry of the queue
		 * @param key
		 * @return
		 */
		public LocalDateTime MakeNewQueueWithNowAtTip(String key) {
			LocalDateTime now = LocalDateTime.now();
			ConcurrentLinkedQueue<LocalDateTime> queue = new 
										ConcurrentLinkedQueue<LocalDateTime>();
			queue.add(now);
			this.PutQueue(key, queue);
			return now;
		}
		
		/***
		 * Attempts to deduplicate entries in the queue up to a threshold
		 * amount of possible deduplications per millisecond (per JAVA 8 millis
		 * being the maximum precision of LocalDateTime.now()). If the milli
		 * second counter ticks up while trying to deduplicate, return that!
		 * If fails to deduplicate, will return null as no entry could be made!
		 * 
		 * @param key
		 * @param now
		 * @return
		 */
		private LocalDateTime DeduplicateValue(String key, LocalDateTime now) {
			int thresholdCount = 0;
			while(queueMap.QueueContainsNonDiscreteValue(key, now)) {
				now = now.plusNanos(1);
				if(thresholdCount < deduplicationThresholdPerMilliSecond) {
					thresholdCount++;
				} else if(LocalDateTime.now().isAfter(now)){
					return LocalDateTime.now();
				} else {
					return null;
				}
			}
			return now;
		}
		
		/***
		 * Add the current de-duplicated time to the mapped queue
		 * @param key
		 * @return
		 */
		public LocalDateTime AddCurrentTimeToExistingQueue(String key) {
			LocalDateTime now = DeduplicateValue(key,LocalDateTime.now());
			this.AddToQueue(key, now);
			return now;
		}
		
	}
	
	
	/* STATIC METHOD TO GET NEW RateLimitingMap INSTANCE
	 * Function to return a new "QueueMap" instance targeted at the storing
	 * and retrieval of LocalDateTime per String keys
	 */
	
	/***
	 * Return a new "RateLimitingMap" instance targeted at the storing
	 * and retrieval of LocalDateTime per String keys
	 * @return
	 */
	public static RateLimitingMap NewRateLimitingMap() {
		return new RateLimitingMap();
	};

	/*
	 * Functions that take a RateLimitedIdentity to record a new attempt
	 * or check when the next request by that identity will be allowed
	 */
	
	/***
	 * Query the data store for availability to record a 
	 * new attempt from a rateLimitedIdentity, if it is
	 * available to create a new record, record this attempt
	 * @param rateLimitedIdentity
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	public boolean RecordNewAttempt(RateLimitedIdentity rateLimitedIdentity,
									int maxAttempts, 
									int maxSeconds);
	
	/***
	 * Query the data store to check when the next available request 
	 * by an identity will be allowed. If the next attempt is allowed now, 
	 * will return the current time
	 * @param rateLimitedIdentity
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	public LocalDateTime CheckWhenNextRequestAllowed(
									RateLimitedIdentity rateLimitedIdentity,
									int maxAttempts, 
									int maxSeconds);
	
	/*
	 * Provides functionality to store and check against stored user 
	 * authorization strings.
	 */
	
	/***
	 * Store a user Authorization string in a list of known users
	 * @param UserAuth
	 */
	public void StoreUserAuth(String UserAuth);
	
	/***
	 * Forget a user Authorization string from a list of known users
	 * @param UserAuth
	 */
	public void ForgetUserAuth(String UserAuth);
	
	/***
	 * Query the data store whether it contains a given user's Authorization
	 * @param UserAuth
	 * @return
	 */
	public boolean IsUserAuthValid(String UserAuth);
	
	/*
	 * other functionality: Check hostile IP
	 */
	
	/***
	 * Query the data store for hostile IP
	 * @param IP
	 * @return
	 */
	public boolean containsHostileIP(String IP);
	
	/***
	 * Records a new hostile IP
	 * @param IP
	 */
	public void recordHostileIP(String IP);
	
	/***
	 * Removes a tracked hostile IP
	 * @param IP
	 */
	public void removeHostileIP(String IP);
	
}
