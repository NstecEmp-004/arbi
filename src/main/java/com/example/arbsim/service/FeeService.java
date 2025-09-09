package com.example.arbsim.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** 約定代金に対する定額手数料（片側ごと）。
 *  100万円以下:0円 / 100万1円〜200万円:2200円 / 以降100万円ごとに+1100円
 */
@Service
public class FeeService {
    private static final BigDecimal ONE_M = new BigDecimal("1000000");
    private static final BigDecimal TWO_M = new BigDecimal("2000000");
    private static final BigDecimal FEE_MID = new BigDecimal("2200");
    private static final BigDecimal FEE_STEP = new BigDecimal("1100");
    public BigDecimal feeForNotional(BigDecimal notional) {
        if (notional == null) return BigDecimal.ZERO;
        if (notional.compareTo(ONE_M) <= 0) return BigDecimal.ZERO;
        if (notional.compareTo(TWO_M) <= 0) return FEE_MID;
        BigDecimal over = notional.subtract(TWO_M);
        BigDecimal steps = over.divide(ONE_M, 0, RoundingMode.CEILING);
        return FEE_MID.add(FEE_STEP.multiply(steps));
    }
}
