package com.example.arbsim.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TradesPageController {

    // /ui/trades → src/main/resources/templates/trades.html を表示
    @GetMapping("/ui/trades")
    public String tradesView() {
        return "trades"; // 拡張子不要。templates/trades.html を探します
    }

    // 他の /ui/* は既存コントローラに任せる。ここでは追加しない（競合回避）
}
