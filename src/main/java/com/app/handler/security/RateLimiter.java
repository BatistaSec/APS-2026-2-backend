package com.app.handler.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RateLimiter {

    private static final int DEFAULT_LIMIT = 60; // 60 requisições por minuto por IP
    private static final long WINDOW_MS = 60_000L;

    private final int maxRequests;
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> requestCounts = new ConcurrentHashMap<>();

    public RateLimiter() {
        this(DEFAULT_LIMIT);
    }

    public RateLimiter(int maxRequests) {
        this.maxRequests = maxRequests;
    }

    /**
     * Tenta adquirir um slot para o IP. O lock é feito por fila individual de cada IP,
     * evitando o gargalo de sincronizar o método inteiro (problemático com Virtual Threads).
     */
    public boolean tryAcquire(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = "UNKNOWN";
        }

        long now = System.currentTimeMillis();
        ConcurrentLinkedQueue<Long> timestamps = requestCounts.computeIfAbsent(clientIp, k -> new ConcurrentLinkedQueue<>());

        // Limpeza de entradas expiradas fora do lock é segura: só remove elementos antigos
        while (!timestamps.isEmpty() && (now - timestamps.peek()) > WINDOW_MS) {
            timestamps.poll();
        }

        // Sincroniza apenas na fila deste IP (scope mínimo)
        synchronized (timestamps) {
            // Re-verifica após lock para consistência
            while (!timestamps.isEmpty() && (now - timestamps.peek()) > WINDOW_MS) {
                timestamps.poll();
            }
            if (timestamps.size() < maxRequests) {
                timestamps.add(now);
                return true;
            }
        }

        return false;
    }

    public int getRemaining(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = "UNKNOWN";
        }
        ConcurrentLinkedQueue<Long> timestamps = requestCounts.get(clientIp);
        if (timestamps == null) return maxRequests;

        long now = System.currentTimeMillis();
        long validCount = timestamps.stream().filter(t -> (now - t) <= WINDOW_MS).count();
        return (int) Math.max(0, maxRequests - validCount);
    }

    public long getResetSeconds(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = "UNKNOWN";
        }
        ConcurrentLinkedQueue<Long> timestamps = requestCounts.get(clientIp);
        if (timestamps == null || timestamps.isEmpty()) {
            return 60;
        }
        Long oldest = timestamps.peek();
        if (oldest == null) return 60;
        long elapsed = System.currentTimeMillis() - oldest;
        long remainingMs = Math.max(0, WINDOW_MS - elapsed);
        return Math.max(1, remainingMs / 1000);
    }

    public int getMaxRequests() {
        return maxRequests;
    }
}
