package MockServerTest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import MockServer.Client;
import MockServer.Server;
import RateLimiterService.AbstractRateLimiter;
import RateLimiterService.RateLimiter;

class ServerTest {

	@Test
	void test() throws IOException {
		//Make a rate limiter which doesn't demand validated users, but will demand auth
		//Allow 5 per 10 seconds
		int maxAttempts = 5;
		int maxSeconds = 2;
		AbstractRateLimiter rateLimiter = new RateLimiter(maxAttempts,maxSeconds,false);
		Server server = new Server(true,rateLimiter);
		server.AddServerSocket(8085);
		Client client = new Client("localhost",8085,"GET","GG/M8","SuchUser","VeryPassword");
		String[] responses = new String[maxAttempts+1];
		for(int k = 0; k <= maxAttempts; k++) {
			responses[k] = client.SubmitRequest();
		}
		for(int k = 0; k < maxAttempts; k++) {
			assertTrue(responses[k].split("`n")[0].contains("200"));
		}
		assertTrue(responses[maxAttempts].split("`n")[0].contains("429"));
		//And a second user
		client = new Client("localhost",8085,"GET","GG/M8","SuchSecondUser","VeryMuchPassword");
		for(int k = 0; k <= maxAttempts; k++) {
			responses[k] = client.SubmitRequest();
		}
		for(int k = 0; k < maxAttempts; k++) {
			assertTrue(responses[k].split("`n")[0].contains("200"));
		}
		assertTrue(responses[maxAttempts].split("`n")[0].contains("429"));
		server.CloseServerSocketListener(8085);
	}

}
