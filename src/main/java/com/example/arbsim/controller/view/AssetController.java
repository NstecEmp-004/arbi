package com.example.arbsim.controller.view;

import com.example.arbsim.entity.Asset;
import com.example.arbsim.entity.Market;
import com.example.arbsim.repo.AssetRepository;
import com.example.arbsim.repo.MarketRepository;
import com.example.arbsim.service.MarketState;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class AssetController {
    private final AssetRepository assetRepo;
    private final MarketRepository marketRepo;
    private final MarketState state;

    public AssetController(AssetRepository assetRepo, MarketRepository marketRepo, MarketState state) {
        this.assetRepo = assetRepo;
        this.marketRepo = marketRepo;
        this.state = state;
    }

    @GetMapping("/assets")
    public String list(Model model) {
        model.addAttribute("assets", assetRepo.findAll());
        return "assets";
    }

    @GetMapping("/assets/{symbol}")
    public String show(@PathVariable String symbol, Model model) {
        Asset a = assetRepo.findBySymbol(symbol).orElseThrow();
        List<Market> markets = marketRepo.findAll();
        Map<Long, Map<Long, MarketState.PricePoint>> snap = state.snapshot();
        model.addAttribute("asset", a);
        model.addAttribute("markets", markets);
        model.addAttribute("snap", snap);
        return "asset";
    }
}
