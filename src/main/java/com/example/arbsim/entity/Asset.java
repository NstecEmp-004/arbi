package com.example.arbsim.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Asset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false) private String symbol;
    @Column(nullable = false) private String name;
    @Column(nullable = false, precision = 19, scale = 6) private BigDecimal basePrice;

    public Asset() {}
    public Asset(String symbol, String name, BigDecimal basePrice) {
        this.symbol = symbol; this.name = name; this.basePrice = basePrice;
    }
    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public BigDecimal getBasePrice() { return basePrice; }
}
