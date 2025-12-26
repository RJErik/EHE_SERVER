package com.example.ehe_server.service.intf.websocket;

/**
 * Interface for managing WebSocket session lifecycle and cleanup callbacks.
 * Provides a centralized registry for coordinating cleanup operations when sessions disconnect.
 */
public interface WebSocketSessionRegistryInterface {

    /**
     * Register a cleanup callback for a WebSocket session.
     * The callback will be executed when the session disconnects.
     * Multiple callbacks can be registered for the same session.
     *
     * @param sessionId The WebSocket session ID
     * @param cleanup The cleanup logic to run when session disconnects
     */
    void registerSessionCleanup(String sessionId, Runnable cleanup);

    /**
     * Execute all registered cleanup callbacks for a session and remove them from the registry.
     * Called when a WebSocket session disconnects.
     * Each callback is executed independently - if one fails, others continue.
     *
     * @param sessionId The WebSocket session ID that has disconnected
     */
    void cleanupSession(String sessionId);
}