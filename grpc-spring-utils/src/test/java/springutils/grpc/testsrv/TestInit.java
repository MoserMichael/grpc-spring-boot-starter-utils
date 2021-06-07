package springutils.grpc.testsrv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("springutils.grpc")
public class TestInit {

    public static void main(String[] args) {
        SpringApplication.run(TestInit.class, args);
    }


}