package com.example.arbsim.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicBoolean;

/** 価格更新や機会計算は常に実行。トレード実行だけON/OFF。 */
@Service
public class ControlService {
    private final AtomicBoolean tradingEnabled = new AtomicBoolean(true);
    public boolean isTradingEnabled() { return tradingEnabled.get(); }
    public void setTradingEnabled(boolean enabled) { tradingEnabled.set(enabled); }
}
