package com.example.arbsim.controller.api;

import com.example.arbsim.service.ControlService;
import com.example.arbsim.service.BudgetService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * シミュレーション制御（API版）
 * - /api/control/trading?enabled=... で取引ON/OFF
 * - /api/control/budget?yen=... でセッション予算を更新
 * 画面のJSから叩かれる想定。機能や遷移は変更しない。
 */
@RestController
public class SimulationControlApiController {

    private final ControlService control;
    private final BudgetService budget;

    public SimulationControlApiController(ControlService control, BudgetService budget) {
        this.control = control;
        this.budget = budget;
    }

    /** 取引の有効・無効を切り替える（ボタンの終了時はこちらを叩くケースあり） */
    @PostMapping("/api/control/trading")
    public Map<String, Object> setTrading(@RequestParam("enabled") boolean enabled) {
        control.setTradingEnabled(enabled);
        return Map.of("tradingEnabled", control.isTradingEnabled());
    }

    /** セッション予算を更新（円）。?amount= も互換で受け付ける */
    @PostMapping("/api/control/budget")
    public Map<String, Object> setBudget(
            @RequestParam(value = "amount", required = false) BigDecimal amount,
            @RequestParam(value = "yen", required = false) BigDecimal yen) {
        BigDecimal v = (amount != null) ? amount : (yen != null ? yen : BigDecimal.ZERO);
        if (v.signum() < 0)
            v = BigDecimal.ZERO;
        budget.setSessionBudget(v);
        return Map.of("budget", budget.getSessionBudget());
    }
}
