package edu.pku.migrationhelper.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xuyul on 2020/2/15.
 */
@Configuration
@ConfigurationProperties(prefix = "migration-helper.thread-pool")
public class ThreadPoolConfiguration {

    Logger LOG = LoggerFactory.getLogger(getClass());

    private int threadCount = 1;

    @Bean
    public ExecutorService getExecutorService() {
        LOG.info("Thread pool thread count = {}", threadCount);
        return Executors.newFixedThreadPool(threadCount);
    }

    public int getThreadCount() {
        return threadCount;
    }

    public ThreadPoolConfiguration setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }
}
