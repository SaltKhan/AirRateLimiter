package MockServer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public abstract class ServerBase {

	/***
	 * 
	 */
	static final private int retryPolicy = 3;

	/***
	 * 
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
	
	protected abstract boolean isVerboseMessagingEnabled();

	/***
	 * Prints a verbose message, if set to print them.
	 * 
	 * @param verboseMessage
	 */
	protected final void printOutVerboseMessage(String verboseMessage) {
		if (isVerboseMessagingEnabled()) {
			System.out.println(verboseMessage);
		}
	}
	
	/***
	 * Prints a verbose message to err, if set to print them.
	 * 
	 * @param verboseMessage
	 */
	protected final void printErrVerboseMessage(String verboseMessage) {
		if (isVerboseMessagingEnabled()) {
			System.err.println(verboseMessage);
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	//                             Sockets                                   //
	///////////////////////////////////////////////////////////////////////////

	protected abstract void addMappedServerSocketOpenOnPort(int port, ServerSocket socket);
	
	protected abstract ServerSocket getServerSocketOpenOnPort(int port);
	
	protected abstract ServerSocket removeServerSocketOpenOnPort(int port);
	
	protected abstract boolean isServerSocketMappedOnPort(int port);

	protected abstract void addMappedServerSocketListenerOpenOnPort(int port, ServerSocketListener socketListener);
	
	protected abstract ServerSocketListener getServerSocketListenerOpenOnPort(int port);
	
	protected abstract ServerSocketListener removeServerSocketListenerOpenOnPort(int port);
	
	protected abstract void startNewClientSOcketListenerThread(Socket clientSocket) throws InterruptedIOException, IOException, HostileIP, NullPointerException;
	
	///////////////////////////////////////////////////////////////////////////
	//            Define the "Server Socket Listener" inner class            //
	///////////////////////////////////////////////////////////////////////////

	/***
	 * Handle the process of a server socket listening for client acceptance in a
	 * thread which manages the spawning of client socket threads, when an incoming
	 * message opens a new socket.
	 */
	protected final class ServerSocketListener implements Runnable {

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
					Socket clientSocket = getServerSocketOpenOnPort(port).accept();
					printOutVerboseMessage("Incoming connection to port " + port + " from "
							+ clientSocket.getInetAddress().getHostAddress());
					// Start a new thread to handle the incoming connection
					startNewClientSOcketListenerThread(clientSocket);
				} catch (InterruptedIOException e) {
					// Print a message if the timeout cycles
					printOutVerboseMessage("The socket " + port + " has timed out (" + ServerSocketTimeoutSeconds
							+ " seconds) waiting for input. Will continue to listen.");
				} catch (IOException e) {
					e.printStackTrace();
					// The server socket is allowed the socketFailurePolicy 
					// many IOExceptions before the server socket's thread ends
					socketFailurePolicy--;
					if (socketFailurePolicy == 0) {
						printOutVerboseMessage(
								"The socket " + port + " has failed too many times, will stop listening.");
						break;
					}
				} catch (HostileIP e) {
					// Custom exception to allow throwing the client socket
					// if from a "hostile IP" as per the rate limiter
					printOutVerboseMessage("Found hostile IP " + "attempt from " + e.getIP());
					e.printStackTrace();
				} catch (NullPointerException e) {
					if (this.StoppingListening) {
						printOutVerboseMessage(
								"Null pointer tripped on port " + port + "; Listener was told it was stopping!");
						break;
					} else {
						printOutVerboseMessage(
								"Null pointer tripped on port " + port + "; Listener was not told to stop.");
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

	///////////////////////////////////////////////////////////////////////////
	//                Add a "Server Socket Listener" instance                //
	///////////////////////////////////////////////////////////////////////////

	/***
	 * Add a new server socket to the server instance, and start listening on that
	 * port
	 * 
	 * @param port
	 */
	public void AddServerSocket(int port) {
		InternalAddServerSocket(port, 0);
	}

	/***
	 * Internal retry method to attempt to add a server socket and start a listener
	 * thread.
	 * 
	 * @param port
	 * @param retries
	 */
	private void InternalAddServerSocket(int port, int retries) {
		if (MakeANewServerSocketWithTimeout(port, retries)) {
			Thread listener = MakeNewThreadToListenToSocketOnPort(port);
			listener.start();
		}
	}

	private boolean MakeANewServerSocketWithTimeout(int port, int retries) {
		ServerSocket socket = RetryInstantiateNewServerSocketWithTimeout(port, retries);
		if (socket == null) {
			return false;
		} else {
			addMappedServerSocketOpenOnPort(port, socket);
			printOutVerboseMessage("Listening for connection on port " + port);
			return true;
		}
	}

	/***
	 * Chain invokes both retriable
	 * 
	 * @param port
	 * @param retries
	 * @return
	 */
	private ServerSocket RetryInstantiateNewServerSocketWithTimeout(int port, int retries) {
		return RetrySetServerSocketTimeout(RetryInstantiateNewServerSocket(port, retries), retries);
	}

	/***
	 * Attempts "retries" many times to instantiate a ServerSocket instance on
	 * "port." If the retry count is exceeded, returns null.
	 * 
	 * @param port
	 * @param retries
	 * @return
	 */
	private ServerSocket RetryInstantiateNewServerSocket(int port, int retries) {
		try {
			ServerSocket socket = new ServerSocket(port);
			return socket;
		} catch (IOException e) {
			e.printStackTrace();
			// Retry up to the retryPolicy many times.
			if (retries < retryPolicy) {
				printErrVerboseMessage(
							"Failed to start listening for connection on port " + port + "; retry " + (retries + 1));
				return RetryInstantiateNewServerSocket(port, retries + 1);
			} else {
				printErrVerboseMessage("Failed to start listening for connection on port " + port
							+ "; Has reached the retry maximum of " + retryPolicy);
				return null;
			}
		}
	}

	/***
	 * Attempts "retries" many times to set the socket timeout for a ServerSocket
	 * instance. If the retry count is exceeded, returns null.
	 * 
	 * @param socket
	 * @param retries
	 * @return
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
			// Retry up to the retryPolicy many times.
			if (retries < retryPolicy) {
				printErrVerboseMessage("Failed to set the socket timeout for connection on port "
							+ socket.getLocalPort() + "; retry " + (retries + 1));
				return RetrySetServerSocketTimeout(socket, retries + 1);
			} else {
				printErrVerboseMessage("Failed to set the socket timeout for connection on port "
							+ socket.getLocalPort() + "; Has reached the retry maximum of " + retryPolicy);
				return null;
			}
		}
	}

	private Thread MakeNewThreadToListenToSocketOnPort(int port) {
		ServerSocketListener socketListener = new ServerSocketListener(port);
		addMappedServerSocketListenerOpenOnPort(port, socketListener);
		return (new Thread(getServerSocketListenerOpenOnPort(port)));
	}

	///////////////////////////////////////////////////////////////////////////
	//                          Close a Server Socket                        //
	///////////////////////////////////////////////////////////////////////////

	/***
	 * Close an open/existing server socket
	 * 
	 * @param port
	 */
	public void CloseServerSocket(int port) {
		InternalCloseServerSocket(port, 0);
	}

	/***
	 * Internal retry method to close and remove the server socket and listener for
	 * a specified port!
	 * 
	 * @param port
	 * @param retries
	 */
	private void InternalCloseServerSocket(int port, int retries) {
		if (isServerSocketMappedOnPort(port)) {
			try {
				// Close and remove the socket and the thread 
				// listening on that socket
				getServerSocketListenerOpenOnPort(port).StopListening();
				removeServerSocketListenerOpenOnPort(port);
				getServerSocketOpenOnPort(port).close();
				removeServerSocketOpenOnPort(port);
				printOutVerboseMessage("Stopped listening for connection " + "on port " + port);
			} catch (IOException e) {
				e.printStackTrace();
				// Retry up to the retryPolicy many times.
				if (retries < retryPolicy) {
					printOutVerboseMessage("Failed to stop listening for " + "connection on port " + port + "; retry "
								+ (retries + 1));
					InternalCloseServerSocket(port, retries + 1);
				} else {
					printOutVerboseMessage("Failed to stop listening for " + "connection on port " + port + "; Has reached "
							+ "the retry maximum of " + retryPolicy);
				}
			}
		} else {
			printOutVerboseMessage("Failed to stop listening for connection " + "on port " + port
						+ "; Not currently listening to this port!");
		}
	}
	
	/***
	 * Custom exception to be thrown if the incoming connection's 
	 * IP is tagged hostile by the rate limiter
	 * @author NathanLevett
	 *
	 */
	protected final class HostileIP extends Exception {
		
		private static final long serialVersionUID = -2057815366947861017L;
		final private String IP;
		
		public HostileIP(String IP) {
			this.IP = IP;
		}
		
		public String getIP() {
			return this.IP;
		}
		
	}

}
