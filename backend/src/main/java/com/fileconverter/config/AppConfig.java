package com.fileconverter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private Storage storage = new Storage();
    private Security security = new Security();
    private Worker worker = new Worker();

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public static class Storage {
        private String tempDir;
        private long maxFileSize = 524288000L;
        private int ttlMinutes = 10;

        public String getTempDir() {
            return tempDir;
        }

        public void setTempDir(String tempDir) {
            this.tempDir = tempDir;
        }

        public long getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(long maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        public int getTtlMinutes() {
            return ttlMinutes;
        }

        public void setTtlMinutes(int ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }
    }

    public static class Security {
        private String tokenSecret;
        private int tokenTtlMinutes = 30;
        private RateLimit rateLimit = new RateLimit();

        public String getTokenSecret() {
            return tokenSecret;
        }

        public void setTokenSecret(String tokenSecret) {
            this.tokenSecret = tokenSecret;
        }

        public int getTokenTtlMinutes() {
            return tokenTtlMinutes;
        }

        public void setTokenTtlMinutes(int tokenTtlMinutes) {
            this.tokenTtlMinutes = tokenTtlMinutes;
        }

        public RateLimit getRateLimit() {
            return rateLimit;
        }

        public void setRateLimit(RateLimit rateLimit) {
            this.rateLimit = rateLimit;
        }

        public static class RateLimit {
            private int requestsPerMinute = 30;
            private int burstCapacity = 10;

            public int getRequestsPerMinute() {
                return requestsPerMinute;
            }

            public void setRequestsPerMinute(int requestsPerMinute) {
                this.requestsPerMinute = requestsPerMinute;
            }

            public int getBurstCapacity() {
                return burstCapacity;
            }

            public void setBurstCapacity(int burstCapacity) {
                this.burstCapacity = burstCapacity;
            }
        }
    }

    public static class Worker {
        private int poolSize = 4;
        private int queueCapacity = 100;
        private int processTimeoutSeconds = 300;

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public int getProcessTimeoutSeconds() {
            return processTimeoutSeconds;
        }

        public void setProcessTimeoutSeconds(int processTimeoutSeconds) {
            this.processTimeoutSeconds = processTimeoutSeconds;
        }
    }
}
