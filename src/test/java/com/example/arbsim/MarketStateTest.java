package com.example.arbsim;

import com.example.arbsim.entity.Asset;
import com.example.arbsim.entity.Market;
import com.example.arbsim.service.MarketState;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MarketStateTest {
    @Test
    void putLikeMethodAndSnapshot() throws Exception {
        MarketState s = new MarketState();
        Market m = new Market("M1","M1");
        Asset a = new Asset("A","A Stock", new BigDecimal("100"));

        Field mid = Market.class.getDeclaredField("id");
        mid.setAccessible(true);
        mid.set(m, 1L);
        Field aid = Asset.class.getDeclaredField("id");
        aid.setAccessible(true);
        aid.set(a, 10L);

        Method target = null;
        for (Method me : MarketState.class.getDeclaredMethods()) {
            Class<?>[] ps = me.getParameterTypes();
            if (ps.length == 4 &&
                Market.class.isAssignableFrom(ps[0]) &&
                Asset.class.isAssignableFrom(ps[1]) &&
                BigDecimal.class.isAssignableFrom(ps[2]) &&
                Instant.class.isAssignableFrom(ps[3])) {
                target = me; break;
            }
        }
        assertNotNull(target, "MarketState should have a put/update-like method");
        target.setAccessible(true);
        target.invoke(s, m, a, new BigDecimal("123.45"), Instant.parse("2024-01-01T00:00:00Z"));

        Map<Long, Map<Long, MarketState.PricePoint>> snap = s.snapshot();
        assertTrue(snap.containsKey(1L));
        assertTrue(snap.get(1L).containsKey(10L));
        assertEquals(new BigDecimal("123.45"), snap.get(1L).get(10L).price());
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), s.lastTick());
    }
}
