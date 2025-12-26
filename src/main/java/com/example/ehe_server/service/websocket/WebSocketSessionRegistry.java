package com.example.ehe_server.service.websocket;

import com.example.ehe_server.service.intf.websocket.WebSocketSessionRegistryInterface;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketSessionRegistry implements WebSocketSessionRegistryInterface {

    // sessionId → Set of cleanup callbacks
    private final Map<String, Set<Runnable>> sessionCleanupCallbacks = new ConcurrentHashMap<>();

    /**
     * Register a cleanup callback for a session
     * @param sessionId The WebSocket session ID
     * @param cleanup The cleanup logic to run when session disconnects
     */
    public void registerSessionCleanup(String sessionId, Runnable cleanup) {
        sessionCleanupCallbacks
                .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(cleanup);
    }

    /**
     * Called when a session disconnects - runs all cleanup callbacks
     */
    public void cleanupSession(String sessionId) {
        Set<Runnable> callbacks = sessionCleanupCallbacks.remove(sessionId);

        if (callbacks != null && !callbacks.isEmpty()) {
            System.out.println("[SessionRegistry] Running " + callbacks.size() + " cleanup callbacks for session: " + sessionId);

            callbacks.forEach(callback -> {
                try {
                    callback.run();
                } catch (Exception e) {
                    System.err.println("[SessionRegistry] Error in cleanup callback: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            System.out.println("[SessionRegistry] Session cleanup complete: " + sessionId);
        }
    }
}