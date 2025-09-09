package com.example.arbsim.controller;

import com.example.arbsim.entity.Trade;
import com.example.arbsim.repo.TradeRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.Duration;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.*;

@Controller
class AnalyticsPageController {
    @GetMapping("/analytics")
    public String analytics() { return "analytics"; }
}
@RestController
class AnalyticsApiController {
    private final TradeRepository tradeRepo;
    public AnalyticsApiController(TradeRepository tradeRepo) { this.tradeRepo = tradeRepo; }

    @GetMapping("/api/metrics")
    public Map<String,Object> metrics(
            @RequestParam(value="period", required=false) String period,
            @RequestParam(value="from", required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value="to", required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        Instant now = Instant.now();
        if ("today".equalsIgnoreCase(period)) {
            ZoneId tz = ZoneId.of("Asia/Tokyo");
            LocalDate today = LocalDate.now(tz);
            from = today.atStartOfDay(tz).toInstant();
            to = now;
        } else {
            if (to == null) to = now;
            if (from == null) from = to.minus(Duration.ofHours(2));
        }

        List<Trade> trades = tradeRepo.findByTsBetweenOrderByTsAsc(from, to);

        List<Map<String,Object>> cumSeries = new ArrayList<>();
        java.math.BigDecimal cum = java.math.BigDecimal.ZERO;
        for (Trade t : trades) {
            cum = cum.add(t.getProfit());
            cumSeries.add(Map.of("ts", t.getTs().toString(), "value", cum));
        }

        Map<String, List<Map<String,Object>>> perAsset = new LinkedHashMap<>();
        Map<String, java.math.BigDecimal> cm = new HashMap<>();
        for (Trade t : trades) {
            String sym = t.getAsset().getSymbol();
            cm.putIfAbsent(sym, java.math.BigDecimal.ZERO);
            cm.put(sym, cm.get(sym).add(t.getProfit()));
            perAsset.computeIfAbsent(sym, k -> new ArrayList<>()).add(Map.of("ts", t.getTs().toString(), "value", cm.get(sym)));
        }

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("from", from.toString()); res.put("to", to.toString()); res.put("count", trades.size());
        res.put("cumulativePnL", cumSeries); res.put("perAssetCumulative", perAsset);
        return res;
    }
}
