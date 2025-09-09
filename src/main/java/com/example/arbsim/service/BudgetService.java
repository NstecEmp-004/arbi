package com.example.arbsim.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

/** セッションの「同時に使ってよい資金（予算）」を保持し、
 *  1ティック（1秒）の間に使える残額を管理する
 */
@Service
public class BudgetService {
    private final AtomicReference<BigDecimal> sessionBudget = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> tickRemaining = new AtomicReference<>(BigDecimal.ZERO);

    public synchronized void setSessionBudget(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        if (amount.signum() < 0) amount = BigDecimal.ZERO;
        sessionBudget.set(amount);
        // 設定と同時にティック残高も更新
        tickRemaining.set(amount);
    }

    public BigDecimal getSessionBudget() { return sessionBudget.get(); }

    /** 新しいティック開始時に呼び出し、同時使用上限をリセット */
    public synchronized void resetTickBudget() {
        tickRemaining.set(sessionBudget.get());
    }

    public BigDecimal remainingThisTick() { return tickRemaining.get(); }

    /** 残額の範囲内で消費し、実際に消費できた額を返す */
    public synchronized BigDecimal consume(BigDecimal notional) {
        if (notional == null || notional.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal rem = tickRemaining.get();
        if (rem.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal use = notional.min(rem);
        tickRemaining.set(rem.subtract(use));
        return use;
    }
}
