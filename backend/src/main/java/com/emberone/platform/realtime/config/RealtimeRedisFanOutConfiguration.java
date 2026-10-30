/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.config;

import com.emberone.platform.realtime.publish.RealtimeRedisFanOut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@ConditionalOnProperty(prefix = "emberone.realtime", name = "enabled", havingValue = "true")
public class RealtimeRedisFanOutConfiguration {

    @Bean
    TaskScheduler realtimeHeartbeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("realtime-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    RedisMessageListenerContainer realtimeRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RealtimeRedisFanOut realtimeRedisFanOut,
            @Value("${emberone.realtime.redis-channel:" + RealtimeRedisFanOut.CHANNEL + "}")
                    String channel) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                (message, pattern) -> realtimeRedisFanOut.onRedisMessage(message.getBody()),
                new ChannelTopic(channel));
        return container;
    }
}
