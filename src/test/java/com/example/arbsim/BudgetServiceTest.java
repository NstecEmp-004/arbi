package com.example.arbsim;

import com.example.arbsim.service.BudgetService;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BudgetServiceTest {
    private static void callTickReset(BudgetService s) throws Exception {
        String[] names = new String[] {"resetTickRemaining","resetTick","resettick","beginTick","newTick","reset","startTick"};
        for (String n : names) {
            try {
                Method m = BudgetService.class.getDeclaredMethod(n);
                m.setAccessible(true);
                m.invoke(s);
                return;
            } catch (NoSuchMethodException ignored) {}
        }
        fail("tick reset method not found on BudgetService");
    }

    @Test
    void budgetAndConsumeFlow() throws Exception {
        BudgetService s = new BudgetService();
        s.setSessionBudget(new BigDecimal("1000"));
        callTickReset(s);

        assertEquals(new BigDecimal("1000"), s.remainingThisTick());

        BigDecimal used = s.consume(new BigDecimal("200"));
        assertEquals(new BigDecimal("200"), used);
        assertEquals(new BigDecimal("800"), s.remainingThisTick());

        assertEquals(new BigDecimal("800"), s.consume(new BigDecimal("9999")));
        assertEquals(new BigDecimal("0"), s.remainingThisTick());

        assertEquals(new BigDecimal("0"), s.consume(new BigDecimal("1")));
        assertEquals(new BigDecimal("0"), s.consume(new BigDecimal("-1")));
        assertEquals(new BigDecimal("0"), s.consume(null));
    }
}
