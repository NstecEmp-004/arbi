package com.example.arbsim.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(indexes = {@Index(columnList = "market_id,asset_id,ts"), @Index(columnList = "ts")})
public class PriceTick {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) private Market market;
    @ManyToOne(optional = false) private Asset asset;
    @Column(nullable = false, precision = 19, scale = 6) private BigDecimal price;
    @Column(nullable = false) private Instant ts;

    public PriceTick() {}
    public PriceTick(Market m, Asset a, BigDecimal price, Instant ts) {
        this.market = m; this.asset = a; this.price = price; this.ts = ts;
    }
    public Long getId() { return id; }
    public Market getMarket() { return market; }
    public Asset getAsset() { return asset; }
    public BigDecimal getPrice() { return price; }
    public Instant getTs() { return ts; }
}
