package DataStore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DataStore implements IDataStore {

	//Aside from these "data storing" objects, ideally the data store shouldn't host any other data members. It's blind to them.
	//For this reason the methods here from the IDataStore are obtusely named to do all checks in one!
	private ArrayList<String> hostileIPs;
	private ArrayList<String> ValidUserAuths;
	private ConcurrentHashMap<String,ConcurrentLinkedQueue<LocalDateTime>> IPAttempts;
	private ConcurrentHashMap<String,ConcurrentLinkedQueue<LocalDateTime>> UserAttempts;
	private ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentLinkedQueue<LocalDateTime>>> EndpointAttempts;
	
	public DataStore() {
		this.hostileIPs = new ArrayList<String>();
		this.ValidUserAuths = new ArrayList<String>();
		this.IPAttempts = new ConcurrentHashMap<String,ConcurrentLinkedQueue<LocalDateTime>>();
		this.UserAttempts = new ConcurrentHashMap<String,ConcurrentLinkedQueue<LocalDateTime>>();
		this.EndpointAttempts = new ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentLinkedQueue<LocalDateTime>>>();
	}
	
	public boolean containsHostileIP(String IP) {
		return hostileIPs.contains(IP);
	}
	
	public boolean RecordNewIPAttempt(String IP, int maxAttempts, int maxSeconds) {
		if(IPAttempts.containsKey(IP)) {
			// Clear old attempts
			LocalDateTime tip;
			while((tip = IPAttempts.get(IP).peek()) != null) {
				if(tip.isBefore(LocalDateTime.now().minusSeconds(maxSeconds))) {
					IPAttempts.get(IP).poll();
				} else {
					break;
				}
			}
			// Check for maximum attempts
			if(IPAttempts.get(IP).size() >= maxAttempts) {
				System.out.println("Datastore: Store new IP attempt | Not stored, "+IP+" has too many already!");
				return false;
			} else {
				// If not at maximum attempts, record the current attempt
				LocalDateTime now = LocalDateTime.now();
				IPAttempts.get(IP).add(now);
				System.out.println("Datastore: Store new IP attempt | Stored "+IP+" on "+now.toString());
				return true;
			}
		} else {
			// If not at maximum attempts, record the current attempt, after creating the record for the IP
			ConcurrentLinkedQueue<LocalDateTime> newQueue = new ConcurrentLinkedQueue<LocalDateTime>();
			LocalDateTime now = LocalDateTime.now();
			newQueue.add(now);
			IPAttempts.put(IP,newQueue);
			System.out.println("Datastore: Store new IP attempt | Stored "+IP+" on "+now.toString());
			return true;
		}
	}
	
	public boolean RecordNewUserAttempt(String User, int maxAttempts, int maxSeconds) {
		if(UserAttempts.containsKey(User)) {
			// Clear old attempts
			LocalDateTime tip;
			while((tip = UserAttempts.get(User).peek()) != null) {
				if(tip.isBefore(LocalDateTime.now().minusSeconds(maxSeconds))) {
					UserAttempts.get(User).poll();
				} else {
					break;
				}
			}
			// Check for maximum attempts
			if(UserAttempts.get(User).size() >= maxAttempts) {
				System.out.println("Datastore: Store new User attempt | Not stored, "+User+" has too many already!");
				return false;
			} else {
				// If not at maximum attempts, record the current attempt
				LocalDateTime now = LocalDateTime.now();
				UserAttempts.get(User).add(now);
				System.out.println("Datastore: Store new User attempt | Stored "+User+" on "+now.toString());
				return true;
			}
		} else {
			// If not at maximum attempts, record the current attempt, after creating the record for the user
			ConcurrentLinkedQueue<LocalDateTime> newQueue = new ConcurrentLinkedQueue<LocalDateTime>();
			LocalDateTime now = LocalDateTime.now();
			newQueue.add(now);
			UserAttempts.put(User,newQueue);
			System.out.println("Datastore: Store new User attempt | Stored "+User+" on "+now.toString());
			return true;
		}
	}
	
	public boolean RecordNewEndpointAttempt(String Identity, String Endpoint, int maxAttempts, int maxSeconds) {
		// Anticipates that "Endpoint" will be the combination of Http verb and resource
		if(EndpointAttempts.containsKey(Identity)) {
			if(EndpointAttempts.get(Identity).containsKey(Endpoint)) {
				// Clear old attempts
				LocalDateTime tip;
				while((tip = EndpointAttempts.get(Identity).get(Endpoint).peek()) != null) {
					if(tip.isBefore(LocalDateTime.now().minusSeconds(maxSeconds))) {
						EndpointAttempts.get(Identity).get(Endpoint).poll();
					} else {
						break;
					}
				}
				// Check for maximum attempts
				if(EndpointAttempts.get(Identity).get(Endpoint).size() >= maxAttempts) {
					System.out.println("Datastore: Store new endpoint attempt | Not stored, "+Identity+" has too many already for the endpoint "+Endpoint);
					return false;
				} else {
					// If not at maximum attempts, record the current attempt
					LocalDateTime now = LocalDateTime.now();
					EndpointAttempts.get(Identity).get(Endpoint).add(now);
					System.out.println("Datastore: Store new endpoint attempt | Stored "+Identity+" at "+Endpoint+" on "+now.toString()+"; Stored in existing endpoint cache");
					return true;
				}
			} else {
				// If not at maximum attempts, record the current attempt, after creating the record for the endpoint per identity
				ConcurrentLinkedQueue<LocalDateTime> newQueue = new ConcurrentLinkedQueue<LocalDateTime>();
				LocalDateTime now = LocalDateTime.now();
				newQueue.add(now);
				EndpointAttempts.get(Identity).put(Endpoint, newQueue);
				System.out.println("Datastore: Store new endpoint attempt | Stored "+Identity+" at "+Endpoint+" on "+now.toString()+"; Stored in existing identity cache");
				return true;
			}
		} else {
			// If not at maximum attempts, record the current attempt, after creating the record for the identity
			ConcurrentLinkedQueue<LocalDateTime> newQueue = new ConcurrentLinkedQueue<LocalDateTime>();
			LocalDateTime now = LocalDateTime.now();
			newQueue.add(now);
			ConcurrentHashMap<String,ConcurrentLinkedQueue<LocalDateTime>> newMap = new ConcurrentHashMap<String,ConcurrentLinkedQueue<LocalDateTime>>();
			newMap.put(Endpoint, newQueue);
			EndpointAttempts.put(Identity,newMap);
			System.out.println("Datastore: Store new endpoint attempt | Stored "+Identity+" at "+Endpoint+" on "+now.toString());
			return true;
		}
	}

	@Override
	public LocalDateTime CheckWhenNextRequestAllowedByIP(String IP, int maxAttempts, int maxSeconds) {
		if(IPAttempts.containsKey(IP)) {
			if(IPAttempts.get(IP).size() >= maxAttempts) {
				return IPAttempts.get(IP).peek().plusSeconds(maxSeconds);
			} else {
				return LocalDateTime.now();
			}
		} else {
			return LocalDateTime.now();
		}
	}

	@Override
	public LocalDateTime CheckWhenNextRequestAllowedByUser(String UserAuth, int maxAttempts, int maxSeconds) {
		if(UserAttempts.containsKey(UserAuth)) {
			if(UserAttempts.get(UserAuth).size() >= maxAttempts) {
				return UserAttempts.get(UserAuth).peek().plusSeconds(maxSeconds);
			} else {
				return LocalDateTime.now();
			}
		} else {
			return LocalDateTime.now();
		}
	}

	@Override
	public LocalDateTime CheckWhenNextEndpointRequestAllowedByIdentity(String Identity, String Endpoint, int maxAttempts, int maxSeconds) {
		if(EndpointAttempts.containsKey(Identity)) {
			if(EndpointAttempts.get(Identity).containsKey(Endpoint)) {
				if(EndpointAttempts.get(Identity).get(Endpoint).size() >= maxAttempts) {
					return EndpointAttempts.get(Identity).get(Endpoint).peek().plusSeconds(maxSeconds);
				} else {
					return LocalDateTime.now();
				}
			} else {
				return LocalDateTime.now();
			}
		} else {
			return LocalDateTime.now();
		}
	}

	@Override
	public void StoreUserAuth(String UserAuth) {
		if(!ValidUserAuths.contains(UserAuth)) {
			ValidUserAuths.add(UserAuth);
		}
	}

	@Override
	public boolean IsUserAuthValid(String UserAuth) {
		return ValidUserAuths.contains(UserAuth);
	}
	
	
}
