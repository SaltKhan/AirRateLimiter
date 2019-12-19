package MockServer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/***
 * A conglomerate class of the IO streams associated with a socket.
 */
public class SocketedIOConglomerate {
	
	/***
	 * The socket opened to handle the outgoing/incoming client request
	 */
	private Socket clientSocket;
	
	/***
	 * The socket address of the first instance of the clientSocket, used
	 * to enable a "refresh" of the clientSocket after each closing.
	 */
	private final InetSocketAddress socketAddress;
	
	/***
	 * Output stream associated with the socket to read from the connection.
	 */
	protected InputStreamReader inputStreamReader;
	
	/***
	 * Output stream associated with the socket to read from the connection.
	 */
	protected BufferedReader bufferedReader;
	
	/***
	 * Input stream associated with the socket to write text to the connection.
	 */
	protected PrintWriter printWriter;
	
	/***
	 * Input stream associated with the socket to write data to the connection.
	 */
	protected BufferedOutputStream bufferedOutputStream;
	
	/***
	 * Instantiate with reference to the socket for single connection.
	 * @param clientSocket
	 * @throws IOException
	 * @throws HostileIP
	 */
	protected SocketedIOConglomerate(Socket clientSocket) throws IOException {
		this.clientSocket = clientSocket;
		this.socketAddress = null;
	}
	
	/***
	 * Instantiate with reference to the host and port for reconnection;
	 * The "host"/"domain" of the URL that the client will be connecting to,
	 * the port of the URL that the client will be connecting to.
	 * @param targetHost
	 * @param targetPort
	 * @throws IOException
	 */
	protected SocketedIOConglomerate(String targetHost, int targetPort) throws IOException {
		this.clientSocket = new Socket();
		this.socketAddress = new InetSocketAddress(targetHost, targetPort);
	}
	
	/***
	 * @return The inner socket
	 */
	final protected Socket getSocket() {
		return clientSocket;
	}
	
	/***
	 * @return The host address used by the socket
	 */
	final protected String getSocketHostAddress() {
		return clientSocket.getInetAddress().getHostAddress();
	}
	
	/***
	 * @return The port used by the socket
	 */
	final protected int getSocketPort() {
		return clientSocket.getLocalPort();
	}
	
	/***
	 * Open streams associated with the socket.
	 * @throws IOException
	 */
	final protected void openStreams() throws IOException {
		InputStream is = clientSocket.getInputStream();
		this.inputStreamReader = new InputStreamReader(is);
		this.bufferedReader = new BufferedReader(inputStreamReader);
		OutputStream os = clientSocket.getOutputStream();
		this.printWriter = new PrintWriter(os);
		this.bufferedOutputStream = new BufferedOutputStream(os);
	}
	
	/***
	 * Reconnect the socket to the previous address.
	 * @throws IOException
	 */
	final protected void reconnectSocket() throws IOException {
		if(!this.clientSocket.isClosed()) {
			closeSocket();
		}
		this.clientSocket = new Socket();
		this.clientSocket.connect(this.socketAddress);
	}
	
	/***
	 * Close the streams.
	 * @throws IOException
	 */
	final protected void closeStreams() throws IOException {
		this.bufferedReader.close();
		this.inputStreamReader.close();
		this.printWriter.close();
		this.bufferedOutputStream.close();
	}
	
	/***
	 * Close the socket.
	 * @throws IOException
	 */
	final protected void closeSocket() throws IOException {
		this.clientSocket.close();
	}
	
}
