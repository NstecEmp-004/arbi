package com.example.arbsim.service;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class AssetWindowService {
    private final Map<String, Instant> startMap = new ConcurrentHashMap<>();
    public Instant getWindowStart(String symbol) {
        return startMap.getOrDefault(symbol, Instant.EPOCH);
    }
    public Instant resetWindow(String symbol) {
        Instant now = Instant.now();
        startMap.put(symbol, now);
        return now;
    }
}