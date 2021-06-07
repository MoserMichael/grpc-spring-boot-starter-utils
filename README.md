# utility class for  grpc-spring-boot-starter

This small project adds a grpc ServerInterceptor that does several things;

- logs the request, response messages and http headers of a grpc request with log4j at info log level. By default all grpc method calls are traced, however you can optionally limit the set of traced  classes and methods by means of an optional configuration file that looks like [this](https://github.com/MoserMichael/grpc-spring-boot-starter-utils/blob/50d971d9af2e8da823631bc396725a56095ffe5e/grpc-spring-utils/src/main/resources/logdef.yml) To activate this configuration file, set spring property spring-utils.log-definition to the log file name, the file must be in the resource path.
- catch unhandled exceptions and pass an exception back to the client that includes the cause of the server exception and it's stack trace. This makes it a bit easier to track problems by looking at the error that is available at the client side.
 

# dependencies

When working with spring, one is often challenged to find the correct set of dependencies, luckily we have the grpc-spring-boot-starter [dependency matrix](https://github.com/LogNet/grpc-spring-boot-starter/blob/master/ReleaseNotes.md) in this case.

Also nowadays one should use [maven central](https://search.maven.org/) repository, jcentral has become read only (see [announcement](https://developer.android.com/studio/build/jcenter-migration) )
