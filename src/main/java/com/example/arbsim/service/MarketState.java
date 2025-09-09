package com.example.arbsim.service;

import com.example.arbsim.entity.Asset;
import com.example.arbsim.entity.Market;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class MarketState {
    public static record PricePoint(Long marketId, Long assetId, BigDecimal price, Instant ts) {
    }

    private final Map<Long, Map<Long, PricePoint>> last = new HashMap<>();
    private Instant lastTick = Instant.EPOCH;

    public synchronized void put(Market m, Asset a, BigDecimal price, Instant ts) {
        last.computeIfAbsent(m.getId(), k -> new HashMap<>()).put(a.getId(),
                new PricePoint(m.getId(), a.getId(), price, ts));
        lastTick = ts;
    }

    public synchronized Map<Long, Map<Long, PricePoint>> snapshot() {
        Map<Long, Map<Long, PricePoint>> copy = new HashMap<>();
        for (var e : last.entrySet())
            copy.put(e.getKey(), new HashMap<>(e.getValue()));
        return copy;
    }

    public synchronized Instant lastTick() {
        return lastTick;
    }
}
