package RateLimiterService;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;


/***
 * Define a class to abstract the instantiation of a thread-safe mapping
 * from Strings to a thread-safe queue of LocalDateTime objects
 *
 */
public class RateLimitingMap {
	
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