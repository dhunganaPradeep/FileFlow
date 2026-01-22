package com.fileconverter.job;

import com.fileconverter.config.AppConfig;
import com.fileconverter.converter.ConverterRegistry;
import com.fileconverter.converter.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class WorkerPool {

    private static final Logger log = LoggerFactory.getLogger(WorkerPool.class);

    private final JobQueue jobQueue;
    private final ConverterRegistry converterRegistry;
    private final int poolSize;
    private final AtomicBoolean running;
    private ExecutorService executor;

    public WorkerPool(JobQueue jobQueue, ConverterRegistry converterRegistry, AppConfig config) {
        this.jobQueue = jobQueue;
        this.converterRegistry = converterRegistry;
        this.poolSize = config.getWorker().getPoolSize();
        this.running = new AtomicBoolean(false);
    }

    @PostConstruct
    public void start() {
        running.set(true);
        executor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "converter-worker");
            t.setDaemon(true);
            return t;
        });

        for (int i = 0; i < poolSize; i++) {
            executor.submit(this::workerLoop);
        }
        log.info("Worker pool started with {} workers", poolSize);
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("Worker pool stopped");
    }

    private void workerLoop() {
        while (running.get()) {
            try {
                Job job = jobQueue.take();
                processJob(job);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Worker error", e);
            }
        }
    }

    private void processJob(Job job) {
        log.info("Processing job {}: {} -> {}",
                job.getId(), job.getSourceMimeType(), job.getTargetFormat());

        try {
            job.startProcessing();

            Converter converter = converterRegistry.getConverter(
                    job.getSourceMimeType(), job.getTargetFormat());

            if (converter == null) {
                job.fail("No converter available for this format combination");
                return;
            }

            job.updateProgress(20);

            converter.convert(
                    job.getInputPath(),
                    job.getOutputPath(),
                    job.getTargetFormat(),
                    progress -> job.updateProgress(20 + (int) (progress * 0.7)));

            job.complete();
            log.info("Job {} completed successfully", job.getId());

        } catch (Exception e) {
            log.error("Job {} failed: {}", job.getId(), e.getMessage());
            job.fail(e.getMessage());
        }
    }
}
