package com.example.arbsim.controller.view;

import com.example.arbsim.entity.Market;
import com.example.arbsim.entity.Asset;
import com.example.arbsim.repo.MarketRepository;
import com.example.arbsim.repo.AssetRepository;
import com.example.arbsim.service.MarketState;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class MarketController {
    private final MarketRepository marketRepo;
    private final AssetRepository assetRepo;
    private final MarketState state;

    public MarketController(MarketRepository marketRepo, AssetRepository assetRepo, MarketState state) {
        this.marketRepo = marketRepo;
        this.assetRepo = assetRepo;
        this.state = state;
    }

    @GetMapping("/markets")
    public String list(Model model) {
        model.addAttribute("markets", marketRepo.findAll());
        return "markets";
    }

    @GetMapping("/markets/{id}")
    public String show(@PathVariable Long id, Model model) {
        Market m = marketRepo.findById(id).orElseThrow();
        List<Asset> assets = assetRepo.findAll();
        Map<Long, Map<Long, MarketState.PricePoint>> snap = state.snapshot();
        model.addAttribute("market", m);
        model.addAttribute("assets", assets);
        model.addAttribute("snap", snap);
        return "market";
    }
}
