# AirRateLimiter
AirTasker Rate Limiting Challenge
## The "Server" class
The Server class designates on object to be injected with an instance of the IRateLimiter interface, which itself is injected with an instance of the IDataStore interface. The IDataStore is meant to shadow the equivalent of an external data store, here, mocked in in-memory. The IRateLimiter exposes methods for the Server class to utilise to query the data store for relevant data.
## The IRateLimiter interface
The IRateLimiter exposes several different ways of storing "who" has accessed "what." It can be used to rate limit incoming requests based on IP, an HTTP Authorization header, or either of these rated against "what server resource" they are trying to access.
## The "Client" class
The client class is a mock'd client instantiating a socketed connection to the same port that the server class will start listening on, and send a simple Http request, with an Authorisation. Short of being able to "fake an incoming IP on a socket", enforcing the incoming connection to serve an Authorization header is how the RateLimiter distinguishes between identities, although it can be instantiated to only inspect IP and disregard the Http Authorization header.
## Have a go at it;
Beyond the obvious test cases sampled here which only test the *requested functionality* and not any of the *future requested functionality stubs*, short of readibility of the results of the server test (testing the returned 429 after hitting the rate limit conditions), spin up an instance of the Server, and submit messages from the client. For more reasonable "hits per time", consider lowering these in the Server.main(...)
