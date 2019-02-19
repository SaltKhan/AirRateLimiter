package DataStoreTest;

import DataStore.IDataStore;
import DataStore.IDataStore.RateLimitedIdentity;
import DataStore.IDataStore.RateLimitingMap;

/***
 * Base class to provide quick spin-up of SUT objects related to the IDataStore
 */
public class IDataStoreTestBase {
	
	public static final String testIP = "8.8.8.8";
	public static final String testUser = "Igor";
	public static final String testIdentity = "SuchIdentity";
	public static final String testEndpoint = "VeryResource";
	public static final String key = "A key";
	public static final String anotherKey = "Another key";
	public static final int anHour = 3600;
	public static final int manyAttempts = 5;
	
	/***
	 * @return A new instance of the IDataStore's inner class RateLimitingMap
	 */
	public RateLimitingMap NewTestRateLimitingMap() {
		return IDataStore.NewRateLimitingMap();
	}
	
	/***
	 * @param IP
	 * @return A new instance of the IDataStore's inner class 
	 * RateLimitedIdentity, assigned the "IP" type
	 */
	public RateLimitedIdentity NewTestRateLimitedIP(String IP) {
		return IDataStore.NewRateLimitedIP(IP);
	}
	
	/***
	 * @return A new instance of the IDataStore's inner class 
	 * RateLimitedIdentity, assigned the "IP" type, with the generic input
	 */
	public RateLimitedIdentity NewTestRateLimitedIP() {
		return NewTestRateLimitedIP(testIP);
	}
	
	/***
	 * @param User
	 * @return A new instance of the IDataStore's inner class 
	 * RateLimitedIdentity, assigned the "User" type
	 */
	public RateLimitedIdentity NewTestRateLimitedUser(String User) {
		return IDataStore.NewRateLimitedUser(User);
	}
	
	/***
	 * @return A new instance of the IDataStore's inner class 
	 * RateLimitedIdentity, assigned the "User" type, with the generic input
	 */
	public RateLimitedIdentity NewTestRateLimitedUser() {
		return NewTestRateLimitedUser(testUser);
	}
	
	/***
	 * @param Identity
	 * @param Endpoint
	 * @return A new instance of the IDataStore's inner class 
	 * RateLimitedIdentity, assigned the "Endpoint" type
	 */
	public RateLimitedIdentity NewTestRateLimitedEndpoint(String Identity, 
														  String Endpoint) {
		return IDataStore.NewRateLimitedEndpoint(Identity,Endpoint);
	}
	
	/***
	 * @return A new instance of the IDataStore's inner class 
	 * RateLimitedIdentity, assigned the "Endpoint" type, 
	 * with the generic input
	 */
	public RateLimitedIdentity NewTestRateLimitedEndpoint() {
		return NewTestRateLimitedEndpoint(testIdentity,testEndpoint);
	}

}
