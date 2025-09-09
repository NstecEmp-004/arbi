package com.example.arbsim.service;

import com.example.arbsim.controller.TradeControlController;
import com.example.arbsim.entity.Trade;
import com.example.arbsim.repo.TradeRepository;
import com.fasterxml.jackson.databind.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 価格表(/api/state/table)を読み、最安買い×最高売りの差(スプレッド)がしきい値超えなら約定を記録。
 * - 反射で Trade のフィールドに合わせて保存（存在するsetterだけ叩く）
 * - 1回のtickで最大1件だけ作成し、UIの履歴と利益に反映させる
 */
@Service
public class TradingLoopScheduler {

    private final TradeRepository tradeRepo;
    private final TradeControlController ctrl; // 設定値を参照
    private final RestTemplate rt = new RestTemplate();
    private final ObjectMapper om = new ObjectMapper();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public TradingLoopScheduler(TradeRepository tradeRepo, TradeControlController ctrl) {
        this.tradeRepo = tradeRepo;
        this.ctrl = ctrl;
    }

    // 1.5秒に1回
    @Scheduled(fixedDelay = 1500)
    public void tick() {
        if (!ctrl.isTradingEnabled())
            return;
        if (running.getAndSet(true))
            return; // 重複防止
        try {
            String json = rt.getForObject("http://localhost:8080/api/state/table", String.class);
            if (json == null)
                return;
            JsonNode root = om.readTree(json);
            JsonNode table = root.get("table");
            if (table == null || !table.isArray() || table.size() == 0)
                return;

            // A,B,C で最安買い/最高売りを探索
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
                return;
            if (bestSpread < ctrl.getThreshold())
                return;

            // 予算から数量を試算（最低1に丸め）
            double budget = Math.max(0.0, ctrl.getBudget());
            if (budget <= 0)
                return;
            double qty = Math.max(1.0, Math.floor(budget / Math.max(1e-9, buyP)));
            double net = (sellP - buyP) * qty;

            // 約定を保存（存在するsetterにだけ入れる）
            Trade t = new Trade();
            setIfExists(t, "setExecutedAt", Instant.class, Instant.now());
            setIfExists(t, "setQuantity", double.class, qty);
            setIfExists(t, "setBuyPrice", double.class, buyP);
            setIfExists(t, "setSellPrice", double.class, sellP);
            setIfExists(t, "setNet", double.class, net);
            // 文字として入れられるsetterがあれば入れておく（表示のため）
            setIfExists(t, "setSymbol", String.class, bestAsset);
            setIfExists(t, "setAssetSymbol", String.class, bestAsset);
            setIfExists(t, "setBuyMarketName", String.class, buyM);
            setIfExists(t, "setSellMarketName", String.class, sellM);

            tradeRepo.save(t);
        } catch (Exception ignore) {
            // ログに出したければここで出力
        } finally {
            running.set(false);
        }
    }

    private static void setIfExists(Object target, String method, Class<?> type, Object value) {
        try {
            Method m = target.getClass().getMethod(method, type);
            m.invoke(target, value);
        } catch (Exception ignore) {
        }
    }
}
