package com.example.arbsim;

import com.example.arbsim.service.AssetWindowService;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class AssetWindowServiceTest {
    @Test
    void defaultIsEpochAndResetSetsNow() {
        AssetWindowService s = new AssetWindowService();
        assertEquals(Instant.EPOCH, s.getWindowStart("A"));
        Instant start = s.resetWindow("A");
        assertEquals(start, s.getWindowStart("A"));
    }
}
