package com.example.arbsim.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ui")
public class PageController {

    @GetMapping({ "", "/" })
    public String uiRoot() { return "title"; }

    @GetMapping("/control")
    public String control() { return "control"; }

    @GetMapping("/main")
    public String main() { return "main"; }

    @GetMapping("/select-asset")
    public String selectAsset() { return "select-asset"; }

    @GetMapping("/select-market")
    public String selectMarket() { return "select-market"; }

    @GetMapping("/price")
    public String price() { return "price"; }

    @GetMapping("/asset-info")
    public String assetInfo() { return "asset-info"; }

    @GetMapping("/market-info")
    public String marketInfo() { return "market-info"; }

    @GetMapping("/graph")
    public String graph() { return "graph"; }

    @GetMapping("/profit-graph")
    public String profitGraph() { return "profit-graph"; }

    @GetMapping("/matrix")
    public String matrix() { return "matrix"; }
}
