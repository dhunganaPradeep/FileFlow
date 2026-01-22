package com.fileconverter.converter;

import com.fileconverter.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class ProcessExecutor {

    private static final Logger log = LoggerFactory.getLogger(ProcessExecutor.class);

    private final int timeoutSeconds;

    public ProcessExecutor(AppConfig config) {
        this.timeoutSeconds = config.getWorker().getProcessTimeoutSeconds();
    }

    public int execute(List<String> command, Path workDir,
            Consumer<String> outputHandler) throws Exception {

        log.debug("Executing: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);

        // Security: restrict environment
        pb.environment().clear();
        pb.environment().put("PATH", "/usr/bin:/bin:/usr/local/bin");
        pb.environment().put("HOME", "/tmp");
        pb.environment().put("LANG", "en_US.UTF-8");

        Process process = pb.start();

        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (outputHandler != null) {
                        outputHandler.accept(line);
                    }
                    log.trace("Process output: {}", line);
                }
            } catch (Exception e) {
                log.warn("Error reading process output", e);
            }
        });
        outputThread.start();

        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("Process timed out after " + timeoutSeconds + " seconds");
        }

        outputThread.join(5000);

        int exitCode = process.exitValue();
        log.debug("Process exited with code: {}", exitCode);

        return exitCode;
    }

    public boolean isToolAvailable(String toolName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", toolName);
            Process process = pb.start();
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
