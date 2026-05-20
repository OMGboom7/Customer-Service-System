package com.openclaw.test.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages active SSE streams, allowing abort/stop of in-progress streams.
 */
public class StreamManager {
    private static final Logger log = LoggerFactory.getLogger(StreamManager.class);

    private static final ConcurrentMap<String, SseEmitter> ACTIVE_EMITTERS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, HttpURLConnection> ACTIVE_CONNECTIONS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, CompletableFuture<?>> ACTIVE_FUTURES = new ConcurrentHashMap<>();

    /**
     * Register an SseEmitter for a stream key.
     */
    public static void registerEmitter(String key, SseEmitter emitter) {
        ACTIVE_EMITTERS.put(key, emitter);
        log.info("Stream registered: {}", key);
    }

    /**
     * Register an HttpURLConnection for a stream key.
     */
    public static void registerConnection(String key, HttpURLConnection conn) {
        ACTIVE_CONNECTIONS.put(key, conn);
    }

    /**
     * Register a CompletableFuture (for cancellation) for a stream key.
     */
    public static void registerFuture(String key, CompletableFuture<?> future) {
        ACTIVE_FUTURES.put(key, future);
    }

    /**
     * Unregister all resources for a stream key.
     */
    public static void unregister(String key) {
        ACTIVE_EMITTERS.remove(key);
        ACTIVE_CONNECTIONS.remove(key);
        ACTIVE_FUTURES.remove(key);
        log.info("Stream unregistered: {}", key);
    }

    /**
     * Abort a stream by key: complete the emitter, disconnect the connection, cancel the future.
     * @return true if any resource was found and acted upon
     */
    public static boolean abort(String key) {
        SseEmitter emitter = ACTIVE_EMITTERS.remove(key);
        HttpURLConnection conn = ACTIVE_CONNECTIONS.remove(key);
        CompletableFuture<?> future = ACTIVE_FUTURES.remove(key);

        if (emitter != null) {
            try {
                emitter.complete();
                log.info("Aborted - emitter completed: {}", key);
            } catch (Exception e) {
                log.warn("Error completing emitter for {}: {}", key, e.getMessage());
            }
        }
        if (conn != null) {
            try {
                conn.disconnect();
                log.info("Aborted - connection disconnected: {}", key);
            } catch (Exception e) {
                log.warn("Error disconnecting connection for {}: {}", key, e.getMessage());
            }
        }
        if (future != null) {
            try {
                future.cancel(true);
                log.info("Aborted - future cancelled: {}", key);
            } catch (Exception e) {
                log.warn("Error cancelling future for {}: {}", key, e.getMessage());
            }
        }

        boolean found = emitter != null || conn != null || future != null;
        if (!found) {
            log.warn("Abort called but no active stream found: {}", key);
        }
        return found;
    }

    /**
     * Check if a stream key is currently active.
     */
    public static boolean isActive(String key) {
        return ACTIVE_EMITTERS.containsKey(key) || ACTIVE_CONNECTIONS.containsKey(key);
    }

    /**
     * Get the emitter for a key (for checking active streams).
     */
    public static SseEmitter getEmitter(String key) {
        return ACTIVE_EMITTERS.get(key);
    }

    /**
     * Atomically replace all resources for a stream key.
     * Disconnects old connection and cancels old future, then registers new ones.
     */
    public static void replace(String key, SseEmitter newEmitter, HttpURLConnection newConn, CompletableFuture<?> newFuture) {
        // 原子替换: 断开旧的 + 完成旧emitter + 注册新的
        SseEmitter oldEmitter = ACTIVE_EMITTERS.remove(key);
        HttpURLConnection oldConn = ACTIVE_CONNECTIONS.remove(key);
        CompletableFuture<?> oldFuture = ACTIVE_FUTURES.remove(key);
        if (oldEmitter != null) {
            try { oldEmitter.complete(); } catch (Exception e) { /* ignore */ }
        }
        if (oldConn != null) oldConn.disconnect();
        if (oldFuture != null) oldFuture.cancel(true);
        ACTIVE_EMITTERS.put(key, newEmitter);
        if (newConn != null) ACTIVE_CONNECTIONS.put(key, newConn);
        if (newFuture != null) ACTIVE_FUTURES.put(key, newFuture);
        log.info("Stream replaced: {} (completed old emitter + disconnected old connection)", key);
    }
}
