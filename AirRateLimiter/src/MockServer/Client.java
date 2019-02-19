package MockServer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;

public class Client {

	/*
	 * The members of the "client" are such that you need a new instance
	 * for every connection you attempt to make to the server class
	 */
	final private String targetURL;
	final private int targetPort;
	private Socket socket;
	private InputStreamReader inputStreamReader;
	private BufferedReader bufferedReader;
	private PrintWriter printWriter;
	private BufferedOutputStream bufferedOutputStream;
	final private String method;
	final private String targetResource;
	final private String userCredentials;
	final private String basicAuth;
	
	/***
	 * Construct a Client without an attached socket or streams
	 * @param targetURL
	 * @param targetPort
	 * @param method
	 * @param targetResource
	 * @param username
	 * @param password
	 * @throws IOException
	 */
	public Client(String targetURL, 
				  int targetPort, 
				  String method, 
				  String targetResource, 
				  String username, 
				  String password) throws IOException {
		this.targetURL = targetURL;
		this.targetPort = targetPort;
		this.method = method;
		this.targetResource = targetResource;
		this.userCredentials = username+":"+password;
		this.basicAuth = "Basic " + new String(Base64.getEncoder()
									 .encode(this.userCredentials.getBytes()));
	}
	
	/***
	 * Opens the streams, write the request headers, read the 
	 * response as a string, then close the streams and socket
	 * @return
	 * @throws IOException
	 */
	public String SubmitRequest() throws IOException {
		openStreams();
		writeHeaders();
	    StringBuilder response = new StringBuilder();
	    String line;
	    while ((line = bufferedReader.readLine()) != null) {
	      response.append(line);
	      response.append('\r');
	    }
	    closeStreams();
	    return response.toString();
	}
	
	/***
	 * Open the socket and attach the streams to the socket
	 * @throws IOException
	 */
	public void openStreams() throws IOException {
		socket = new Socket(targetURL,targetPort);
		OutputStream os = socket.getOutputStream();
		inputStreamReader = new InputStreamReader(socket.getInputStream());
		bufferedReader = new BufferedReader(inputStreamReader);
		printWriter = new PrintWriter(os);
		bufferedOutputStream = new BufferedOutputStream(os);
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
	
	/***
	 * Close the streams and then the socket.
	 * @throws IOException
	 */
	public void closeStreams() throws IOException {
		this.bufferedReader.close();
		this.inputStreamReader.close();
		this.printWriter.close();
		this.bufferedOutputStream.close();
		this.socket.close();
	}
	
	
	public static void main(String args[] ) throws IOException  { 
		Client client = new Client("localhost",
								   8085,
								   "GET",
								   "GG/M8",
								   "VeryUser",
								   "SuchPassword");
		String response = client.SubmitRequest();
		System.out.println(response);
	}
	
}
