package RateLimiterService;

import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/***
 * Implements the expectations of the AbstractRateLimiter
 */
public class RateLimiter extends AbstractRateLimiter {

	final private RateLimitingBehaviour rateLimitingBehaviour;
	
	/*
	 * Aside from these "data storing" objects, ideally the data store 
	 * shouldn't host any other data members. It's blind to them.
	 * For this reason the methods here from the IDataStore are obtusely
	 * named to do all checks in one!
	 */
	
	private final ArrayList<String> hostileIPs;
	private final ArrayList<String> ValidUserAuths;
	private final RateLimitingMap IPAttempts;
	private final RateLimitingMap UserAttempts;
	private final ConcurrentHashMap<String,RateLimitingMap> EndpointAttempts;
	
	
	/*
	 * Constructors
	 */
	
	/***
	 * The most generic constructor. Assign everything
	 * @param GetDataStoreInstance()
	 * @param RequestLimitHits
	 * @param TimeLimitSeconds
	 * @param storeHostileIPs
	 * @param rateLimitByIP
	 * @param rateLimitByUser
	 * @param rateLimitByEndpoint
	 * @param approvedUsersOnly
	 */
	public RateLimiter(int RequestLimitHits, 
					   int TimeLimitSeconds, 
					   boolean storeHostileIPs, 
					   boolean rateLimitByIP, 
					   boolean rateLimitByUser, 
					   boolean rateLimitByEndpoint, 
					   boolean approvedUsersOnly) {
		this.rateLimitingBehaviour = new RateLimitingBehaviour(RequestLimitHits,
				TimeLimitSeconds,
				storeHostileIPs,
				rateLimitByIP,
				rateLimitByUser,
				rateLimitByEndpoint,
				approvedUsersOnly);
		this.hostileIPs = new ArrayList<String>();
		this.ValidUserAuths = new ArrayList<String>();
		this.IPAttempts = NewRateLimitingMap();
		this.UserAttempts = NewRateLimitingMap();
		this.EndpointAttempts = new ConcurrentHashMap<String,RateLimitingMap>();
	}
	
	/***
	 * Constructor generic to everything other 
	 * than predefined rate at which to limit
	 * @param GetDataStoreInstance()
	 * @param storeHostileIPs
	 * @param rateLimitByIP
	 * @param rateLimitByUser
	 * @param rateLimitByEndpoint
	 * @param approvedUsersOnly
	 * @param demandsUserAuth
	 */
	public RateLimiter(boolean storeHostileIPs, 
					   boolean rateLimitByIP, 
					   boolean rateLimitByUser, 
					   boolean rateLimitByEndpoint, 
					   boolean approvedUsersOnly, 
					   boolean demandsUserAuth) {
		this.rateLimitingBehaviour = new RateLimitingBehaviour(storeHostileIPs,
				rateLimitByIP,
				rateLimitByUser,
				rateLimitByEndpoint,
				approvedUsersOnly);
		this.hostileIPs = new ArrayList<String>();
		this.ValidUserAuths = new ArrayList<String>();
		this.IPAttempts = NewRateLimitingMap();
		this.UserAttempts = NewRateLimitingMap();
		this.EndpointAttempts = new ConcurrentHashMap<String,RateLimitingMap>();
	}
	
	/***
	 * Make a rate limiter which limits on End-points per User identities,
	 * but allow generic assignment of the approvedUsers and 
	 * the metrics by which the rate limiter operates.
	 * @param GetDataStoreInstance()
	 * @param RequestLimitHits
	 * @param TimeLimitSeconds
	 * @param approvedUsersOnly
	 */
	public RateLimiter(int RequestLimitHits, int TimeLimitSeconds, boolean approvedUsersOnly) {
		this.rateLimitingBehaviour = new RateLimitingBehaviour(RequestLimitHits,TimeLimitSeconds,approvedUsersOnly);
		this.hostileIPs = new ArrayList<String>();
		this.ValidUserAuths = new ArrayList<String>();
		this.IPAttempts = NewRateLimitingMap();
		this.UserAttempts = NewRateLimitingMap();
		this.EndpointAttempts = new ConcurrentHashMap<String,RateLimitingMap>();
	}
	
	/***
	 * Least generic constructor. Makes a RateLimiter with the standard
	 * rate at which to limit, which limits on End-points per User Identities
	 * Must assign an IDataStore.
	 * @param GetDataStoreInstance()
	 */
	public RateLimiter() {
		this.rateLimitingBehaviour = new RateLimitingBehaviour();
		this.hostileIPs = new ArrayList<String>();
		this.ValidUserAuths = new ArrayList<String>();
		this.IPAttempts = NewRateLimitingMap();
		this.UserAttempts = NewRateLimitingMap();
		this.EndpointAttempts = new ConcurrentHashMap<String,RateLimitingMap>();
	}
	
	/*
	 * Getter overrides
	 */
	
	@Override
	public RateLimitingBehaviour GetRateLimitingBehaviour() {
		return this.rateLimitingBehaviour;
	}
	
	/*
	 * Incorporating the previous DataStore
	 */
	

	/*
	 * We cannot supply getters for the maps/lists as this will open them to 
	 * having elements inserted outside of the design of this class
	 */
	
	/* Overrides
	 * Functions that take a RateLimitedIdentity to record a new attempt
	 * or check when the next request by that identity will be allowed
	 */
	
	@Override
	public boolean RecordNewAttempt(RateLimitedIdentity RLIdentity, 
									int maxAttempts, 
									int maxSeconds) {
		//Handle the special case when dealing with an End-point
		//As end-points map identities to the regular attempt map types
		if((RLIdentity.IsIdentityAnEndpointAttempt())) {
			if(!EndpointAttempts.containsKey(RLIdentity.GetIdentity())) {
				//If we don't contain the identity we must make it's entry!
				if(maxAttempts > 0) {
					return MakeEndpointMapWithNewQueue(RLIdentity, 
												   EndpointAttempts);
				} else {
					return GetRecordAttemptMessage(RLIdentity,null);
				}
			}
		}
		//Now we've addressed the end-point special case, treat all generic.
		return RecoredNewAttemptInner(RLIdentity,maxAttempts,maxSeconds);
	}
	
	@Override
	public LocalDateTime CheckWhenNextRequestAllowed(
												RateLimitedIdentity RLIdentity,
												int maxAttempts, 
												int maxSeconds) {
		RateLimitingMap lookupMap = GetAttemptMapForIdentity(RLIdentity);
		if(lookupMap == null) {
			return LocalDateTime.now();
		} else {
			String lookupKey = GetAttemptKeyForIdentity(RLIdentity);
			if(lookupMap.MapsFromKey(lookupKey)) {
				if(IdentityHasTooManyAttempts(lookupMap, 
											  lookupKey, 
											  maxAttempts)) {
					return lookupMap.PeekQueueTip(lookupKey)
									.plusSeconds(maxSeconds);
				} else {
					return LocalDateTime.now();
				}
			} else {
				return LocalDateTime.now();
			}
		}
	}
	
	/* Overrides
	 * Provides functionality to store and check against stored user 
	 * authorization strings.
	 */

	@Override
	public void StoreUserAuth(String UserAuth) {
		AddToArrayList(ValidUserAuths,UserAuth);
	}
	
	@Override
	public void ForgetUserAuth(String UserAuth) {
		RemoveFromArrayList(ValidUserAuths,UserAuth);
	}

	@Override
	public boolean IsUserAuthValid(String UserAuth) {
		return DoesArrayListContainValue(ValidUserAuths,UserAuth);
	}
	
	/* Overrides
	 * other functionality: Check hostile IP
	 */
	
	@Override
	public boolean containsHostileIP(String IP) {
		return DoesArrayListContainValue(hostileIPs,IP);
	}
	
	@Override
	public void recordHostileIP(String IP) {
		AddToArrayList(hostileIPs,IP);
	}
	
	@Override
	public void removeHostileIP(String IP) {
		RemoveFromArrayList(hostileIPs,IP);
	}
	
	/*
	 * Helpers
	 */
	
	/***
	 * Handles the inner generic method of adding a new attempt
	 * @param RLIdentity
	 * @param lookupKey
	 * @param maxSeconds
	 * @param maxAttempts
	 * @return
	 */
	private boolean RecoredNewAttemptInner(RateLimitedIdentity RLIdentity,
										   int maxAttempts,
										   int maxSeconds) {
		RateLimitingMap RLMap = GetAttemptMapForIdentity(RLIdentity);
		String lookupKey = GetAttemptKeyForIdentity(RLIdentity);
		if(RLMap.MapsFromKey(lookupKey)) {
			ClearOldAttemptsFromAttemptMap(RLMap,lookupKey,maxSeconds);
			if(IdentityHasTooManyAttempts(RLMap, lookupKey, maxAttempts)){
				return GetRecordAttemptMessage(RLIdentity,null);
			} else {
				// If not at maximum attempts, record the current attempt
				LocalDateTime now = 
							RLMap.AddCurrentTimeToExistingQueue(lookupKey);
				return GetRecordAttemptMessage(RLIdentity,now);
			}
		} else if(maxAttempts > 0) {
			// If not at maximum attempts, record the current attempt, 
			// after creating the record for the IP
			LocalDateTime now = RLMap.MakeNewQueueWithNowAtTip(lookupKey);
			return GetRecordAttemptMessage(RLIdentity,now);
		} else {
			return GetRecordAttemptMessage(RLIdentity,null);
		}
	}
	
	/***
	 * Returns the map being used for the context of 
	 * the RateLimitedIdentity context
	 * @param RLIdentity
	 * @return
	 */
	private RateLimitingMap GetAttemptMapForIdentity(
									 		   RateLimitedIdentity RLIdentity){
		switch(RLIdentity.GetRateLimitedIdentityType()) {
			case IP:
				return IPAttempts;
			case User:
				return UserAttempts;
			case Endpoint:
				if(EndpointAttempts.containsKey(RLIdentity.GetIdentity())) {
					return EndpointAttempts.get(RLIdentity.GetIdentity());
				} else {
					return null;
				}
			default:
				return null;
		}
	}
	
	/***
	 * Returns the lookup key to be checked against in the map for
	 * the local context of the RateLimitedIdentity
	 * @param RLIdentity
	 * @return
	 */
	private String GetAttemptKeyForIdentity(RateLimitedIdentity RLIdentity){
		switch(RLIdentity.GetRateLimitedIdentityType()) {
			case IP:
				return RLIdentity.GetIdentity();
			case User:
				return RLIdentity.GetIdentity();
			case Endpoint:
				return RLIdentity.GetEndpoint();
			default:
				return null;
		}
	}
	
	/***
	 * Handles the logic specific to when we need to make a new "String ->
	 * RateLimitingMap" entry
	 * @param RLIdentity
	 * @param EndpointMap
	 * @return
	 */
	private boolean MakeEndpointMapWithNewQueue(RateLimitedIdentity RLIdentity,
									 ConcurrentHashMap<String,
									 	RateLimitingMap
									 > EndpointMap) {
		RateLimitingMap newMap = new RateLimitingMap();
		LocalDateTime now = newMap.MakeNewQueueWithNowAtTip(
													 RLIdentity.GetEndpoint());
		EndpointMap.put(RLIdentity.GetIdentity(),newMap);
		return GetRecordAttemptMessage(RLIdentity,now);
	}
	
	/***
	 * Destroys old recorded attempts from the RateLimitingMaps
	 * @param attemptMap
	 * @param lookupKey
	 * @param maxSeconds
	 */
	private void ClearOldAttemptsFromAttemptMap(RateLimitingMap attemptMap,
			   							   String lookupKey,
			   							   int maxSeconds) {
		LocalDateTime tip;
		while((tip = attemptMap.PeekQueueTip(lookupKey)) != null) {
			if(AttemptIsOld(tip,maxSeconds)){
				attemptMap.PollQueueTip(lookupKey);
			} else {
				break;
			}
		}
	}
	
	/***
	 * Checks whether a single LocalDateTime entry is too old to keep storing
	 * @param oldTime
	 * @param maxSeconds
	 * @return
	 */
	private boolean AttemptIsOld(LocalDateTime oldTime, int maxSeconds) {
		return oldTime.isBefore(LocalDateTime.now().minusSeconds(maxSeconds)
			 .plusNanos(RateLimitingMap.deduplicationThresholdPerMilliSecond));
	}
	
	/***
	 * Checks whether a RateLimitingMap already stores too many attempts
	 * from a given identity against which the attempts are stored.
	 * @param attemptMap
	 * @param lookupKey
	 * @param maxAttempts
	 * @return
	 */
	private boolean IdentityHasTooManyAttempts(RateLimitingMap attemptMap,
				   						   String lookupKey,
				   						   int maxAttempts) {
		return (attemptMap.GetQueue(lookupKey).size() >= maxAttempts);
	}
	
	/***
	 * Handles presenting a message to the terminal and returning according to
	 * whether or not a new attempt's time was successfully stored, after
	 * having run through all the logic to determine if it would be stored
	 * or discarded.
	 * @param RLIdentity
	 * @param StoredOn
	 * @return
	 */
	private boolean GetRecordAttemptMessage(RateLimitedIdentity RLIdentity,
											  LocalDateTime StoredOn) {
		String endingWord = "<Endpoint?>";
		String type = RLIdentity.GetRateLimitedIdentityType().toString();
		if(RLIdentity.IsIdentityAnEndpointAttempt()) {
			endingWord = "|"+RLIdentity.GetEndpoint()+"|";
		}
		if(StoredOn == null) {
			System.out.println("Datastore: Store new "+type+
							" attempt | Not stored, "+RLIdentity.GetIdentity()+
							" "+endingWord+" has too many already!");
			return false;
		} else {
			System.out.println("Datastore: Store new "+type+
					" attempt | Stored "+RLIdentity.GetIdentity()+
					" "+endingWord+" on "+StoredOn.toString());
			return true;
		}
	}
	
	/***
	 * Checks the presence of a value in an ArrayList
	 * @param list
	 * @param Value
	 * @return
	 */
	private <T> boolean DoesArrayListContainValue(ArrayList<T> list, T Value){
		return list.contains(Value);
	}
	
	/***
	 * Add a value to an ArrayList
	 * @param list
	 * @param Value
	 */
	private <T> void AddToArrayList(ArrayList<T> list, T Value){
		if(!DoesArrayListContainValue(list,Value)) {
			list.add(Value);
		}
	}
	
	/***
	 * Remove a value from an ArrayList
	 * @param list
	 * @param Value
	 */
	private <T> void RemoveFromArrayList(ArrayList<T> list, T Value){
		if(DoesArrayListContainValue(list,Value)) {
			list.remove(Value);
		}
	}
	
	
}
