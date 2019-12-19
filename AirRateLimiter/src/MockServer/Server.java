package MockServer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import RateLimiterService.IRateLimiter;
import DataStore.IDataStore;

/***
 * The Server class designates on object to be injected with an instance of the
 * IRateLimiter interface (or, extensibly, any actualised instance of any 
 * interface that might be added to the design, following an interface 
 * injection paradigm). The IRateLimiter exposes methods for the Server class 
 * to utilise to implement a rate limiting methodology, upon a request being
 * opened by an instance of the Client class.
 */
public class Server extends ServerBase { 
	
	///////////////////////////////////////////////////////////////////////////
	//                             Parameters                                //
	///////////////////////////////////////////////////////////////////////////
	
	/***
	 * Maps the port integers to the ServerSocketListener listening on the
	 * ServerSocket open on that port.
	 */
	final private HashMap<Integer,ServerSocketListener> serverSocketListeners =
			new HashMap<Integer,ServerSocketListener>();
	
	/***
	 * Verbose message output to streams Out and Err
	 */
	private boolean verbose = false;
	
	/***
	 * Injected instance of the rate limiter service.
	 */
	/* TODO: For extensibility, if more injected services were added, to make
	 * the allocation and passing of instances of the services easier on the
	 * constructors, a "ServiceCollection" class may be of value.
	 */
	final private IRateLimiter rateLimiter;
	
	///////////////////////////////////////////////////////////////////////////
	//                             Constructors                              //
	///////////////////////////////////////////////////////////////////////////
	
	/***
	 * Default constructor. Adds no server sockets and leaves verbose off.
	 */
	public Server(IRateLimiter rateLimiter) {
		this.rateLimiter = rateLimiter;
	}
	
	/***
	 * Start a server instance with no server sockets, but choose 
	 * to start with verbose on or off.
	 * @param verbosity
	 */
	public Server(boolean verbosity, IRateLimiter rateLimiter) {
		this.verbose = verbosity;
		this.rateLimiter = rateLimiter;
	}
	
	/***
	 * Start a server instance with a single server socket
	 * @param port
	 */
	public Server(IRateLimiter rateLimiter, int port) {
		AddServerSocket(port);
		this.rateLimiter = rateLimiter;
	}
	
	/***
	 * Start a server instance with many server sockets!
	 * @param ports
	 */
	public Server(IRateLimiter rateLimiter, int... ports) {
		for(int port : ports) {
			AddServerSocket(port);
		}
		this.rateLimiter = rateLimiter;
	}
	
	///////////////////////////////////////////////////////////////////////////
	//                             Verbosity                                 //
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public void turnOnVerbose() {
		verbose = true;
	}
	
	@Override
	public void turnOffVerbose() {
		verbose = false;
	}
	
	@Override
	protected boolean isVerboseMessagingEnabled() {
		return verbose;
	}
	
	///////////////////////////////////////////////////////////////////////////
	//                             Sockets                                   //
	///////////////////////////////////////////////////////////////////////////

	@Override
	protected void addMappedServerSocketListenerOpenOnPort(int port, ServerSocketListener socketListener) {
		serverSocketListeners.put(port, socketListener);
	}

	@Override
	protected ServerSocketListener getServerSocketListenerOpenOnPort(int port) {
		return serverSocketListeners.get(port);
	}

	@Override
	protected ServerSocketListener removeServerSocketListenerOpenOnPort(int port) {
		return serverSocketListeners.remove(port);
	}

	@Override
	protected void startNewClientSocketListenerThread(Socket clientSocket) throws InterruptedIOException, IOException, HostileIP {
		ClientSocketListener clientSocketListener = new ClientSocketListener(clientSocket);
		(new Thread(clientSocketListener)).start();
	}
	
	///////////////////////////////////////////////////////////////////////////
	//                        Client Socket Listener                         //
	///////////////////////////////////////////////////////////////////////////

	/***
	 * Handle the case of client sockets being opened in their own threads. The
	 * rate limiting functionality is mainly segregated into this thread, using
	 * the rate limiting service to determine (perhaps after peeking at the
	 * request) how the connection request will be handled!
	 */
	private class ClientSocketListener extends SocketedIOConglomerate implements Runnable {
		
		/***
		 * Constant used to detect the header that passes the authorisation
		 * string/token, to search for the auth header and use it as part of
		 * the rate limiting service, rate limiting based on authorisation.
		 */
		final static private String authHeaderPrefix = "Authorization: ";
		
		//TODO: Add more authorisation headers.
		
		/***
		 * Instantiate IO streams related to the incoming socket connection.
		 * Throw if the connection is from a hostile IP.
		 * @param clientSocket
		 * @throws IOException
		 * @throws HostileIP
		 */
		public ClientSocketListener(Socket clientSocket) throws IOException, HostileIP {
			super(clientSocket);
			if(rateLimiter.IsIPHostile(getSocket())) {
				throw new HostileIP(getSocketHostAddress());
			} else {
				openStreams();
			}
		}
		
		/***
		 * Close the streams and print a message if they fail to close, and
		 * also print the incoming closing message.
		 * @param closureMessage
		 */
		private void closeStreams(String closureMessage) {
			try {
				closeStreams();
				closeSocket();
			} catch (IOException e) {
				e.printStackTrace();
				printOutVerboseMessage("Failed to close a client socket connection");
			}
			printOutVerboseMessage(closureMessage);
		}
		
		/***
		 * Responds to the incoming request, passing it through a rate limiter
		 * service. This contains the actual utilisation by the server class of
		 * the rate limiting service, besides the hostile IP prior to opening
		 * the streams for the client socket.
		 */
		@Override
		public void run() {
			String closureMessage = "";
			String clientIP = getSocketHostAddress();
			try {
				// We must read the tokens and headers before rate limiting
				// Read the tokens
				String[] tokens = ReadTokens(this.bufferedReader);
				String method = tokens[0];
				String resource = tokens[1];
				String protocol = tokens[2];
				// Read the headers
				String[] headers = ReadHeaders(this.bufferedReader);
				// Get the auth string from the headers.
				String auth = AuthFromHeaders(headers);
				// Use the rate limiting services to construct the end point
				// to the requested method / resource, using the rate limiting
				// services internal end point context.
				String endpoint = rateLimiter.FormEndpointStringFromVerbAndResource(method,resource);
				// Now return 401 or 403 if Auth is invalid 
				// or missing and we need it!
				closureMessage = rateLimiter.ServeHttp40XPerUserAuth(printWriter,auth);
				if(closureMessage.isEmpty()) {
					// Form the "rateLimitedIdentity" from the context supplied
					// to the RateLimiter when it was instantiated. Supply the 
					// context with the clientIP, Auth and Endpoint and 
					// retrieve an "Identity" aware of its own context
					IDataStore.RateLimitedIdentity rateLimitedIdentity = 
					  rateLimiter.GetRateLimitedIdentityFromRateLimiterContext(clientIP,auth,endpoint);
					// Check if the Rate Limiting context will
					// rate limit this attempt
					closureMessage = rateLimiter.IsAttemptRateLimited(rateLimitedIdentity);
					if(closureMessage.isEmpty()) {
						// If it wasn't rate limited, then handle the request.
						HandleRequest(method, resource, protocol, headers, bufferedReader, printWriter, bufferedOutputStream);
						closureMessage = "Serviced the request from IP "+clientIP+"; User "+auth+"; Resource "+resource;
					} else {
						// If it was rate limited, then serve the 
						// 429 for the appropriate context.
						rateLimiter.ServeHttp429PerAttempt(printWriter,rateLimitedIdentity);
					}
				}
			} catch (IOException e) {
				// Print messages if the incoming stream couldn't be processed
				// independent of the rate limiting service.
				e.printStackTrace();
				printOutVerboseMessage("Failed to interact with the socket's streams, closing connection to port "+getSocketPort()+" from "+clientIP);
			} finally {
				closeStreams(closureMessage);
				System.out.println();
			}
		}
		
		/***
		 * We expect the first line of the incoming connection to be three 
		 * tokens; Reads the Method, Resource and Protocol
		 * @param reader
		 * @return "Method, Resource, Protocol"
		 * @throws IOException
		 */
		private String[] ReadTokens(BufferedReader reader) throws IOException {
			String tokenString = reader.readLine(); 
			StringTokenizer tokeniser = new StringTokenizer(tokenString);
			String[] tokens = new String[3];
			// method 
			tokens[0] = tokeniser.nextToken().toUpperCase();
			// resource 
			tokens[1] = tokeniser.nextToken();
			// protocol 
			tokens[2] = tokeniser.nextToken();
			return tokens;
		}
		
		/***
		 * Read the headers following the tokens; Read until an empty line.
		 * @param reader
		 * @return Headers
		 * @throws IOException
		 */
		private String[] ReadHeaders(BufferedReader reader) throws IOException{
			ArrayList<String> headers = new ArrayList<String>();
			String line = reader.readLine(); 
			while (!line.isEmpty()) {
				headers.add(line);
				line = reader.readLine();
			}
			String[] retHeaders = new String[headers.size()];
			return headers.toArray(retHeaders);
		}
		
		/***
		 * Extracts the string passed under the authorisation header
		 * @param headers
		 * @return String passed under the authorisation header
		 */
		/* TODO: When more auth header prefixes are added, this will need a way
		 * of reading multiple, and passing back a map from auth header to auth
		 * token received in the headers.
		 */
		private String AuthFromHeaders(String[] headers) {
			String auth = "";
			for(String header : headers) {
				if(header.startsWith(authHeaderPrefix)) {
					auth = header.substring(authHeaderPrefix.length());
					break;
				}
			}
			return auth;
		}
		
	}
	
	///////////////////////////////////////////////////////////////////////////
	//        Handle the response sent by the Client Socket Listener         //
	///////////////////////////////////////////////////////////////////////////
	
	/* TODO: Although the below are called to from the ClientSocketListener and
	 * used to respond to the attempted incoming request, and "seem" to work,
	 * as the focus of this task is demonstrating the rate limiting service,
	 * and this is not how you would handle incoming or outgoing http requests
	 * if you were actually integrating a service into an existing framework,
	 * the below methods are not thoroughly tested beyond that they seem to
	 * work without issue in supplying the response to the client instance
	 * during testing of other functions.
	 */
	
	/***
	 * Handle a request that has passed the rate limiter. 
	 * Expected to serve an http response before returning.
	 * @param tokens
	 * @param headers
	 * @param reader
	 * @param printWriter
	 * @param bufferedOutputStream
	 * @throws IOException
	 */
	private void HandleRequest(String method, 
							   String resource, 
							   String protocol,
							   String[] headers, 
							   BufferedReader reader, 
							   PrintWriter printWriter, 
							   BufferedOutputStream bufferedOutputStream)
									   throws IOException {
		System.out.println("Method: "+method);
		System.out.println("Resource: "+resource);
		System.out.println("Protocol: "+protocol);
		for(String header : headers) {
			System.out.println(header);
		}
		ArrayList<String> content = new ArrayList<String>();
		if(reader.ready()) {
			String line = reader.readLine();
			while (!line.isEmpty()) {
				System.out.println(line);
				content.add(line);
				line = reader.readLine();
			}
		}
		ArrayList<ResponsePart> response = RouteRequest(method, resource, headers, content);
		sendResponseToClient(printWriter,bufferedOutputStream,response);
		System.out.println();
	}
	
	/***
	 * Wrapper for routing the content and headers to the 
	 * appropriate method for the requested method|resource
	 * @param method
	 * @param resource
	 * @param headers
	 * @param content
	 * @return
	 */
	private ArrayList<ResponsePart> RouteRequest(String method, 
												 String resource, 
												 String[] headers, 
												 ArrayList<String> content) {
		ArrayList<ResponsePart> returnMessage = new ArrayList<ResponsePart>();
		returnMessage.add(new ResponsePart("HTTP/1.1 200 Woo"));
		returnMessage.add(new ResponsePart(""));
		return returnMessage;
	}
	
	/***
	 * A class to control the flushing of strings and 
	 * byte streams to the output streams.
	 */
	private class ResponsePart {
		
		final boolean isString;
		final String string;
		final byte[] byteArr;
		
		public ResponsePart(String stringIn) {
			this.isString = true;
			this.string = stringIn;
			this.byteArr = null;
		}
		
		@SuppressWarnings("unused")
		public ResponsePart(byte[] byteArrIn) {
			this.isString = false;
			this.string = null;
			this.byteArr = byteArrIn.clone();
		}
		
	}
	
	/***
	 * Sends an ArrayList<ResponsePart> through a PrintWriter 
	 * and BufferedOutputStream to the client
	 * @param printWriter
	 * @param bufferedOutputStream
	 * @param response
	 * @throws IOException
	 */
	private void sendResponseToClient(PrintWriter printWriter, 
									BufferedOutputStream bufferedOutputStream,
									ArrayList<ResponsePart> response) 
											throws IOException {
		boolean contextIsString = response.get(0).isString;
		boolean contextSwapped = false;
		for(ResponsePart respPart : response) {
			contextSwapped = ((contextIsString && !respPart.isString) || 
							  (!contextIsString && respPart.isString));
			contextIsString = respPart.isString;
			// Flush on context swapping, to allow stream interleaving
			if(contextSwapped) {
				if(contextIsString) {
					bufferedOutputStream.flush();
				} else {
					printWriter.flush();
				}
			}
			if(contextIsString) {
				printWriter.println(respPart.string);
			} else {
				bufferedOutputStream.write(respPart.byteArr);
			}
		}
		if(contextIsString) {
			printWriter.flush();
		} else {
			bufferedOutputStream.flush();
		}
	}
	
	//public static void main(String args[]) { 
		//System.out.println(IDataStore.RateLimitedIdentity.RateLimitedIdentityType.User.toString());
	//}

}
