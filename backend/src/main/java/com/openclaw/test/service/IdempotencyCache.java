package com.openclaw.test.service;

import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等缓存组件，用于防止重复请求。
 * 使用 ConcurrentHashMap 存储，5 分钟自动过期清理。
 */
@Component
@Schema(description = "幂等缓存，5 分钟自动过期")
public class IdempotencyCache {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCache.class);
    private static final long EXPIRE_MS = 5 * 60 * 1000L; // 5 分钟

    /**
     * 特殊标记值，表示 key 对应的请求正在处理中。
     */
    public static final Object PROCESSING_MARKER = new Object();

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * 获取缓存的值。如果 key 不存在或已过期返回 null。
     */
    @Schema(description = "获取缓存值")
    public Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        // 检查是否过期
        if (System.currentTimeMillis() - entry.createdAt > EXPIRE_MS) {
            cache.remove(key);
            return null;
        }
        return entry.value;
    }

    /**
     * 判断 key 是否存在且未过期。
     */
    @Schema(description = "判断 key 是否存在")
    public boolean exists(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() - entry.createdAt > EXPIRE_MS) {
            cache.remove(key);
            return false;
        }
        return true;
    }

    /**
     * 存入缓存并设置 5 分钟自动过期。
     */
    @Schema(description = "存入缓存（5 分钟自动过期）")
    public void put(String key, Object value) {
        CacheEntry entry = new CacheEntry(value, System.currentTimeMillis());
        cache.put(key, entry);

        // 5 分钟后清理（使用 CompletableFuture 延迟执行）
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(EXPIRE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 仅当条目未被更新时删除
            cache.remove(key, entry);
            log.debug("Idempotency cache expired for key: {}", key);
        });
    }

    /**
     * 移除指定 key 的缓存。
     */
    public void remove(String key) {
        cache.remove(key);
    }

    /**
     * 放入处理中标记。返回 true 如果成功设置（之前不存在），
     * 返回 false 表示已有条目存在（重复请求）。
     */
    public boolean startProcessing(String key) {
        CacheEntry existing = cache.putIfAbsent(key, new CacheEntry(PROCESSING_MARKER, System.currentTimeMillis()));
        if (existing != null) {
            // 检查是否过期
            if (System.currentTimeMillis() - existing.createdAt > EXPIRE_MS) {
                // 过期了，就算未完成也可覆盖
                cache.put(key, new CacheEntry(PROCESSING_MARKER, System.currentTimeMillis()));
                return true;
            }
            return false;
        }
        return true;
    }

    private record CacheEntry(Object value, long createdAt) {}
}
