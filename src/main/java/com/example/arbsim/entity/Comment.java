package com.example.arbsim.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(indexes = {@Index(columnList = "assetSymbol,createdAt")})
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 16)
    private String assetSymbol;
    @Column(nullable = false, length = 1000)
    private String content;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Comment() {}
    public Comment(String assetSymbol, String content) {
        this.assetSymbol = assetSymbol;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getAssetSymbol() { return assetSymbol; }
    public void setAssetSymbol(String s) { this.assetSymbol = s; }
    public String getContent() { return content; }
    public void setContent(String c) { this.content = c; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant t) { this.createdAt = t; }
}