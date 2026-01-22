package com.fileconverter.job;

import com.fileconverter.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class JobQueue {

    private static final Logger log = LoggerFactory.getLogger(JobQueue.class);

    private final BlockingQueue<Job> pendingJobs;
    private final ConcurrentHashMap<String, Job> allJobs;
    private final int capacity;

    public JobQueue(AppConfig config) {
        this.capacity = config.getWorker().getQueueCapacity();
        this.pendingJobs = new LinkedBlockingQueue<>(capacity);
        this.allJobs = new ConcurrentHashMap<>();
    }

    public boolean submit(Job job) {
        if (pendingJobs.offer(job)) {
            allJobs.put(job.getId(), job);
            log.info("Job {} queued. Queue size: {}", job.getId(), pendingJobs.size());
            return true;
        }
        log.warn("Queue full. Rejected job {}", job.getId());
        return false;
    }

    public Job take() throws InterruptedException {
        return pendingJobs.take();
    }

    public Optional<Job> poll() {
        return Optional.ofNullable(pendingJobs.poll());
    }

    public Optional<Job> getJob(String jobId) {
        return Optional.ofNullable(allJobs.get(jobId));
    }

    public void removeJob(String jobId) {
        Job job = allJobs.remove(jobId);
        if (job != null) {
            pendingJobs.remove(job);
            log.debug("Removed job {}", jobId);
        }
    }

    public int getPendingCount() {
        return pendingJobs.size();
    }

    public int getTotalCount() {
        return allJobs.size();
    }

    public int getCapacity() {
        return capacity;
    }

    public void cleanupExpired() {
        allJobs.values().stream()
                .filter(Job::isExpired)
                .forEach(job -> {
                    job.expire();
                    removeJob(job.getId());
                    log.info("Expired job {} removed", job.getId());
                });
    }
}
