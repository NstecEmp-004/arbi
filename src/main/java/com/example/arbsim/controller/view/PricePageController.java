package com.example.arbsim.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PricePageController {
    @GetMapping("/prices")
    public String prices() {
        return "prices";
    }
}
