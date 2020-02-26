package com.example.specialagentdemo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
public class SpecialagentDemoApplication {
    private static final Logger log = LoggerFactory.getLogger(SpecialagentDemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpecialagentDemoApplication.class, args);
        attachSpecialAgent();
    }

    /**
     * The Java property denoting the Java home directory.
     */
    private static final String JAVA_HOME = "java.home";

    /**
     * The Java property denoting the operating system name.
     */
    private static final String OS_NAME = "os.name";
    /**
     * The status code expected as a result of a successful attachment.
     */
    private static final int SUCCESSFUL_ATTACH = 0;


    public static void attachSpecialAgent() {
        log.info("attaching");
        try {
            File agentJar = ResourceUtils.getFile("classpath:opentracing-specialagent-1.5.8.jar");
            String thisPid = ByteBuddyAgent.ProcessProvider.ForCurrentVm.INSTANCE.resolve();

            int status = new ProcessBuilder(System.getProperty(JAVA_HOME)
                    + File.separatorChar + "bin"
                    + File.separatorChar
                    + (System.getProperty(OS_NAME, "").toLowerCase(Locale.US).contains("windows") ? "java.exe"
                            : "java"),
                    "-Dsa.init.defer=false",
                    "-Dsa.tracer=mock",
                    "-jar",
                    agentJar.getAbsolutePath(),
                    thisPid).start().waitFor();
            if (status != SUCCESSFUL_ATTACH) {
                log.error("external attach process failed, exit status {}", status);
            }

            log.info("done attaching");
        } catch (FileNotFoundException e) {
            log.warn("no agent to attach");
        } catch (IOException e) {
            log.error("external attach process failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @RestController
    public static class MyController {
        private final RestTemplate restTemplate = new RestTemplate();

        @Value("${server.port}")
        private int port;

        @GetMapping("/api/recurse/{depth}")
        public String recurse(@PathVariable int depth, @RequestHeader Map<String, String> headers) {
            log.info("headers at depth {} are {} ", depth, headers);
// if you uncomment this, dynamic attach will break the application.
//            Span activeSpan = GlobalTracer.get().scopeManager().activeSpan();
//            if (activeSpan != null && !activeSpan.context().toSpanId().isEmpty()) {
//                log.info("*** in a span at depth {}! spanid {}***", depth, activeSpan.context().toSpanId());
//            } else {
//                log.info("not in span at depth {}", depth);
//            }
            if (depth > 0) {
                return restTemplate.getForObject("http://localhost:{port}/api/recurse/{depth}", String.class, port,
                        depth - 1);
            }
            return "base case";
        }

        @Scheduled(fixedRate = 5000)
        public void startRecursion() {
            recurse(10, null);
        }
    }
}
