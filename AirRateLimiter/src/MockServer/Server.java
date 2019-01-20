package MockServer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException; 
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.ServerSocket; 
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import RateLimiterService.IRateLimiter;
import RateLimiterService.RateLimiter; 
import DataStore.IDataStore;
import DataStore.DataStore;


public class Server { 
	
	private HashMap<Integer,ServerSocket> serverSockets = new HashMap<Integer,ServerSocket>();
	private HashMap<Integer,ServerSocketListener> serverSocketListeners = new HashMap<Integer,ServerSocketListener>();
	private boolean verbose = false;
	final private int retryPolicy = 3;
	final private int ServerSocketTimeoutSeconds = 20;
	final private IRateLimiter rateLimiter;
	
	/***
	 * Default constructor. Adds no server sockets and leaves verbose off.
	 */
	public Server(IRateLimiter rateLimiter) {
		this.rateLimiter = rateLimiter;
	}
	
	/***
	 * Start a server instance with no server sockets, but choose to start with verbose on or off.
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
	public Server(int port, IRateLimiter rateLimiter) {
		AddServerSocket(port);
		this.rateLimiter = rateLimiter;
	}
	
	/***
	 * Start a server instance with many server sockets!
	 * @param ports
	 */
	public Server(int[] ports, IRateLimiter rateLimiter) {
		for(int port : ports) {
			AddServerSocket(port);
		}
		this.rateLimiter = rateLimiter;
	}
	
	/***
	 * Turn on verbose messages!
	 */
	public void turnOnVerbose() {
		verbose = true;
	}
	
	/***
	 * Turn off verbose messages!
	 */
	public void turnOffVerbose() {
		verbose = false;
	}
	
	/***
	 * Add a new server socket to the server instance, and start listening on that port
	 * @param port
	 */
	public void AddServerSocket(int port) {
		InternalAddServerSocket(port, 0);
	}
	
	/***
	 * Internal retry method to attempt to add a server socket and start a listener thread.
	 * @param port
	 * @param retries
	 */
	private void InternalAddServerSocket(int port, int retries) {
		try {
			// Make a new Server Socket and set a timeout
			serverSockets.put(port,new ServerSocket(port));
			if(verbose) {
				System.out.println("Listening for connection on port "+port); 
			}
			serverSockets.get(port).setSoTimeout(ServerSocketTimeoutSeconds*1000);
			// Start a new thread to listen on this socket.
			ServerSocketListener socketListener = new ServerSocketListener(port);
			serverSocketListeners.put(port, socketListener);
			(new Thread(serverSocketListeners.get(port))).start();
		} catch (IOException e) {
			e.printStackTrace();
			// Retry up to the retryPolicy many times.
			if(retries < retryPolicy) {
				if(verbose) {
					System.out.println("Failed to start listening for connection on port "+port+"; retry "+(retries+1)); 
				}
				InternalAddServerSocket(port, retries+1);
			} else if(verbose) {
				System.out.println("Failed to start listening for connection on port "+port+"; Has reached the retry maximum of "+retryPolicy); 
			}
		}
	}
	
	/***
	 * Close an open/existing server socket
	 * @param port
	 */
	public void CloseServerSocket(int port) {
		InternalCloseServerSocket(port, 0);
	}
	
	/***
	 * Internal retry method to close and remove the server socket and listener for a specified port!
	 * @param port
	 * @param retries
	 */
	private void InternalCloseServerSocket(int port, int retries) {
		if(serverSockets.containsKey(port)) {
			try {
				// Close and remove the socket and the thread listening on that socket
				serverSocketListeners.get(port).StopListening();
				serverSocketListeners.remove(port);
				serverSockets.get(port).close();
				serverSockets.remove(port);
				if(verbose) {
					System.out.println("Stopped listening for connection on port "+port); 
				}
			} catch (IOException e) {
				e.printStackTrace();
				// Retry up to the retryPolicy many times.
				if(retries < retryPolicy) {
					if(verbose) {
						System.out.println("Failed to stop listening for connection on port "+port+"; retry "+(retries+1)); 
					}
					InternalCloseServerSocket(port, retries+1);
				} else if(verbose) {
					System.out.println("Failed to stop listening for connection on port "+port+"; Has reached the retry maximum of "+retryPolicy); 
				}
			}
		} else {
			if(verbose) {
				System.out.println("Failed to stop listening for connection on port "+port+"; Not currently listening to this port!"); 
			}
		}
	}
	
	/***
	 * Handle the process of a server socket listening for client acceptance in a thread which manages the spawning
	 * of client socket threads, when an incoming message opens a new socket.
	 * @author NathanLevett
	 *
	 */
	private class ServerSocketListener implements Runnable {
		
		private int port;
		private int socketFailurePolicy = 10;
		private boolean StoppingListening;

		public ServerSocketListener(int port) {
			this.port = port;
			this.StoppingListening = false;
		}
		
		/***
		 * Listen to the ServerSocketListener's port
		 */
		@Override
		public void run() {
			while (true) { 
				try {
					// Wait for a new incoming connection
					Socket clientSocket= serverSockets.get(port).accept();
					if(verbose) {
						System.out.println("Incoming connection to port "+port+" from "+clientSocket.getInetAddress().getHostAddress());
					}
					// Start a new thread to handle the incoming connection
					ClientSocketListener clientSocketListener = new ClientSocketListener(clientSocket);
					(new Thread(clientSocketListener)).start();
				} catch (InterruptedIOException e) {
					// Print a message if the timeout cycles
					if(verbose) {
						System.out.println("The socket "+port+" has timed out ("+ServerSocketTimeoutSeconds+" seconds) waiting for input. Will continue to listen.");
					}
				} catch (IOException e) {
					e.printStackTrace();
					// The server socket is allowed the socketFailurePolicy many IOExceptions before the server socket's thread ends
					socketFailurePolicy--;
					if(socketFailurePolicy == 0) {
						if(verbose) {
							System.out.println("The socket "+port+" has failed too many times, will stop listening.");
						}
						break;
					}
				} catch (HostileIP e) {
					// Custom exception to allow throwing the client socket if from a "hostile IP" as per the rate limiter
					if(verbose) {
						System.out.println("Found hostile IP attempt from "+e.getIP());
					}
					e.printStackTrace();
				} catch (NullPointerException e) {
					if(verbose && this.StoppingListening) {
						System.out.println("Null pointer tripped on port "+port+"; Listener was told it was stopping!");
						break;
					} else if (verbose) {
						System.out.println("Null pointer tripped on port "+port+"; Listener was not told to stop.");
					}
					e.printStackTrace();
				}
			}
			CloseServerSocket(this.port);
		}
		
		/***
		 * Allows the thread to be aware that the socket was forced shut from outside.
		 */
		public void StopListening() {
			this.StoppingListening = true;
		}
	}
	
	/***
	 * Handle the case of client sockets being opened in their own threads. Here's the crunch!
	 * @author NathanLevett
	 *
	 */
	private class ClientSocketListener implements Runnable {
		
		final Socket clientSocket;
		final InputStreamReader inputStreamReader;
		final BufferedReader bufferedReader;
		final PrintWriter printWriter;
		final BufferedOutputStream bufferedOutputStream;
		final String authHeaderPrefix = "Authorization: ";
		
		/***
		 * Instantiate IO streams related to the incoming socket connection. Throw if the connection is from a hostile IP.
		 * @param clientSocket
		 * @throws IOException
		 * @throws HostileIP
		 */
		public ClientSocketListener(Socket clientSocket) throws IOException, HostileIP {
			this.clientSocket = clientSocket;
			if(rateLimiter.IsIPHostile(this.clientSocket)) {
				throw new HostileIP(this.clientSocket.getInetAddress().getHostAddress());
			}
			this.inputStreamReader = new InputStreamReader(clientSocket.getInputStream());
			this.bufferedReader = new BufferedReader(this.inputStreamReader);
			this.printWriter = new PrintWriter(this.clientSocket.getOutputStream());
			this.bufferedOutputStream = new BufferedOutputStream(this.clientSocket.getOutputStream());
		}
		
		/***
		 * Close the streams and print a message if they fail to close
		 * @param closureMessage
		 */
		private void closeStreams(String closureMessage) {
			try {
				this.bufferedReader.close();
				this.inputStreamReader.close();
				this.printWriter.close();
				this.bufferedOutputStream.close();
				this.clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
				if(verbose) {
					System.out.println("Failed to close a client socket connection");
				}
			}
			if(verbose) {
				System.out.println(closureMessage);
			}
		}
		
		/***
		 * Responds to the incoming request, passing it through a rate limiter.
		 */
		@Override
		public void run() {
			String clientIP = this.clientSocket.getInetAddress().getHostAddress();
			// If we are rate limiting by IP, check if the connection's IP is rate limited and serve a 429 if it is.
			if(rateLimiter.IsIPRateLimited(clientIP)) {
				rateLimiter.ServeHttp429PerIP(printWriter,clientIP);
				closeStreams("Found rate limited IP attempt from "+clientIP);
				System.out.println();
			} else {
				String closureMessage = "";
				try {
					// We must read the tokens and headers before rate limiting by User authorization
					String[] tokens = ReadTokens(this.bufferedReader);
					String[] headers = ReadHeaders(this.bufferedReader);
					String auth = "";
					for(String header : headers) {
						if(header.startsWith(authHeaderPrefix)) {
							auth = header.substring(authHeaderPrefix.length());
							break;
						}
					}
					if(auth.isEmpty() && rateLimiter.RateLimitsByUser()) {
						// We first need to check if the rate limiter will expect an auth, in case it's empty!
						rateLimiter.ServeHttp401PerUser(printWriter);
						closureMessage = "No user auth included, but we rate limit by user, so send a 401!";
					} else if(!auth.isEmpty() && !rateLimiter.UserAuthIsValid(auth)) {	
						rateLimiter.ServeHttp403PerUser(printWriter);
						closureMessage = "User auth included, but is invalid, so send a 403!";
					} else if(rateLimiter.IsUserRateLimited(auth)) {
						// If we are rate limiting by User's auth, check if the auth token is rate limited and serve a 429 if it is.
						rateLimiter.ServeHttp429PerUser(printWriter,auth);
						closureMessage = "Found rate limited User "+auth;
					} else if(rateLimiter.IsIdentityRateLimitedToEndPoint(clientIP, auth, tokens[0], tokens[1])) {
						// If we are Rate limiting the Endpoint/Resource, check both the IP and User auth, and serve the appropriate 429
						rateLimiter.ServeHttp429PerEndpoint(printWriter, clientIP, auth, tokens[0], tokens[1]);
						closureMessage = "Found rate limited User "+auth+"; per resource "+tokens[1];
					} else {
						// If we have bypassed the IP, User, and Resource rate limiting, do the stuff!
						HandleRequest(tokens, headers, this.bufferedReader, this.printWriter, this.bufferedOutputStream);
						closureMessage = "Serviced the request from IP "+clientIP+"; User "+auth+"; Resource "+tokens[1];
					}
				} catch (IOException e) {
					e.printStackTrace();
					if(verbose) {
						System.out.println("Failed to interact with the socket's streams, closing connection to port "+this.clientSocket.getLocalPort()+" from "+this.clientSocket.getInetAddress().getHostAddress());
					}
				} finally {
					closeStreams(closureMessage);
					System.out.println();
				}
			}
		}
		
		/***
		 * We expect the first line of the incoming connection to be three tokens; Reads the Method, Resource and Protocol
		 * @param reader
		 * @return
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
		 * @return
		 * @throws IOException
		 */
		private String[] ReadHeaders(BufferedReader reader) throws IOException {
			ArrayList<String> headers = new ArrayList<String>();
			String line = reader.readLine(); 
			while (!line.isEmpty()) {
				headers.add(line);
				line = reader.readLine();
			}
			String[] retHeaders = new String[headers.size()];
			return headers.toArray(retHeaders);
		}
		
	}
	
	/***
	 * Custom exception to be thrown if the incoming connection's IP is tagged hostile by the rate limiter
	 * @author NathanLevett
	 *
	 */
	private class HostileIP extends Exception {
		
		private static final long serialVersionUID = -2057815366947861017L;
		final private String IP;
		
		public HostileIP(String IP) {
			this.IP = IP;
		}
		
		public String getIP() {
			return this.IP;
		}
		
	}
	
	/***
	 * Handle a request that has passed the rate limiter. Expected to serve an http response before returning.
	 * @param tokens
	 * @param headers
	 * @param reader
	 * @param printWriter
	 * @param bufferedOutputStream
	 * @throws IOException
	 */
	private void HandleRequest(String[] tokens, String[] headers, BufferedReader reader, PrintWriter printWriter, BufferedOutputStream bufferedOutputStream) throws IOException {
		System.out.println("Method: "+tokens[0]);
		System.out.println("Resource: "+tokens[1]);
		System.out.println("Protocol: "+tokens[2]);
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
		ArrayList<ResponsePart> response = RouteRequest(tokens[0], tokens[1], headers, content);
		sendResponseToClient(printWriter,bufferedOutputStream,response);
		System.out.println();
	}
	
	/***
	 * Wrapper for routing the content and headers to the appropriate method for the requested method|resource
	 * @param method
	 * @param resource
	 * @param headers
	 * @param content
	 * @return
	 */
	private ArrayList<ResponsePart> RouteRequest(String method, String resource, String[] headers, ArrayList<String> content) {
		ArrayList<ResponsePart> returnMessage = new ArrayList<ResponsePart>();
		returnMessage.add(new ResponsePart("HTTP/1.1 200 Woo"));
		returnMessage.add(new ResponsePart(""));
		return returnMessage;
	}
	
	/***
	 * A class to control the flushing of strings and byte streams to the output streams.
	 * @author NathanLevett
	 *
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
	 * Sends an ArrayList<ResponsePart> through a PrintWriter and BufferedOutputStream to the client
	 * @param printWriter
	 * @param bufferedOutputStream
	 * @param response
	 * @throws IOException
	 */
	private void sendResponseToClient(PrintWriter printWriter, BufferedOutputStream bufferedOutputStream, ArrayList<ResponsePart> response) throws IOException {
		boolean contextIsString = response.get(0).isString;
		boolean contextSwapped = false;
		for(ResponsePart respPart : response) {
			contextSwapped = ((contextIsString && !respPart.isString) || (!contextIsString && respPart.isString));
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
	
	public static void main(String args[] )  { 
		IDataStore dataStore = new DataStore();
		int maxRequests = 100;
		int maxSeconds = 3600;
		boolean OnlyAcceptStoredBasicAuth = false;
		IRateLimiter rateLimiter = new RateLimiter(dataStore,maxRequests,maxSeconds,OnlyAcceptStoredBasicAuth);
		if(OnlyAcceptStoredBasicAuth) {
			//This is now the only user that the rate limiter will accept the auth of.
			rateLimiter.StoreNewBasicAuth("Garry","1234");
		}
		//Start the server with verbose messaging
		Server server = new Server(true,rateLimiter);
		server.AddServerSocket(8085);
	}
	
}
