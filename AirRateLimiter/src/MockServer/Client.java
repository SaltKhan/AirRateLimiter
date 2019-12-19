package MockServer;

import java.io.IOException;
import java.util.Base64;

/***
 * The "Client" class serves as a class with which to test the (rate-limiting)
 * functionality of the server class, utilised as an abstracted way of opening
 * socket requests against an open socket of the server class, such that a
 * client has no visibility on the (rate-limiting) activity of the server,
 * besides the response from the server upon a request being made. In this way,
 * a client is used to make repeated requests of different types, to track
 * whether and when specific attempts are (rate-limited) or rejected.
 * 
 * The members of the "client" are such that you need a new instance for every 
 * connection you attempt to make to the server class, where each connection
 * has a unique "method", "target resource", "user credentials" & "basic auth"
 */
public class Client extends SocketedIOConglomerate {
	
	///////////////////////////////////////////////////////////////////////////
	//                             Parameters                                //
	///////////////////////////////////////////////////////////////////////////
	
	final private String method;
	final private String targetResource;
	final private String userCredentials;
	final private String basicAuth;
	
	/***
	 * Construct a Client without an attached socket or streams
	 * @param targetHost
	 * @param targetPort
	 * @param method
	 * @param targetResource
	 * @param username
	 * @param password
	 * @throws IOException
	 */
	public Client(String targetHost, 
				  int targetPort, 
				  String method, 
				  String targetResource, 
				  String username, 
				  String password) throws IOException {
		super(targetHost,targetPort);
		this.method = method;
		this.targetResource = targetResource;
		this.userCredentials = username+":"+password;
		this.basicAuth = "Basic " + new String(Base64.getEncoder().encode(this.userCredentials.getBytes()));
	}
	
	/***
	 * Opens the streams, write the request headers, read the 
	 * response as a string, then close the streams and socket
	 * @return
	 * @throws IOException
	 */
	public String SubmitRequest() throws IOException {
		reconnectSocket();
		openStreams();
		writeHeaders();
	    StringBuilder response = new StringBuilder();
	    String line;
	    while ((line = bufferedReader.readLine()) != null) {
	      response.append(line);
	      response.append('\r');
	    }
	    closeStreams();
	    closeSocket();
	    return response.toString();
	}
	
	/***
	 * Write the Http Headers relevant to this client to the printWriter stream
	 */
	public void writeHeaders() {
		printWriter.flush();
	    printWriter.println(this.method+" "+this.targetResource+" HTTP/1.1");
	    printWriter.println("Authorization: "+this.basicAuth);
	    printWriter.println();
	    printWriter.flush();
	}
	
}
