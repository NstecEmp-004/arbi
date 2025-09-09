package com.example.arbsim.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** マーケット×銘柄ごとの出来高上限（1秒ごとに0〜1000） */
@Service
public class VolumeService {
    // marketId -> (assetId -> volume)
    private final Map<Long, Map<Long, Integer>> volumes = new ConcurrentHashMap<>();

    /** 1秒ごとに全組み合わせの出来高をリセット（0〜1000ランダム） */
    public void regenerate(Collection<Long> marketIds, Collection<Long> assetIds) {
        Map<Long, Map<Long, Integer>> next = new ConcurrentHashMap<>();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (Long m : marketIds) {
            Map<Long, Integer> inner = new ConcurrentHashMap<>();
            for (Long a : assetIds) {
                inner.put(a, rnd.nextInt(0, 1001));
            }
            next.put(m, inner);
        }
        volumes.clear();
        volumes.putAll(next);
    }

    /** 指定量だけ消費。実際に消費できた量（0以上）を返す */
    public synchronized int consume(Long marketId, Long assetId, int want) {
        if (want <= 0) return 0;
        Map<Long,Integer> m = volumes.get(marketId);
        if (m == null) return 0;
        Integer cur = m.get(assetId);
        if (cur == null || cur <= 0) return 0;
        int use = Math.min(cur, want);
        m.put(assetId, cur - use);
        return use;
    }

    public int available(Long marketId, Long assetId) {
        Map<Long,Integer> m = volumes.get(marketId);
        if (m == null) return 0;
        return m.getOrDefault(assetId, 0);
    }

    /** デバッグ／表示用スナップショット */
    public Map<Long, Map<Long, Integer>> snapshot() {
        Map<Long, Map<Long, Integer>> copy = new HashMap<>();
        volumes.forEach((mid, inner) -> {
            copy.put(mid, new HashMap<>(inner));
        });
        return copy;
    }
}
