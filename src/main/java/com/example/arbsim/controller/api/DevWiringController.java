package com.example.arbsim.controller.api;

import com.example.arbsim.repo.TradeRepository;
import com.example.arbsim.entity.Trade;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.*;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;

@RestController
public class DevWiringController {

    private final ApplicationContext ctx;
    private final ObjectProvider<ScheduledTaskHolder> schedHolder;
    private final TradeRepository tradeRepo;

    public DevWiringController(ApplicationContext ctx,
            ObjectProvider<ScheduledTaskHolder> schedHolder,
            TradeRepository tradeRepo) {
        this.ctx = ctx;
        this.schedHolder = schedHolder;
        this.tradeRepo = tradeRepo;
    }

    /** 配線診断：主要ビーンの有無、スケジュールタスク数を可視化 */
    @GetMapping("/api/dev/wiring")
    public Map<String, Object> wiring() {
        Map<String, Object> res = new LinkedHashMap<>();
        String[] names = ctx.getBeanDefinitionNames();

        List<String> hits = new ArrayList<>();
        List<String> keywords = List.of("trade", "trading", "engine", "arb", "price", "tick", "market", "metrics",
                "state", "sim", "schedule");
        for (String n : names) {
            String low = n.toLowerCase();
            for (String kw : keywords)
                if (low.contains(kw)) {
                    hits.add(n);
                    break;
                }
        }
        res.put("beansLike", hits);
        var sh = schedHolder.getIfAvailable();
        res.put("scheduledTasks", sh != null ? sh.getScheduledTasks().size() : 0);
        res.put("hasTradeRepository", tradeRepo != null);
        return res;
    }

    /** ダミー1件を保存（配線が生きているか最短チェック） */
    @PostMapping("/api/dev/mock-trade")
    public Map<String, Object> mockTrade() {
        Trade t = new Trade();
        // プロジェクトの Trade に合わせて反射で setter を叩く（存在すれば入る）
        setIfExists(t, "setExecutedAt", Instant.class, Instant.now());
        setIfExists(t, "setQuantity", double.class, 1.0);
        setIfExists(t, "setBuyPrice", double.class, 50.0);
        setIfExists(t, "setSellPrice", double.class, 50.2);
        setIfExists(t, "setNet", double.class, 0.2);
        tradeRepo.save(t);
        return Map.of("ok", true);
    }

    /** 価格表から最安買い/最高売りを1回だけ見つけて約定を作る（応急処置） */
    @PostMapping("/api/dev/force-one")
    public Map<String, Object> forceOne() throws Exception {
        RestTemplate rt = new RestTemplate();
        ObjectMapper om = new ObjectMapper();
        // /api/state/table を読む
        String json = rt.getForObject("http://localhost:8080/api/state/table", String.class);
        JsonNode root = om.readTree(json);
        JsonNode table = root.get("table");
        if (table == null || !table.isArray() || table.size() == 0)
            return Map.of("ok", false, "reason", "no table");

        String bestAsset = null, buyM = null, sellM = null;
        double bestSpread = 0, buyP = 0, sellP = 0;
        for (String asset : List.of("A", "B", "C")) {
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            String minM = null, maxM = null;
            for (JsonNode row : table) {
                String m = row.get("market").asText();
                JsonNode p = row.get(asset);
                if (p == null || !p.isNumber())
                    continue;
                double v = p.asDouble();
                if (v < min) {
                    min = v;
                    minM = m;
                }
                if (v > max) {
                    max = v;
                    maxM = m;
                }
            }
            if (minM != null && maxM != null && max - min > bestSpread) {
                bestSpread = max - min;
                bestAsset = asset;
                buyM = minM;
                sellM = maxM;
                buyP = min;
                sellP = max;
            }
        }
        if (bestAsset == null)
            return Map.of("ok", false, "reason", "no spread");

        // 予算を /api/metrics から取る（なければ 1000）
        double budget = 1000.0;
        try {
            String mjson = rt.getForObject("http://localhost:8080/api/metrics", String.class);
            JsonNode m = om.readTree(mjson);
            if (m.has("budget") && m.get("budget").isNumber())
                budget = m.get("budget").asDouble();
        } catch (Exception ignore) {
        }

        double qty = Math.max(1.0, Math.floor(budget / Math.max(1e-9, buyP)));
        double net = (sellP - buyP) * qty;

        Trade t = new Trade();
        setIfExists(t, "setExecutedAt", Instant.class, Instant.now());
        setIfExists(t, "setQuantity", double.class, qty);
        setIfExists(t, "setBuyPrice", double.class, buyP);
        setIfExists(t, "setSellPrice", double.class, sellP);
        setIfExists(t, "setNet", double.class, net);
        tradeRepo.save(t);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("asset", bestAsset);
        res.put("buyMarket", buyM);
        res.put("sellMarket", sellM);
        res.put("qty", qty);
        res.put("buyPrice", buyP);
        res.put("sellPrice", sellP);
        res.put("net", net);
        return res;
    }

    private static void setIfExists(Object target, String method, Class<?> type, Object value) {
        try {
            Method m = target.getClass().getMethod(method, type);
            m.invoke(target, value);
        } catch (Exception ignore) {
        }
    }
}
