package RateLimiterService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/***
 * Define a class to abstract the occurrence of thread-safe maps which point to
 * instances of thread-safe queues. Abstracted on type. While this class can be
 * instantiated, its intention is to be subclassed, but is not made explicitly
 * abstract as its purpose is to encapsulate the handling of the inner map and
 * queue instances.
 * @param <K>
 * @param <V>
 */
public class QueueMap<K,V>{
	
	/***
	 * The inner map from type K to a queue of elements of type V.
	 */
	private final ConcurrentHashMap<K,ConcurrentLinkedQueue<V>> map;
	
	/***
	 * Instantiates the map of type K to queues of elements of type V.
	 */
	QueueMap(){
		map = new ConcurrentHashMap<K,ConcurrentLinkedQueue<V>>();
	}
	
	/***
	 * Indicates if a key maps to an existing queue.
	 * @param key
	 * @return True, if the key K maps to a queue.
	 */
	public boolean MapsFromKey(K key) {
		return map.containsKey(key);
	}
	
	/***
	 * Gets a queue mapped to from a key
	 * @param key
	 * @return A queue of elements of type V
	 */
	protected ConcurrentLinkedQueue<V> GetQueue(K key) {
		return map.get(key);
	}
	
	/***
	 * Add a new queue to the map for a given key
	 * @param key
	 */
	public void InitialiseQueue(K key) {
		ConcurrentLinkedQueue<V> queue = new ConcurrentLinkedQueue<V>();
		PutQueue(key, queue);
	}
	
	/***
	 * Places the existing queue in the map.
	 * @param key
	 * @param Queue
	 */
	public void PutQueue(K key, ConcurrentLinkedQueue<V> queue) {
		map.put(key, queue);
	}
	
	/***
	 * Peek the tip value of the queue for a given key
	 * @param key
	 * @return The top element V in the queue mapped to by the key
	 */
	public V PeekQueueTip(K key) {
		return GetQueue(key).peek();
	}
	
	/***
	 * Poll the tip value of the queue for a given key
	 * @param key
	 * @return The top element V in the queue mapped to by the key.
	 */
	public V PollQueueTip(K key) {
		return GetQueue(key).poll();
	}
	
	/***
	 * Checks if the queue mapped to by some key already contains a value.
	 * @param key
	 * @param value
	 * @return True, if the value exists in the queue mapped to by the key.
	 */
	public boolean QueueContainsNonDiscreteValue(K key, V value) {
		return GetQueue(key).contains(value);
	}
	
	/***
	 * Add a new item to the queue for a given key
	 * @param key
	 * @param item
	 * @return The item that was added to the mapped queue.
	 */
	public V AddToQueue(K key, V item) {
		GetQueue(key).add(item);
		return item;
	}
	
	/***
	 * Add a new queue to the map for a given key, starting with a given entry
	 * @param key
	 * @param item
	 */
	public V InitialiseQueueWith(K key, V item) {
		InitialiseQueue(key);
		AddToQueue(key,item);
		return item;
	}
}
