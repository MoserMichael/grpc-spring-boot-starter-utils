package springutils.grpc.interceptors.logcfg;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import springutils.grpc.interceptors.LogCallInterceptor;

import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogCallConfiguration {

    public static final int LOG_ARGUMENTS = 2;
    public static final int LOG_HEADERS = 4;
    public static final int LOG_RETURN_VALUES = 8;
    public static final int LOG_DEFAULT_MASK =  LOG_ARGUMENTS | LOG_RETURN_VALUES | LOG_HEADERS;

    private static final Logger log = LoggerFactory.getLogger(LogCallInterceptor.class);

    private static class LogEntryCfg {

            LogEntryCfg(int mask) {
                this.logMask = mask;
            }

            int logMask;
            Map<String, LogEntryCfg> nextLevel;

            void addNextLevel(String name, LogEntryCfg cfg) {
                if (nextLevel == null) {
                    nextLevel = new HashMap<String, LogEntryCfg>();
                }
                nextLevel.put(name, cfg);
            }
    };

    private static class CfgMain {
        @JsonProperty("default-mask")
        private Integer defaultMask;
        @JsonProperty("services")
        private List<CfgService> services;

    }

    private static class CfgService {
        @JsonProperty("mask")
        private int mask;

        @JsonProperty("class")
        private String name;

        @JsonProperty("methods")
        private List<CfgMethod> methods;
    }

    private static class CfgMethod {
        @JsonProperty("method")
        private String name;

        @JsonProperty("mask")
        private int mask;

    }

    private LogEntryCfg cfgRoot;
    private int defaultLogMask = LOG_DEFAULT_MASK;

    public LogCallConfiguration(Environment env) {

        var logDefinitionFile = env.getProperty("spring-utils.log-definition");
        if (logDefinitionFile == null) {
            return;
        }
        parseLogDefinitionFile(logDefinitionFile);
        dumpLogDefinitions();
    }

    private void dumpLogDefinitions() {
        log.info("defaultLogMask: {}", defaultLogMask);
        logCfg(cfgRoot,0);
    }

    private void logCfg(LogEntryCfg logEntry, int nesting) {
        var prefix = new String(" ").repeat(nesting);
        if (logEntry.nextLevel != null) {
            logEntry.nextLevel.entrySet().stream().forEach(
                    entry -> {
                        log.info("{}{} {} mask: {}", prefix, (nesting == 0 ? "service:" : "method:"), entry.getKey(), entry.getValue().logMask);
                        logCfg(entry.getValue(), nesting + 1);
                    });
        }
    }


    private void parseLogDefinitionFile(String logDefinitionFile) {

        try {
            var jsondatastream = getClass().getClassLoader().getResourceAsStream(logDefinitionFile);

            var mapper = new ObjectMapper(new YAMLFactory());
            var order = mapper.readValue(jsondatastream, CfgMain.class);

            if (order.defaultMask != null) {
                defaultLogMask = order.defaultMask;
            }
            cfgRoot = new LogEntryCfg(defaultLogMask);

            if (order.services != null){
                order.services.stream().forEach(
                        service -> {
                            int mask = defaultLogMask;

                            var cfgClass = new LogEntryCfg(mask);
                            cfgRoot.addNextLevel(service.name, cfgClass);

                            service.methods.stream().forEach(
                                    methodEntry -> {
                                        cfgClass.addNextLevel( methodEntry.name,
                                                new LogEntryCfg(methodEntry.mask));
                                    });
                        }
                );
            }

        } catch(Throwable ex) {
            String msg = "Can't load configuration file " + logDefinitionFile;
            log.error(msg, ex);
            throw new RuntimeException(msg, ex);
        }
    }



    public int findLogSettings(String serviceName, String methodName) {
        if (cfgRoot == null ) {
            return this.defaultLogMask;
        }

        int curLogMask = cfgRoot.logMask;

        var cfgObj = cfgRoot.nextLevel.get(serviceName);
        if (cfgObj != null) {
            curLogMask = cfgObj.logMask;
            if (cfgObj.nextLevel != null) {
                cfgObj = cfgObj.nextLevel.get(methodName);
                if (cfgObj != null) {
                    curLogMask = cfgObj.logMask;
                }
            }
        }
        return curLogMask;
    }
}
