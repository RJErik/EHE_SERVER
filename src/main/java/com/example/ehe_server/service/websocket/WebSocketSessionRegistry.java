package com.example.ehe_server.service.websocket;

import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.websocket.WebSocketSessionRegistryInterface;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketSessionRegistry implements WebSocketSessionRegistryInterface {
    private final LoggingServiceInterface loggingService;
    public WebSocketSessionRegistry(LoggingServiceInterface loggingService) {
        this.loggingService = loggingService;
    }

    private final Map<String, Set<Runnable>> sessionCleanupCallbacks = new ConcurrentHashMap<>();

    /**
     * Register a cleanup callback for a session
     * @param sessionId The WebSocket session ID
     * @param cleanup The cleanup logic to run when session disconnects
     */
    @Override
    public void registerSessionCleanup(String sessionId, Runnable cleanup) {
        sessionCleanupCallbacks
                .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(cleanup);
    }

    /**
     * Called when a session disconnects - runs all cleanup callbacks
     */
    @Override
    public void cleanupSession(String sessionId) {
        Set<Runnable> callbacks = sessionCleanupCallbacks.remove(sessionId);

        if (callbacks != null && !callbacks.isEmpty()) {
            callbacks.forEach(callback -> {
                try {
                    callback.run();
                } catch (Exception e) {
                    loggingService.logError("[SessionRegistry] Error in cleanup callback: " + e.getMessage(), e);
                }
            });

            loggingService.logAction("[SessionRegistry] Session cleanup complete: " + sessionId);
        } else {
            loggingService.logAction("[SessionRegistry] No cleanup callbacks registered for session: " + sessionId);
        }
    }
}