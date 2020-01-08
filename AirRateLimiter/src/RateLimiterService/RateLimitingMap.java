package RateLimiterService;

import java.time.LocalDateTime;

/***
 * A class to abstract the instantiation of a thread-safe mapping
 * from Strings to a thread-safe queue of LocalDateTime objects.
 * This is currently intended to be sufficient in implementing a
 * "Fixed Window Counter" methodology.
 */
public class RateLimitingMap extends QueueMap<String,LocalDateTime> {
	
	public static final int deduplicationThresholdPerMilliSecond = 1000;
	
	/***
	 * Create a new instance of the map from type 
	 * String to queues of type LocalDateTime
	 */
	RateLimitingMap(){
		super();
	}
	
	/***
	 * Makes a new queue with now as the first (tip) entry of the queue
	 * @param key
	 * @return
	 */
	public LocalDateTime MakeNewQueueWithNowAtTip(String key) {
		return InitialiseQueueWith(key,LocalDateTime.now());
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
		while(this.QueueContainsNonDiscreteValue(key, now)) {
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