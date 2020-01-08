package RateLimiterService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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
