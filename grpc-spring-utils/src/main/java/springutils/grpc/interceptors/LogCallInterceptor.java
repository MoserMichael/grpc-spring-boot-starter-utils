package springutils.grpc.interceptors;

import com.google.protobuf.CodedOutputStream;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import springutils.grpc.interceptors.logcfg.LogCallConfiguration;
import org.springframework.core.env.Environment;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static io.grpc.Status.INTERNAL;

@Component
@Configuration
@GRpcGlobalInterceptor
public class LogCallInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LogCallInterceptor.class);

    private Environment env;
    private LogCallConfiguration cfg;

    LogCallInterceptor(@Autowired Environment env) {
        this.env = env;
        this.cfg = new LogCallConfiguration(env);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        /*
        log.info("call: serviceName: {} fullMethodName: {}",
                call.getMethodDescriptor().getServiceName(),
                call.getMethodDescriptor().getFullMethodName());
        */

        String serviceName = call.getMethodDescriptor().getServiceName();
        String fullMethodName = call.getMethodDescriptor().getFullMethodName();
        int idx = fullMethodName.indexOf('/');
        String methodName = fullMethodName.substring(idx + 1);

        int logSettings = cfg.findLogSettings(serviceName, methodName);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(
                        new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {

                            @Override
                            public void sendMessage(final RespT message) {

                                if ((logSettings & LogCallConfiguration.LOG_RETURN_VALUES) != 0) {
                                    log.info("grpc response message. service: {} method: {} response: {}", serviceName, methodName, message.toString());
                                }
                                super.sendMessage(message);
                            }
                        }, headers)) {

            @Override
            public void onMessage(ReqT message) {
                try {
                    if ((logSettings & LogCallConfiguration.LOG_HEADERS) != 0) {
                        log.info("grpc request header: {}", headers);
                    }
                    if ((logSettings & LogCallConfiguration.LOG_ARGUMENTS) != 0) {
                        log.info("grpc request message. service: {} method: {} request: {}", serviceName, methodName, message.toString());
                    }
                    super.onMessage(message);
                } catch (Throwable ex) {
                    handleEndpointException(ex, call);
                }
            }


            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (Throwable ex) {
                    handleEndpointException(ex, call);
                }
            }

            @Override
            public void onCancel() {
                try {
                    super.onCancel();
                } catch (Throwable ex) {
                    handleEndpointException(ex, call);
                }
            }

            @Override
            public void onComplete() {
                try {
                    super.onComplete();
                } catch (Throwable ex) {
                    handleEndpointException(ex, call);
                }
            }

            public void onReady() {
                try {
                    super.onReady();
                } catch (Throwable ex) {
                    handleEndpointException(ex, call);
                }
            }


            private <ReqT, RespT> void handleEndpointException(Throwable ex, ServerCall<ReqT, RespT> serverCall) {

                Metadata metadata = Status.trailersFromThrowable(ex);
                if (metadata == null) {
                    metadata = new Metadata();
                }

                var message = makeExceptionMessage(ex);

                if (ex instanceof StatusRuntimeException) {
                    StatusRuntimeException stex = (StatusRuntimeException) ex;
                    log.info("calling serverCall close");
                    serverCall.close(stex.getStatus().withDescription(message), metadata);

                } else {
                    log.info("calling serverCall close");
                    serverCall.close(Status.INTERNAL
                            .withCause(ex)
                            .withDescription(message), metadata);
                }
            }
        };
    }

    private String makeExceptionMessage(Throwable ex) {
        String stackTrace = null;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final String utf8 = StandardCharsets.UTF_8.name();
            PrintStream ps = new PrintStream(baos, true, utf8);
            ex.printStackTrace(ps);
            stackTrace = baos.toString(utf8);
        } catch (Throwable exx) {
            stackTrace = "can't get stack trace: " + exx;
        }
        return "server exception: " + ex + "\n" + stackTrace + "\t***eof server exception***\n";
    }
}