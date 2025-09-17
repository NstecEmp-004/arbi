package com.example.arbsim;

import com.example.arbsim.service.FeeService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class FeeServiceTest {
    @Test
    void feeAtOrBelow1MIsZero() {
        FeeService s = new FeeService();
        assertEquals(new BigDecimal("0"), s.feeForNotional(new BigDecimal("0")));
        assertEquals(new BigDecimal("0"), s.feeForNotional(new BigDecimal("999999.99")));
        assertEquals(new BigDecimal("0"), s.feeForNotional(new BigDecimal("1000000")));
    }

    @Test
    void feeBetween1MAnd2MIs2200() {
        FeeService s = new FeeService();
        assertEquals(new BigDecimal("2200"), s.feeForNotional(new BigDecimal("1000000.01")));
        assertEquals(new BigDecimal("2200"), s.feeForNotional(new BigDecimal("1999999.99")));
        assertEquals(new BigDecimal("2200"), s.feeForNotional(new BigDecimal("2000000")));
    }

    @Test
    void feeAbove2MSteps1100Per1M() {
        FeeService s = new FeeService();
        assertEquals(new BigDecimal("3300"), s.feeForNotional(new BigDecimal("2000000.01")));
        assertEquals(new BigDecimal("3300"), s.feeForNotional(new BigDecimal("3000000.00")));
        assertEquals(new BigDecimal("4400"), s.feeForNotional(new BigDecimal("3999999.99")));
    }
}
