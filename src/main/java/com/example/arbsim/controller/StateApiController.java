package com.example.arbsim.controller;

import com.example.arbsim.entity.Asset;
import com.example.arbsim.entity.Market;
import com.example.arbsim.repo.AssetRepository;
import com.example.arbsim.repo.MarketRepository;
import com.example.arbsim.service.MarketState;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class StateApiController {

    private final MarketRepository marketRepo;
    private final AssetRepository assetRepo;
    private final MarketState state;

    public StateApiController(MarketRepository marketRepo, AssetRepository assetRepo, MarketState state) {
        this.marketRepo = marketRepo;
        this.assetRepo = assetRepo;
        this.state = state;
    }

    /**
     * UI用のマトリクス: { table:[{market:'Market 1',A:...,B:...,C:...},...],
     * lastTick:'...' }
     */
    @GetMapping("/api/state/table")
    public Map<String, Object> table() {
        List<Market> markets = marketRepo.findAll();
        List<Asset> assets = assetRepo.findAll();

        // 最新価格のスナップショット（IDベース）
        Map<Long, Map<Long, com.example.arbsim.service.MarketState.PricePoint>> snap = state.snapshot();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Market m : markets) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("market", m.getName());
            Map<Long, com.example.arbsim.service.MarketState.PricePoint> line = snap.getOrDefault(m.getId(),
                    Collections.emptyMap());
            for (Asset a : assets) {
                var pp = line.get(a.getId());
                if (pp != null && pp.price() != null) {
                    row.put(a.getSymbol(), pp.price());
                }
            }
            rows.add(row);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("table", rows);
        res.put("lastTick", state.lastTick().toString());
        return res;
    }
}
