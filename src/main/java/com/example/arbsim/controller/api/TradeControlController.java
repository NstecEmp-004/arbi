package com.example.arbsim.controller.api;

import com.example.arbsim.repo.TradeRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 画面は既存の Analytics の /api/metrics を使う前提に戻す。
 * このクラスは「取引設定」用のAPIのみ提供（/api/dev/control/*）。
 * ※ /api/metrics は一切提供しない（競合と赤波線の元を断つ）
 */
@RestController
public class TradeControlController {

    // 既存APIとの衝突回避のため /api/dev に退避
    private static final String API_PREFIX = "/api/dev";

    @SuppressWarnings("unused")
    private final TradeRepository tradeRepo; // 今後拡張用（未使用でもDIだけ受ける）

    public TradeControlController(TradeRepository tradeRepo) {
        this.tradeRepo = tradeRepo;
    }

    // メモリ保持の設定値（UIの既存画面で利用する場合は別クラスから getter 参照可）
    private final AtomicReference<Double> budgetRef = new AtomicReference<>(0.0);
    private final AtomicReference<Double> thresholdRef = new AtomicReference<>(0.0);
    private final AtomicBoolean tradingEnabledRef = new AtomicBoolean(false);

    /** 予算設定：POST /api/dev/control/budget?amount=1000 */
    @PostMapping(API_PREFIX + "/control/budget")
    public Map<String, Object> setBudget(@RequestParam("amount") double amount) {
        budgetRef.set(Math.max(0.0, amount));
        return Map.of("ok", true, "budget", budgetRef.get());
    }

    /** 取引ON/OFF：POST /api/dev/control/trading?enabled=true */
    @PostMapping(API_PREFIX + "/control/trading")
    public Map<String, Object> setTrading(@RequestParam("enabled") boolean enabled) {
        tradingEnabledRef.set(enabled);
        return Map.of("ok", true, "tradingEnabled", tradingEnabledRef.get());
    }

    /** しきい値設定：POST /api/dev/control/threshold?value=0.0 */
    @PostMapping(API_PREFIX + "/control/threshold")
    public Map<String, Object> setThreshold(@RequestParam("value") double value) {
        thresholdRef.set(Math.max(0.0, value));
        return Map.of("ok", true, "threshold", thresholdRef.get());
    }

    // 他クラス（例：シミュレーション/取引エンジン）から参照できるように getter を公開
    public boolean isTradingEnabled() {
        return tradingEnabledRef.get();
    }

    public double getBudget() {
        return budgetRef.get();
    }

    public double getThreshold() {
        return thresholdRef.get();
    }
}
