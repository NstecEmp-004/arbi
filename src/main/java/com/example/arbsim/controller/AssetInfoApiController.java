package com.example.arbsim.controller;

import com.example.arbsim.entity.Asset;
import com.example.arbsim.entity.Market;
import com.example.arbsim.entity.PriceTick;
import com.example.arbsim.entity.Comment;
import com.example.arbsim.repo.AssetRepository;
import com.example.arbsim.repo.MarketRepository;
import com.example.arbsim.repo.PriceTickRepository;
import com.example.arbsim.repo.CommentRepository;
import com.example.arbsim.service.AssetWindowService;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/asset")
public class AssetInfoApiController {

    private final AssetRepository assetRepo;
    private final MarketRepository marketRepo;
    private final PriceTickRepository priceRepo;
    private final CommentRepository commentRepo;
    private final AssetWindowService windowSvc;

    public AssetInfoApiController(AssetRepository assetRepo, MarketRepository marketRepo,
                                  PriceTickRepository priceRepo, CommentRepository commentRepo,
                                  AssetWindowService windowSvc) {
        this.assetRepo = assetRepo;
        this.marketRepo = marketRepo;
        this.priceRepo = priceRepo;
        this.commentRepo = commentRepo;
        this.windowSvc = windowSvc;
    }

    private String companyBlurb(String symbol) {
        // 簡易ダミーデータ（3行程度）
        return switch(symbol) {
            case "A" -> "A社は次世代半導体の設計・製造を行うテック企業。\n主力は省電力チップとAI向け加速器。\n研究開発比率が高く、海外売上が約60%";
            case "B" -> "B社はクラウドSaaSを提供。\n中小企業向けに決済・在庫・CRMを一体提供。\n解約率が低く、サブスク売上が安定";
            case "C" -> "C社はロボティクス・物流自動化の専業。\n倉庫向けAGVとピッキングシステムが主力。\n海外拠点拡大中";
            default -> symbol + "社は多角化事業を展開";
        };
    }

    /** 現在価格（市場ごと）と平均、ウィンドウ内の高値・安値、会社概要 */
    @GetMapping("/{symbol}/summary")
    public Map<String, Object> summary(@PathVariable String symbol) {
        Asset asset = assetRepo.findBySymbol(symbol).orElseThrow();
        List<Market> markets = marketRepo.findAll();
        Instant from = windowSvc.getWindowStart(symbol);
        Instant to = Instant.now();

        BigDecimal curAvg = BigDecimal.ZERO;
        int curCount = 0;
        Map<String, BigDecimal> currentByMarket = new LinkedHashMap<>();
        BigDecimal hi = null, lo = null;

        for (Market m : markets) {
            List<PriceTick> last1 = priceRepo.findTop1ByMarketAndAssetOrderByTsDesc(m, asset);
            if (!last1.isEmpty()) {
                BigDecimal p = last1.get(0).getPrice();
                currentByMarket.put(m.getName(), p);
                curAvg = curAvg.add(p);
                curCount++;
            } else {
                currentByMarket.put(m.getName(), null);
            }
        }
        if (curCount > 0) curAvg = curAvg.divide(BigDecimal.valueOf(curCount), 4, RoundingMode.HALF_UP);

        // 高値・安値：ウィンドウ（取引開始から）内で算出（全市場の価格をひとまずまとめて比較）
        List<PriceTick> ticks = priceRepo.findByAssetAndTsBetweenOrderByTsAsc(asset, from, to);
        for (PriceTick t : ticks) {
            BigDecimal p = t.getPrice();
            hi = (hi == null || p.compareTo(hi) > 0) ? p : hi;
            lo = (lo == null || p.compareTo(lo) < 0) ? p : lo;
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("symbol", symbol);
        res.put("company", companyBlurb(symbol));
        res.put("current", Map.of("average", curAvg, "byMarket", currentByMarket));
        res.put("window", Map.of("from", from.toString(), "to", to.toString(), "high", hi, "low", lo));
        return res;
    }

    /** 折れ線グラフ用のシンプル時系列（平均価格）。minutes省略時は60分 */
    @GetMapping("/{symbol}/series")
    public Map<String, Object> series(@PathVariable String symbol,
                                      @RequestParam(name="minutes", required=false, defaultValue="60") long minutes) {
        Asset asset = assetRepo.findBySymbol(symbol).orElseThrow();
        Instant to = Instant.now();
        Instant from = to.minus(Duration.ofMinutes(minutes));
        List<PriceTick> ticks = priceRepo.findByAssetAndTsBetweenOrderByTsAsc(asset, from, to);

        // tsごとに価格を平均化
        Map<Instant, List<BigDecimal>> map = new LinkedHashMap<>();
        for (PriceTick t : ticks) {
            map.computeIfAbsent(t.getTs(), k -> new ArrayList<>()).add(t.getPrice());
        }
        List<Map<String, Object>> points = new ArrayList<>();
        for (var e : map.entrySet()) {
            BigDecimal avg = e.getValue().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(e.getValue().size()), 4, RoundingMode.HALF_UP);
            points.add(Map.of("ts", e.getKey().toString(), "price", avg));
        }
        return Map.of("points", points);
    }

    /** コメント */
    @GetMapping("/{symbol}/comments")
    public Map<String, Object> comments(@PathVariable String symbol) {
        var list = commentRepo.findTop100ByAssetSymbolOrderByCreatedAtDesc(symbol);
        List<Map<String,Object>> items = new ArrayList<>();
        for (var c : list) {
            items.add(Map.of("id", c.getId(), "content", c.getContent(), "createdAt", c.getCreatedAt().toString()));
        }
        return Map.of("items", items);
    }

    @PostMapping("/{symbol}/comments")
    public Map<String, Object> addComment(@PathVariable String symbol, @RequestParam("text") String text) {
        text = text == null ? "" : text.trim();
        if (text.isEmpty()) return Map.of("ok", false, "message", "empty");
        commentRepo.save(new Comment(symbol, text));
        return Map.of("ok", true);
    }

    /** 取引開始（この銘柄ページにおける統計ウィンドウをリセット） */
    @PostMapping("/{symbol}/window/reset")
    public Map<String, Object> reset(@PathVariable String symbol) {
        Instant start = windowSvc.resetWindow(symbol);
        return Map.of("ok", true, "start", start.toString());
    }
}