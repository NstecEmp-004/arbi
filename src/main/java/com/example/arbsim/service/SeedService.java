package com.example.arbsim.service;

import com.example.arbsim.config.SimulationConfig;
import com.example.arbsim.entity.*;
import com.example.arbsim.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class SeedService implements CommandLineRunner {
    private final SimulationConfig cfg;
    private final MarketRepository marketRepo;
    private final AssetRepository assetRepo;
    private final PriceTickRepository priceRepo;
    private final MarketState state;

    public SeedService(SimulationConfig cfg, MarketRepository marketRepo, AssetRepository assetRepo, PriceTickRepository priceRepo, MarketState state) {
        this.cfg = cfg; this.marketRepo = marketRepo; this.assetRepo = assetRepo; this.priceRepo = priceRepo; this.state = state;
    }

    @Override public void run(String... args) {
        if (marketRepo.count() == 0) {
            for (int i=1;i<=cfg.getMarkets();i++) marketRepo.save(new Market("MKT"+i, "Market "+i));
        }
        if (assetRepo.count() == 0) {
            Map<String, BigDecimal> init = cfg.getInitialPrice();
            if (init == null) throw new IllegalStateException("sim.initialPrice を設定してください (A/B/C の初期価格)");
            for (String sym : cfg.getAssets()) {
                BigDecimal p = init.get(sym);
                if (p == null) throw new IllegalStateException("初期価格が未設定: " + sym);
                assetRepo.save(new Asset(sym, sym+" Stock", p));
            }
        }

        List<Market> markets = marketRepo.findAll();
        List<Asset> assets = assetRepo.findAll();
        Instant now = Instant.now();
        for (Market m : markets) for (Asset a : assets) {
            priceRepo.save(new PriceTick(m, a, a.getBasePrice(), now));
            state.put(m, a, a.getBasePrice(), now);
        }
    }
}
