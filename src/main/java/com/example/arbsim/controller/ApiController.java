package com.example.arbsim.controller;

import com.example.arbsim.entity.*;
import com.example.arbsim.repo.*;
import com.example.arbsim.service.MarketState;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;

@RestController
public class ApiController {
    private final MarketRepository marketRepo;
    private final AssetRepository assetRepo;
    private final TradeRepository tradeRepo;
    private final MarketState state;
    public ApiController(MarketRepository marketRepo, AssetRepository assetRepo, TradeRepository tradeRepo, MarketState state) {
        this.marketRepo = marketRepo; this.assetRepo = assetRepo; this.tradeRepo = tradeRepo; this.state = state;
    }

    @GetMapping("/api/state")
    public Map<String,Object> state() {
        Map<String,Object> res = new LinkedHashMap<>();
        res.put("lastTick", state.lastTick().toString());
        List<Market> markets = marketRepo.findAll();
        List<Asset> assets = assetRepo.findAll();
        Map<Long, Map<Long, MarketState.PricePoint>> snap = state.snapshot();
        List<Map<String,Object>> table = new ArrayList<>();
        for (Market m : markets) {
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("market", m.getName());
            Map<Long, MarketState.PricePoint> mp = snap.getOrDefault(m.getId(), Map.of());
            for (Asset a : assets) row.put(a.getSymbol(), mp.get(a.getId()) != null ? mp.get(a.getId()).price() : null);
            table.add(row);
        }
        res.put("prices", table);

        List<Trade> trades = tradeRepo.findTop20ByOrderByTsDesc();
        List<Map<String,Object>> trs = new ArrayList<>();
        for (Trade t : trades) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("asset", t.getAsset().getSymbol());
            m.put("buyMarket", t.getBuyMarket().getName());
            m.put("sellMarket", t.getSellMarket().getName());
            m.put("qty", t.getQuantity()); m.put("buyPrice", t.getBuyPrice()); m.put("sellPrice", t.getSellPrice());
            m.put("profit", t.getProfit()); m.put("ts", t.getTs().toString());
            trs.add(m);
        }
        res.put("trades", trs);
        return res;
    }
}
