package com.example.arbsim.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PricesController {
    @GetMapping("/prices")
    public String prices() { return "prices"; }
}
