
package com.example.arbsim;

import com.example.arbsim.service.VolumeService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VolumeServiceTest {

    private static int callGet(VolumeService s, Long marketId, Long assetId) throws Exception {
        String[] names = new String[] { "getAvailable", "available", "getVolume", "volumeOf" };
        for (String n : names) {
            try {
                Method m = VolumeService.class.getDeclaredMethod(n, Long.class, Long.class);
                m.setAccessible(true);
                Object v = m.invoke(s, marketId, assetId);
                return ((Integer) v).intValue();
            } catch (NoSuchMethodException ignored) {
            }
        }
        Map<Long, Map<Long, Integer>> snap = s.snapshot();
        Integer v = snap.getOrDefault(marketId, Collections.emptyMap()).get(assetId);
        return v == null ? 0 : v;
    }

    @Test
    void regenerateMatrixAndBounds() throws Exception {
        VolumeService s = new VolumeService();
        List<Long> markets = Arrays.asList(1L, 2L);
        List<Long> assets = Arrays.asList(10L, 20L, 30L);
        s.regenerate(markets, assets);

        for (Long mid : markets) {
            for (Long aid : assets) {
                int v = callGet(s, mid, aid);
                assertTrue(v >= 0 && v <= 1000, "0..1000 within bounds");
            }
        }

        Map<Long, Map<Long, Integer>> snap = s.snapshot();
        snap.get(1L).put(10L, 99999);
        int original = callGet(s, 1L, 10L);
        assertTrue(original >= 0 && original <= 1000);
    }
}
