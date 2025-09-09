package com.example.arbsim.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(indexes = @Index(columnList = "asset_id,ts"))
public class Opportunity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private Asset asset;
    @Column(nullable = false) private Instant ts;

    public Opportunity() {}
    public Opportunity(Asset asset, Instant ts) { this.asset = asset; this.ts = ts; }
    public Long getId() { return id; }
    public Asset getAsset() { return asset; }
    public Instant getTs() { return ts; }
}
