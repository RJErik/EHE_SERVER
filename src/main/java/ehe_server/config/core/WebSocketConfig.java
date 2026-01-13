package ehe_server.config.core;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Create TaskScheduler for heartbeat
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("websocket-heartbeat-thread-");
        taskScheduler.initialize();

        // Enable simple broker with destination prefixes and set the TaskScheduler directly
        registry.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(taskScheduler)
                .setHeartbeatValue(new long[] {10000, 10000});

        // Prefix for endpoints clients send messages to
        registry.setApplicationDestinationPrefixes("/app");

        // Enable user-specific destinations
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // Allow the same origins as your CORS config
                .setAllowedOrigins("http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost")
                // Enable SockJS fallback
                .withSockJS();
    }
}
