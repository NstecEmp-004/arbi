package com.example.arbsim.controller.api;

import com.example.arbsim.service.ControlService;
import com.example.arbsim.service.BudgetService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.math.BigDecimal;

@RestController
public class SimulationControlApiController {
    private final ControlService control;
    private final BudgetService budget;

    public SimulationControlApiController(ControlService control, BudgetService budget) {
        this.control = control;
        this.budget = budget;
    }

    @PostMapping("/api/control/trading")
    public Map<String, Object> setTrading(@RequestParam("enabled") boolean enabled) {
        control.setTradingEnabled(enabled);
        return Map.of("tradingEnabled", control.isTradingEnabled());
    }

    /** 予算（円）。UIは ?yen= を送るが、curl 等で ?amount= も使えるよう両対応にする */
    @PostMapping("/api/control/budget")
    public Map<String, Object> setBudget(
            @RequestParam(value = "amount", required = false) BigDecimal amount,
            @RequestParam(value = "yen", required = false) BigDecimal yen) {
        BigDecimal v = (amount != null) ? amount : (yen != null ? yen : BigDecimal.ZERO);
        budget.setSessionBudget(v);
        return Map.of("budget", budget.getSessionBudget());
    }
}
