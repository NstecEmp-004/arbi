package com.example.arbsim.entity;

import jakarta.persistence.*;

@Entity
public class Market {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false) private String code;
    @Column(nullable = false) private String name;

    public Market() {}
    public Market(String code, String name) { this.code = code; this.name = name; }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}
