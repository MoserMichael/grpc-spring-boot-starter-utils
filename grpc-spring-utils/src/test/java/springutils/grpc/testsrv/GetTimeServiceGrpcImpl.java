package springutils.grpc.testsrv;

import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import springutils.grpc.test.pb.*;

import javax.management.RuntimeErrorException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.TimeZone;

@GRpcService
public class GetTimeServiceGrpcImpl extends GetTimeServiceGrpc.GetTimeServiceImplBase {

    private String currentTimeZone = "unnown";

    GetTimeServiceGrpcImpl() {
        currentTimeZone = getCurrentTimeZone();
    }

    private String getCurrentTimeZone() {
        Calendar cal = Calendar.getInstance();
        long milliDiff = cal.get(Calendar.ZONE_OFFSET);
        // Got local offset, now loop through available timezone id(s).
        String[] ids = TimeZone.getAvailableIDs();
        String name = null;
        for (String id : ids) {
            TimeZone tz = TimeZone.getTimeZone(id);
            if (tz.getRawOffset() == milliDiff) {
                // Found a match.
                name = id;
                break;
            }
        }
        return name;
    }

    @Override
    public void getLocalTime(LocalTimeRequest req, StreamObserver<LocalTimeResponse> responseObserver) {

        var format = "yyyy/MM/dd HH:mm:ss";
        if (req.getFormat() != null && req.getFormat().equals("") == false) {
            format = req.getFormat();
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
        LocalDateTime now = LocalDateTime.now();
        var strres = dtf.format(now);

        var resp = LocalTimeResponse.newBuilder().
                        setTime(strres).
                        setCurrentTimeZone(this.currentTimeZone).
                        build();

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void getTimeWithZone(TimeWithZoneRequest req, StreamObserver<TimeWithZoneResponse> responseObserver) {

        var format = "yyyy/MM/dd HH:mm:ss";
        if (req.getFormat() != null && req.getFormat().equals("") == false) {
            format = req.getFormat();
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
        LocalDateTime now = LocalDateTime.now(ZoneId.of(req.getTimeZone()));
        var strres = dtf.format(now);

        var resp = TimeWithZoneResponse.newBuilder().
                        setTime(strres).
                        setTimeZone(req.getTimeZone()).
                        build();

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void throwRuntimeException(com.google.protobuf.Empty request, StreamObserver<com.google.protobuf.Empty> response) {
        throw new RuntimeException("runtime exception");
    }
}
