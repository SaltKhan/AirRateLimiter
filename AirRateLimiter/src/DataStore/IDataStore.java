package DataStore;

import java.time.LocalDateTime;

/***
 * Mock implementation of a data store, housed in application memory
 * @author NathanLevett
 *
 */
public interface IDataStore {

	/***
	 * Query the data store for hostile IP
	 * @param IP
	 * @return
	 */
	public boolean containsHostileIP(String IP);
	
	/***
	 * Query the data store for availability to record a new attempt from an IP
	 * @param IP
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	public boolean RecordNewIPAttempt(String IP, int maxAttempts, int maxSeconds);
	
	/***
	 * Query the data store for availability to record a new attempt from a User
	 * @param UserAuth
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	public boolean RecordNewUserAttempt(String UserAuth, int maxAttempts, int maxSeconds);
	
	/***
	 * Query the data store for availability to record a new attempt from an identity accessing an endpoint
	 * @param Identity
	 * @param Endpoint
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	public boolean RecordNewEndpointAttempt(String Identity, String Endpoint, int maxAttempts, int maxSeconds);
	
	/***
	 * Query the data store to check when the next available request by an IP will be allowed
	 * If the next attempt is allowed now, will return the current time
	 * @param IP
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	public LocalDateTime CheckWhenNextRequestAllowedByIP(String IP, int maxAttempts, int maxSeconds);
	
	/***
	 * Query the data store to check when the next available request by a User will be allowed
	 * If the next attempt is allowed now, will return the current time
	 * @param UserAuth
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	public LocalDateTime CheckWhenNextRequestAllowedByUser(String UserAuth, int maxAttempts, int maxSeconds);
	
	/***
	 * Query the data store to check when the next available request by an identity accessing an endpoint will be allowed.
	 * If the next attempt is allowed now, will return the current time
	 * @param Identity
	 * @param Endpoint
	 * @param maxAttempts
	 * @param maxSeconds
	 * @return
	 */
	public LocalDateTime CheckWhenNextEndpointRequestAllowedByIdentity(String Identity, String Endpoint, int maxAttempts, int maxSeconds);
	
	/***
	 * Store a user Authorization string in a list of known users
	 * @param UserAuth
	 */
	public void StoreUserAuth(String UserAuth);
	
	/***
	 * Query the data store whether it contains a given user's Authorization
	 * @param UserAuth
	 * @return
	 */
	public boolean IsUserAuthValid(String UserAuth);
	
}
