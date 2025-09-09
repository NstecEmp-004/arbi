package com.example.arbsim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "sim")
public class SimulationConfig {
    private int markets = 5;
    private List<String> assets = List.of("A","B","C");
    private Map<String, BigDecimal> initialPrice;
    private double epsilon = 0.0001;
    private BigDecimal threshold = new BigDecimal("0.05"); // net(円)
    private int resetIntervalSec = 60;
    private BigDecimal tradeQty = new BigDecimal("1");

    public int getMarkets() { return markets; }
    public void setMarkets(int markets) { this.markets = markets; }
    public List<String> getAssets() { return assets; }
    public void setAssets(List<String> assets) { this.assets = assets; }
    public Map<String, BigDecimal> getInitialPrice() { return initialPrice; }
    public void setInitialPrice(Map<String, BigDecimal> initialPrice) { this.initialPrice = initialPrice; }
    public double getEpsilon() { return epsilon; }
    public void setEpsilon(double epsilon) { this.epsilon = epsilon; }
    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
    public int getResetIntervalSec() { return resetIntervalSec; }
    public void setResetIntervalSec(int resetIntervalSec) { this.resetIntervalSec = resetIntervalSec; }
    public BigDecimal getTradeQty() { return tradeQty; }
    public void setTradeQty(BigDecimal tradeQty) { this.tradeQty = tradeQty; }
}
