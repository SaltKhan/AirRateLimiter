package MockServer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/***
 * The ServerBase class encapsulates the handling of ServerSocketListeners away
 * from the Server class, serving as an abstract base class designed in a 1-1
 * correspondence with the server class, purely to abstract away code unrelated
 * to the actual handling of the rate limiting functionality, but still used
 * in the server class implementation, whose "ClientSocketListener" inner class
 * encapsulates the rate limiting tie in / utilisation.
 */
public abstract class ServerBase {

	///////////////////////////////////////////////////////////////////////////
	//                             Constants                                 //
	///////////////////////////////////////////////////////////////////////////
	
	/***
	 * Retry count creating and setting a timeout on new ServerSockets
	 */
	static final private int retryCount = 3;

	/***
	 * Set all ServerSockets to have a constant timeout
	 */
	static final private int ServerSocketTimeoutSeconds = 20;

	///////////////////////////////////////////////////////////////////////////
	//                             Verbosity                                 //
	///////////////////////////////////////////////////////////////////////////

	/***
	 * Turn on verbose messages!
	 */
	protected abstract void turnOnVerbose();

	/***
	 * Turn off verbose messages!
	 */
	protected abstract void turnOffVerbose();
	
	/***
	 * @return True, if verbose messaging is enabled, otherwise false.
	 */
	protected abstract boolean isVerboseMessagingEnabled();

	/***
	 * Prints a verbose message to out stream, if set to print them.
	 * @param verboseMessage
	 */
	protected final void printOutVerboseMessage(String verboseMessage) {
		printVerboseMessage(verboseMessage,System.out);
	}
	
	/***
	 * Prints a verbose message to err stream, if set to print them.
	 * @param verboseMessage
	 */
	protected final void printErrVerboseMessage(String verboseMessage) {
		printVerboseMessage(verboseMessage,System.err);
	}
	
	/***
	 * Prints a verbose message to a given stream, if set to print them.
	 * @param verboseMessage
	 */
	private void printVerboseMessage(String verboseMessage, PrintStream ps) {
		if (isVerboseMessagingEnabled()) {
			ps.println(verboseMessage);
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	//                             Sockets                                   //
	///////////////////////////////////////////////////////////////////////////
	
	/***
	 * Adds an entry to the "Integer" -> "ServerSocketListener" map of 
	 * currently open ServerSocketListener being used to listen for 
	 * client connections.
	 * @param port
	 * @param socketListener
	 */
	protected abstract void addMappedServerSocketListenerOpenOnPort(int port, ServerSocketListener socketListener);

	/***
	 * Gets an entry from the "Integer" -> "ServerSocketListener" map of 
	 * currently open ServerSocketListener being used to listen for 
	 * client connections.
	 * @param port
	 * @return The ServerSocketListener open on the specified port
	 */
	protected abstract ServerSocketListener getServerSocketListenerOpenOnPort(int port);

	/***
	 * Removes an entry from the "Integer" -> "ServerSocketListener" map of
	 * currently open ServerSocketListener being used to listen for 
	 * client connections.
	 * @param port
	 * @return The ServerSocketListener removed from the map on the 
	 * specified port
	 */
	protected abstract ServerSocketListener removeServerSocketListenerOpenOnPort(int port);

	/***
	 * Creates a new ClientSocketListener (inner class of the Server) and
	 * initiates it as a runnable. Although the bulk of the rate limiting
	 * functionality is contained in the server class and not in this base
	 * class, the instantiation of the ClientSocketListener inner class can
	 * cause a "Hostile IP" related exception (to cause an incoming connection
	 * from an IP listed as a hostile entity to be dropped before initiating).
	 * As such, the HostileIP exception is an inner class of this base class
	 * such that it can be caught in the runnable of the ServerSocketListener
	 * which handles the acceptance or declining of creating a new
	 * ClientSocketListener. 
	 * @param clientSocket
	 * @throws InterruptedIOException
	 * @throws IOException
	 * @throws HostileIP
	 * @throws NullPointerException
	 */
	protected abstract void startNewClientSocketListenerThread(Socket clientSocket) 
			throws InterruptedIOException, IOException, HostileIP;
	
	///////////////////////////////////////////////////////////////////////////
	//            Define the "Server Socket Listener" inner class            //
	///////////////////////////////////////////////////////////////////////////

	/***
	 * Handle the process of a server socket listening for client acceptance in
	 * a thread which manages the spawning of client socket threads, when an 
	 * incoming message opens a new socket.
	 */
	protected final class ServerSocketListener implements Runnable {
		
		/***
		 * The port that the ServerSocket is listening on
		 */
		private int port;
		
		/***
		 * The ServerSocket that this thread is dedicated to listening on.
		 */
		private final ServerSocket serverSocket;
		
		/***
		 * The tolerance threshold for how many IOExceptions will be accepted
		 * in the accepting of incoming connections and instantiations of the
		 * associated ClientSocketListeners before a ServeSocketListener will
		 * be automatically closed.
		 */
		private int socketFailurePolicy = 10;
		
		/***
		 * Initially false, this boolean is a flag to enable this listener
		 * class to be told to "stop listening" prior to the socket itself
		 * being closed external to the listener class.
		 */
		private boolean StoppingListening;

		/***
		 * Create a new listening thread dedicated to the passed ServerSocket.
		 * @param serverSocket
		 */
		public ServerSocketListener(ServerSocket serverSocket) {
			this.port = serverSocket.getLocalPort();
			this.serverSocket = serverSocket;
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
					Socket clientSocket = serverSocket.accept();
					printOutVerboseMessage("Incoming connection to port " + port + " from client at "
							+ clientSocket.getInetAddress().getHostAddress());
					// Create a new ClientSocketListener thread to handle 
					// the incoming connection
					startNewClientSocketListenerThread(clientSocket);
				} catch (InterruptedIOException e) {
					// Print a message if the timeout cycles
					printOutVerboseMessage("The socket " + port + " has timed out (" + ServerSocketTimeoutSeconds
							+ " seconds) waiting for input. Will continue to listen.");
				} catch (IOException e) {
					// If the "stop listening" is raised outside of this thread
					// then this bool indicates to print a verbose friendly
					// message to indicate as such, and break
					if (this.StoppingListening) {
						printOutVerboseMessage("IO Exception tripped on port " + port + "; Listener was told it was stopping!");
						break;
					}
					// The server socket is allowed the socketFailurePolicy 
					// many IOExceptions before the server socket's thread ends
					socketFailurePolicy--;
					if (socketFailurePolicy == 0) {
						printOutVerboseMessage("The socket " + port + " has failed too many times, will stop listening.");
						break;
					}
				} catch (HostileIP e) {
					// Custom exception to allow throwing on the client socket
					// creation if from a "hostile IP" as per the rate limiter
					printOutVerboseMessage("Found hostile IP " + "attempt from " + e.getIP());
				}
			}
			if(!this.StoppingListening) {
				// Only run the closing of the server socket if it was not
				// already closed by the "stop listening" command.
				CloseServerSocket(serverSocket);
			}
		}

		/***
		 * Allows the thread to be aware that the 
		 * socket was forced shut from outside.
		 */
		public void StopListening() {
			this.StoppingListening = true;
			CloseServerSocket(serverSocket);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//                Add a "Server Socket Listener" instance                //
	///////////////////////////////////////////////////////////////////////////

	/***
	 * Add a new server socket to the server instance, and start listening on
	 * that port
	 * @param port
	 */
	public void AddServerSocket(int port) {
		InternalAddServerSocket(port, 0);
	}

	/***
	 * Internal retry method to attempt to add a server socket and start a
	 * listener thread.
	 * @param port
	 * @param retries
	 */
	private void InternalAddServerSocket(int port, int retries) {
		ServerSocket socket = RetryInstantiateNewServerSocketWithTimeout(port, retries);
		if (socket != null) {
			printOutVerboseMessage("Listening for connection on port " + port);
			Thread listener = MakeNewThreadToListenToSocketOnPort(socket);
			listener.start();
		}
	}

	/***
	 * Chain invokes both retriable methods used to create the ServerSocket
	 * and assign it a specified timeout policy.
	 * @param port
	 * @param retries
	 * @return The ServerSocket open on the port, if it was possible to make,
	 * otherwise null.
	 */
	private ServerSocket RetryInstantiateNewServerSocketWithTimeout(int port, int retries) {
		return RetrySetServerSocketTimeout(RetryInstantiateNewServerSocket(port, retries), retries);
	}

	/***
	 * Attempts "retries" many times to instantiate a ServerSocket instance on
	 * "port." If the retry count is exceeded, returns null.
	 * @param port
	 * @param retries
	 * @return The ServerSocket open on the port, if it was possible to make,
	 * otherwise null.
	 */
	private ServerSocket RetryInstantiateNewServerSocket(int port, int retries) {
		try {
			ServerSocket socket = new ServerSocket(port);
			return socket;
		} catch (IOException e) {
			e.printStackTrace();
			// Retry up to the retryCount many times.
			if (retries < retryCount) {
				printErrVerboseMessage(
							"Failed to start listening for connection on port " + port + "; retry " + (retries + 1));
				return RetryInstantiateNewServerSocket(port, retries + 1);
			} else {
				printErrVerboseMessage("Failed to start listening for connection on port " + port
							+ "; Has reached the retry maximum of " + retryCount);
				return null;
			}
		}
	}

	/***
	 * Attempts "retries" many times to set the socket timeout for a ServerSocket
	 * instance. If the retry count is exceeded, returns null.
	 * @param socket
	 * @param retries
	 * @return The ServerSocket open on the port, if it was possible to assign 
	 * the timeout policy, otherwise null.
	 */
	private ServerSocket RetrySetServerSocketTimeout(ServerSocket socket, int retries) {
		if (socket == null) {
			return null;
		}
		try {
			socket.setSoTimeout(ServerSocketTimeoutSeconds * 1000);
			return socket;
		} catch (SocketException e) {
			e.printStackTrace();
			// Retry up to the retryCount many times.
			if (retries < retryCount) {
				printErrVerboseMessage("Failed to set the socket timeout for connection on port "
							+ socket.getLocalPort() + "; retry " + (retries + 1));
				return RetrySetServerSocketTimeout(socket, retries + 1);
			} else {
				printErrVerboseMessage("Failed to set the socket timeout for connection on port "
							+ socket.getLocalPort() + "; Has reached the retry maximum of " + retryCount);
				CloseServerSocket(socket);
				return null;
			}
		}
	}
	
	/***
	 * Creates the ServerSocketListener thread to listen on a socket, and
	 * maps to it from the socket's local port.
	 * @param socket
	 * @return The thread instance running the ServerSocketListener.
	 */
	private Thread MakeNewThreadToListenToSocketOnPort(ServerSocket socket) {
		ServerSocketListener socketListener = new ServerSocketListener(socket);
		addMappedServerSocketListenerOpenOnPort(socket.getLocalPort(), socketListener);
		return (new Thread(getServerSocketListenerOpenOnPort(socket.getLocalPort())));
	}

	///////////////////////////////////////////////////////////////////////////
	//                          Close a Server Socket                        //
	///////////////////////////////////////////////////////////////////////////

	/***
	 * Close an open/existing ServerSocketListener
	 * @param port
	 */
	public void CloseServerSocketListener(int port) {
		// The "Stop Listening" triggers the "CloseServerSocket" internally
		getServerSocketListenerOpenOnPort(port).StopListening();
		removeServerSocketListenerOpenOnPort(port);
	}
	
	/***
	 * Close an open/existing ServerSocket
	 * @param port
	 */
	private void CloseServerSocket(ServerSocket socket) {
		InternalCloseServerSocket(socket, 0);
	}

	/***
	 * Internal retry method to close and remove the server socket and listener
	 * for a specified port!
	 * @param port
	 * @param retries
	 */
	private void InternalCloseServerSocket(ServerSocket socket, int retries) {
		int port = socket.getLocalPort();
		try {
			socket.close();
			printOutVerboseMessage("Stopped listening for connection " + "on port " + port);
		} catch (IOException e) {
			e.printStackTrace();
			String message = "Failed to stop listening for connection on port " + port + "; ";
			// Retry up to the retryCount many times.
			if (retries < retryCount) {
				printOutVerboseMessage(message + "Retry #" + (retries + 1));
				InternalCloseServerSocket(socket, retries + 1);
			} else {
				printOutVerboseMessage(message + "Has reached the retry maximum of " + retryCount);
			}
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	//                         Hostile IP inner class                        //
	///////////////////////////////////////////////////////////////////////////
	
	/***
	 * Custom exception to be thrown if the incoming connection's 
	 * IP is tagged hostile by the rate limiter
	 */
	@SuppressWarnings("serial")
	protected final class HostileIP extends Exception {
		
		/***
		 * The IP that is regarded as hostile
		 */
		final private String IP;
		
		/***
		 * Create a new Hostile IP exception
		 * @param IP
		 */
		public HostileIP(String IP) {
			this.IP = IP;
		}
		
		/***
		 * Get the IP that triggered the Hostile IP exception.
		 * @return
		 */
		public String getIP() {
			return this.IP;
		}
		
	}

}
