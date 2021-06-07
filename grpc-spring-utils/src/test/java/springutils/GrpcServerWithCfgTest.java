package springutils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import springutils.grpc.interceptors.LogCallInterceptor;
import springutils.grpc.test.pb.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import springutils.grpc.testsrv.GetTimeServiceGrpcImpl;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = springutils.grpc.testsrv.TestInit.class)
@ActiveProfiles(profiles = {"testcfg"})
@DirtiesContext
@SpringBootTest(properties = {
        "grpc.port=9091"
})
public class GrpcServerWithCfgTest {

    private int timeSrvPort = 9091;
    private ManagedChannel chan;
    private GetTimeServiceGrpc.GetTimeServiceBlockingStub stub;

    private static final Logger logger = LoggerFactory.getLogger(LogCallInterceptor.class);

    private void initTest() {

        chan = ManagedChannelBuilder
                .forAddress("localhost", timeSrvPort)
                .usePlaintext()
                .build();
        stub = GetTimeServiceGrpc.newBlockingStub(chan);
    }

    private void shutdownTest() {
        try {
            chan.shutdownNow().awaitTermination(10, TimeUnit.SECONDS);
        } catch(InterruptedException ex) {
        }
    }

    @Test
    public void testLocalTime() {
        initTest();
        var resp = stub.getLocalTime(LocalTimeRequest.newBuilder().setFormat("yyyy-MM-dd 'at' hh:mm a ").build());
        logger.info("getlocalTime response: {}", resp);
        shutdownTest();
    }

    @Test
    public void testZonedTime() {
        initTest();

        var resp = stub.getTimeWithZone(
                TimeWithZoneRequest.newBuilder().
                        setFormat("yyyy-MM-dd 'at' hh:mm a ").
                        setTimeZone("GMT").
                        build());
        logger.info("getTimeWithZone response: {}", resp);

        shutdownTest();
    }
}
