package com.example.arbsim;

import com.example.arbsim.service.ControlService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ControlServiceTest {
    @Test
    void toggleTrading() {
        ControlService cs = new ControlService();
        assertTrue(cs.isTradingEnabled());
        cs.setTradingEnabled(false);
        assertFalse(cs.isTradingEnabled());
        cs.setTradingEnabled(true);
        assertTrue(cs.isTradingEnabled());
    }
}
