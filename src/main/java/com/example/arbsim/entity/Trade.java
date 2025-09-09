package com.example.arbsim.entity;

import jakarta.persistence.*;
import lombok.Getter;              // ← 追加
import java.math.BigDecimal;
import java.time.Instant;

@Getter                            // ← 追加（全フィールドの getter を自動生成）
@Entity
@Table(indexes = @Index(columnList = "asset_id,ts"))
public class Trade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) private Asset asset;
    @ManyToOne(optional = false) private Market buyMarket;
    @ManyToOne(optional = false) private Market sellMarket;

    @Column(nullable = false, precision = 19, scale = 6) private BigDecimal quantity;
    @Column(nullable = false, precision = 19, scale = 6) private BigDecimal buyPrice;
    @Column(nullable = false, precision = 19, scale = 6) private BigDecimal sellPrice;

    @Column(nullable = false, precision = 19, scale = 6) private BigDecimal buyNotional;
    @Column(nullable = false, precision = 19, scale = 6) private BigDecimal sellNotional;

    @Column(nullable = false, precision = 19, scale = 6) private BigDecimal buyFee;
    @Column(nullable = false, precision = 19, scale = 6) private BigDecimal sellFee;

    @Column(nullable =  false, precision = 19, scale = 6) private BigDecimal grossProfit;
    @Column(nullable =  false, precision = 19, scale = 6) private BigDecimal profit; // net

    @Column(nullable = false) private Instant ts;

    public Trade() {}
    public Trade(Asset asset, Market buyM, Market sellM, BigDecimal qty, BigDecimal buyPrice, BigDecimal sellPrice,
                 BigDecimal buyNotional, BigDecimal sellNotional, BigDecimal buyFee, BigDecimal sellFee,
                 BigDecimal grossProfit, BigDecimal netProfit, Instant ts) {
        this.asset = asset; this.buyMarket = buyM; this.sellMarket = sellM;
        this.quantity = qty; this.buyPrice = buyPrice; this.sellPrice = sellPrice;
        this.buyNotional = buyNotional; this.sellNotional = sellNotional;
        this.buyFee = buyFee; this.sellFee = sellFee;
        this.grossProfit = grossProfit; this.profit = netProfit; this.ts = ts;
    }
}
