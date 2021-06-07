# utility class for  grpc-spring-boot-starter

This small project adds a grpc ServerInterceptor that does several things;

- logs the request, response messages and http headers of a grpc request with log4j By default all grpc method calls are traced, but you can select the traces classes and methods by means of an optional configuration file.
- catch unhandled exceptions and pass an exception back to the client that includes the cause of the server exception and it's stack trace. This makes it a bit easier to track problems by looking at the error that is available at the client side.
 


