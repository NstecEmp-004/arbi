package com.example.arbsim.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(indexes = @Index(columnList = "opportunity_id"))
public class OpportunityPair {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private Opportunity opportunity;
    @ManyToOne(optional = false) private com.example.arbsim.entity.Market buyMarket;
    @ManyToOne(optional = false) private com.example.arbsim.entity.Market sellMarket;
    @Column(nullable = false, precision = 19, scale = 6) private BigDecimal buyPrice;
    @Column(nullable = false, precision = 19, scale = 6) private BigDecimal sellPrice;
    @Column(nullable = false, precision = 19, scale = 6) private BigDecimal spreadAbs;
    @Column(nullable = false) private boolean best;

    public OpportunityPair() {}
    public OpportunityPair(Opportunity opp, com.example.arbsim.entity.Market buy, com.example.arbsim.entity.Market sell, BigDecimal bp, BigDecimal sp, BigDecimal spreadAbs, boolean best) {
        this.opportunity = opp; this.buyMarket = buy; this.sellMarket = sell; this.buyPrice = bp; this.sellPrice = sp; this.spreadAbs = spreadAbs; this.best = best;
    }
    public Long getId() { return id; }
}
