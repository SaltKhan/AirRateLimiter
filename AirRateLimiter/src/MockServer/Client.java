package MockServer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;

public class Client {

	final private String targetURL;
	final private int targetPort;
	private Socket socket;
	private InputStreamReader inputStreamReader;
	private BufferedReader bufferedReader;
	private PrintWriter printWriter;
	private BufferedOutputStream bufferedOutputStream;
	private String method;
	private String targetResource;
	private String userCredentials;
	private String basicAuth;
	
	public Client(String targetURL, int targetPort, String method, String targetResource, String username, String password) throws IOException {
		this.targetURL = targetURL;
		this.targetPort = targetPort;
		this.method = method;
		this.targetResource = targetResource;
		this.userCredentials = username+":"+password;
		this.basicAuth = "Basic " + new String(Base64.getEncoder().encode(this.userCredentials.getBytes()));
	}
	
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
	
	public void openStreams() throws IOException {
		this.socket = new Socket(this.targetURL,this.targetPort);
		this.inputStreamReader = new InputStreamReader(this.socket.getInputStream());
		this.bufferedReader = new BufferedReader(inputStreamReader);
		this.printWriter = new PrintWriter(this.socket.getOutputStream());
		this.bufferedOutputStream = new BufferedOutputStream(this.socket.getOutputStream());
	}
	
	public void writeHeaders() {
		printWriter.flush();
	    printWriter.println(this.method+" "+this.targetResource+" HTTP/1.1");
	    printWriter.println("Authorization: "+this.basicAuth);
	    printWriter.println();
	    printWriter.flush();
	}
	
	public void closeStreams() throws IOException {
		this.bufferedReader.close();
		this.inputStreamReader.close();
		this.printWriter.close();
		this.bufferedOutputStream.close();
		this.socket.close();
	}
	
	public static void main(String args[] ) throws IOException  { 
		Client client = new Client("localhost",8085,"GET","GG/M8","VeryUser","SuchPassword");
		String response = client.SubmitRequest();
		System.out.println(response);
	}
	
}
