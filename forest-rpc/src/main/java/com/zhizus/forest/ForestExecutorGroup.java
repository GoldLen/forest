package com.zhizus.forest;

import com.zhizus.forest.common.Constants;
import com.zhizus.forest.support.StandardThreadExecutor;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Created by Dempe on 2016/12/7.
 */
public class ForestExecutorGroup implements Closeable {

    private final static Logger LOGGER = LoggerFactory.getLogger(ForestExecutorGroup.class);

    private static Map<String, ExecutorService> executorsMap = Maps.newConcurrentMap();

    private ServerConfig config;

    public ForestExecutorGroup(ServerConfig config, Set<String> groupSet, ApplicationContext context) {
        this.config = config;
        for (String group : groupSet) {
            ExecutorService executor = executorsMap.get(group);
            Object bean = null;
            if (executor == null) {
                try {
                    bean = context.getBean(group);
                } catch (Exception e) {
                    LOGGER.info("no executor {} in spring container.", group);
                }
                if (bean != null && bean instanceof ExecutorService) {
                    executor = (ExecutorService) bean;
                } else {
                    executor = new StandardThreadExecutor(config.coreThread(), config.maxThreads());
                }
            }
            executorsMap.put(group, executor);
        }
    }


    public void execute(String group, Runnable command) {
        Executor executor = executorsMap.get(group);
        if (executor == null) {
            LOGGER.warn("group:{} is null, use {} as default.", group, Constants.DEF_GROUP);
            executor = executorsMap.get(Constants.DEF_GROUP);
        }
        executor.execute(command);
    }

    @Override
    public void close() throws IOException {
        for (Map.Entry<String, ExecutorService> stringExecutorEntry : executorsMap.entrySet()) {
            stringExecutorEntry.getValue().shutdown();
        }
    }


}
