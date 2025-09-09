package com.example.arbsim.service;

import com.example.arbsim.config.SimulationConfig;
import com.example.arbsim.entity.Asset;
import com.example.arbsim.entity.Market;
import com.example.arbsim.entity.Trade;
import com.example.arbsim.repo.AssetRepository;
import com.example.arbsim.repo.MarketRepository;
import com.example.arbsim.repo.PriceTickRepository;
import com.example.arbsim.repo.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * 価格を一定間隔で更新し、スプレッド/出来高/予算/手数料を考慮して約定を生成するエンジン。
 * 価格更新は「直前価格 × U[0.99, 1.0199]」（一様乱数）を 2秒ごとに適用。
 * marketState / volumeService / budgetService のメソッド名差異は反射で吸収。
 */
@Service
public class SimulationEngine {

    private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);

    private final SimulationConfig cfg;
    private final MarketRepository marketRepo;
    private final AssetRepository assetRepo;
    private final PriceTickRepository priceRepo;
    private final TradeRepository tradeRepo;

    // 反射呼び出しを行うため Object で受ける。Bean 名で確定注入。
    private final Object marketState; // bean name: "marketState"
    private final Object volumeService; // bean name: "volumeService"
    private final Object budgetService; // bean name: "budgetService"

    private final ControlService controlService;
    private final FeeService feeService;

    private final Random rnd = new Random();

    public SimulationEngine(
            SimulationConfig cfg,
            MarketRepository marketRepo,
            AssetRepository assetRepo,
            PriceTickRepository priceRepo,
            TradeRepository tradeRepo,
            @Qualifier("marketState") Object marketState,
            @Qualifier("volumeService") Object volumeService,
            @Qualifier("budgetService") Object budgetService,
            ControlService controlService,
            FeeService feeService) {
        this.cfg = cfg;
        this.marketRepo = marketRepo;
        this.assetRepo = assetRepo;
        this.priceRepo = priceRepo;
        this.tradeRepo = tradeRepo;
        this.marketState = marketState;
        this.volumeService = volumeService;
        this.budgetService = budgetService;
        this.controlService = controlService;
        this.feeService = feeService;
    }

    /**
     * 取引頻度：既定 2000ms（=2秒）ごと。
     * application.properties/yml で simulation.tickMillis を上書き可能にしたい場合は
     * fixedRateString="${simulation.tickMillis:2000}" に変更してください。
     */
    @Scheduled(fixedRate = 2000)
    @Transactional
    public void tick() {
        final Instant now = Instant.now();

        final List<Market> markets = marketRepo.findAll();
        final List<Asset> assets = assetRepo.findAll();

        // ===== 出来高のリフレッシュ（存在すれば呼ぶ） =====
        callAny(volumeService,
                new String[] { "regenerate", "refresh", "reset", "regenerateAll", "reseed" },
                new Class[] { Set.class, Set.class },
                new Object[] { idsOf(markets), idsOf(assets) });
        callAny(volumeService,
                new String[] { "regenerate", "refresh", "reset", "regenerateAll", "reseed" },
                new Class[] {}, new Object[] {}); // 引数無し版も試す

        // ===== 価格更新（直前価格 × U[0.99, 1.0199]）& MarketState/DB保存 =====
        final BigDecimal minFactor = new BigDecimal("0.99");
        final BigDecimal maxFactor = new BigDecimal("1.0199");

        for (Market m : markets) {
            for (Asset a : assets) {
                BigDecimal prev = priceOf(m, a);
                if (prev == null)
                    prev = safeBasePrice(a);

                // 一様乱数の掛け算で次価格を生成
                BigDecimal next = randomWalkUniform(prev, minFactor, maxFactor);

                // MarketState 書き込み（候補メソッドを順に試す）
                boolean written = callAnyBool(marketState,
                        new String[] { "put", "setPrice", "updatePrice", "record", "upsert", "post" },
                        new Class[] { Market.class, Asset.class, BigDecimal.class, Instant.class },
                        new Object[] { m, a, next, now })
                        || callAnyBool(marketState,
                                new String[] { "put", "setPrice", "updatePrice", "record", "upsert", "post" },
                                new Class[] { Market.class, Asset.class, BigDecimal.class },
                                new Object[] { m, a, next })
                        || callAnyBool(marketState,
                                new String[] { "put", "setPrice", "updatePrice", "record", "upsert", "post" },
                                new Class[] { Long.class, Long.class, BigDecimal.class, Instant.class },
                                new Object[] { m.getId(), a.getId(), next, now });

                if (!written && log.isDebugEnabled()) {
                    log.debug("MarketState write skipped: market={}, asset={}, price={}",
                            m.getName(), a.getSymbol(), next);
                }

                // DB 側 PriceTick 保存（エンティティに合わせてください）
                priceRepo.save(new com.example.arbsim.entity.PriceTick(m, a, next, now));
            }
        }

        // ===== Tick予算のリセット（メソッドがあれば呼ぶ） =====
        callAny(budgetService,
                new String[] { "resetTick", "resettick", "beginTick", "newTick", "reset", "startTick" },
                new Class[] {}, new Object[] {});

        // 取引無効なら終わり
        if (!controlService.isTradingEnabled()) {
            if (log.isDebugEnabled())
                log.debug("Trading disabled. Skip trading loop.");
            return;
        }

        // snapshot(Optional)
        final Object snapshot = callAny(marketState, new String[] { "snapshot", "snap", "current", "asMap" },
                new Class[] {}, new Object[] {});

        // 閾値・数量上限 設定
        BigDecimal threshold = BigDecimal.ZERO;
        try {
            BigDecimal th = cfg.getThreshold();
            if (th != null)
                threshold = th;
        } catch (Throwable ignore) {
            /* 0 を使う */ }

        int configCap = 1_000_000;
        try {
            BigDecimal q = cfg.getTradeQty();
            if (q != null && q.intValue() > 0)
                configCap = q.intValue();
        } catch (Throwable ignore) {
            /* 既定値 */ }

        // ===== 銘柄ごとの最良買い/最良売り探索 & 約定 =====
        for (Asset a : assets) {
            Market bestBuyM = null, bestSellM = null;
            BigDecimal bestBuyP = null, bestSellP = null;

            for (Market m : markets) {
                BigDecimal p = priceFromSnapshot(snapshot, m, a);
                if (p == null)
                    p = priceOf(m, a);
                if (p == null)
                    continue;

                if (bestBuyP == null || p.compareTo(bestBuyP) < 0) {
                    bestBuyP = p;
                    bestBuyM = m;
                }
                if (bestSellP == null || p.compareTo(bestSellP) > 0) {
                    bestSellP = p;
                    bestSellM = m;
                }
            }
            if (bestBuyM == null || bestSellM == null || bestBuyM.equals(bestSellM)) {
                if (log.isDebugEnabled())
                    log.debug("No distinct markets for asset {}", a.getSymbol());
                continue;
            }

            BigDecimal spread = bestSellP.subtract(bestBuyP);
            if (spread.compareTo(threshold) <= 0) {
                if (log.isDebugEnabled())
                    log.debug("Spread below threshold: asset={}, spread={}, threshold={}",
                            a.getSymbol(), spread, threshold);
                continue;
            }

            // ---- 数量決定（出来高・予算・上限）
            int volBuy = remainingVolume(bestBuyM.getId(), a.getId());
            int volSell = remainingVolume(bestSellM.getId(), a.getId());
            int maxByVolume = Math.min(Math.max(volBuy, 0), Math.max(volSell, 0));

            BigDecimal remainTick = remainingBudgetThisTick();
            int maxByBudget = (remainTick.signum() > 0)
                    ? remainTick.divide(bestBuyP, 0, RoundingMode.FLOOR).intValue()
                    : 0;

            int qtyInt = Math.max(0, Math.min(maxByVolume, Math.min(maxByBudget, configCap)));
            if (qtyInt <= 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Qty=0 skip: asset={}, vol({}->{})={},{} budgetRem={} buyP={}",
                            a.getSymbol(), bestBuyM.getName(), bestSellM.getName(),
                            volBuy, volSell, remainTick, bestBuyP);
                }
                continue;
            }

            BigDecimal qty = new BigDecimal(qtyInt);
            BigDecimal buyNotional = bestBuyP.multiply(qty);
            BigDecimal sellNotional = bestSellP.multiply(qty);

            BigDecimal buyFee = (feeService != null) ? feeService.feeForNotional(buyNotional) : BigDecimal.ZERO;
            BigDecimal sellFee = (feeService != null) ? feeService.feeForNotional(sellNotional) : BigDecimal.ZERO;
            BigDecimal net = sellNotional.subtract(buyNotional).subtract(buyFee).subtract(sellFee);
            if (net.signum() <= 0) {
                if (log.isDebugEnabled())
                    log.debug("Net<=0 skip: asset={}, qty={}, buyP={}, sellP={}, net={}",
                            a.getSymbol(), qtyInt, bestBuyP, bestSellP, net);
                continue;
            }

            // 予算を実際に消費
            BigDecimal consumed = consumeBudget(buyNotional);
            if (consumed == null || consumed.compareTo(buyNotional) < 0) {
                if (log.isDebugEnabled())
                    log.debug("Budget consume failed: need={}, consumed={}", buyNotional, consumed);
                continue;
            }

            // 出来高を実際に消費（両市場）
            int c1 = consumeVolume(bestBuyM.getId(), a.getId(), qtyInt);
            int c2 = consumeVolume(bestSellM.getId(), a.getId(), qtyInt);
            if (c1 < qtyInt || c2 < qtyInt) {
                if (log.isDebugEnabled())
                    log.debug("Volume consume failed: tried={}, gotBuy={}, gotSell={}", qtyInt, c1, c2);
                continue;
            }

            // 約定保存
            tradeRepo.save(new Trade(
                    a, bestBuyM, bestSellM, qty,
                    bestBuyP, bestSellP,
                    buyNotional, sellNotional,
                    buyFee, sellFee,
                    sellNotional.subtract(buyNotional), // gross
                    net, Instant.now()));
        }
    }

    // ================= ヘルパ =================

    private Set<Long> idsOf(Collection<?> xs) {
        Set<Long> s = new LinkedHashSet<>();
        for (Object o : xs) {
            if (o instanceof Market m)
                s.add(m.getId());
            else if (o instanceof Asset a)
                s.add(a.getId());
        }
        return s;
    }

    /** 一様乱数の掛け算：prev × U[minFactor, maxFactor] */
    private BigDecimal randomWalkUniform(BigDecimal prev, BigDecimal minFactor, BigDecimal maxFactor) {
        double min = minFactor.doubleValue();
        double max = maxFactor.doubleValue();
        double factor = min + rnd.nextDouble() * (max - min); // U[min, max]
        BigDecimal next = prev.multiply(BigDecimal.valueOf(factor))
                .setScale(2, RoundingMode.HALF_UP);
        return next.max(new BigDecimal("0.01"));
    }

    private BigDecimal safeBasePrice(Asset a) {
        try {
            BigDecimal p = a.getBasePrice();
            return (p != null && p.signum() > 0) ? p : new BigDecimal("100.00");
        } catch (Throwable ignore) {
            return new BigDecimal("100.00");
        }
    }

    /** MarketState から価格を取得（getPrice/priceOf/get/fetch/lastPrice 等を許容） */
    private BigDecimal priceOf(Market m, Asset a) {
        Object v = callAny(marketState,
                new String[] { "getPrice", "priceOf", "get", "fetch", "lastPrice", "getLastPrice" },
                new Class[] { Market.class, Asset.class }, new Object[] { m, a });
        if (v == null) {
            v = callAny(marketState,
                    new String[] { "getPrice", "priceOf", "get", "lastPrice", "getLastPrice" },
                    new Class[] { Long.class, Long.class }, new Object[] { m.getId(), a.getId() });
        }
        if (v == null)
            return null;
        if (v instanceof BigDecimal bd)
            return bd;
        if (v instanceof Number n)
            return BigDecimal.valueOf(n.doubleValue());
        Object price = callAny(v, new String[] { "price", "getPrice", "value", "getValue" }, new Class[] {},
                new Object[] {});
        if (price instanceof BigDecimal bd2)
            return bd2;
        if (price instanceof Number n2)
            return BigDecimal.valueOf(n2.doubleValue());
        return null;
    }

    /** snapshot(Map構造) がある場合にそこから価格を取る */
    @SuppressWarnings("unchecked")
    private BigDecimal priceFromSnapshot(Object snapshot, Market m, Asset a) {
        if (!(snapshot instanceof Map))
            return null;
        Object line = ((Map<?, ?>) snapshot).get(m.getId());
        if (!(line instanceof Map))
            return null;
        Object cell = ((Map<?, ?>) line).get(a.getId());
        if (cell == null)
            return null;
        if (cell instanceof BigDecimal bd)
            return bd;
        if (cell instanceof Number n)
            return BigDecimal.valueOf(n.doubleValue());
        Object price = callAny(cell, new String[] { "price", "getPrice", "value", "getValue" }, new Class[] {},
                new Object[] {});
        if (price instanceof BigDecimal bd2)
            return bd2;
        if (price instanceof Number n2)
            return BigDecimal.valueOf(n2.doubleValue());
        return null;
    }

    /** このTick残予算（候補名を幅広く試す） */
    private BigDecimal remainingBudgetThisTick() {
        Object v = callAny(budgetService,
                new String[] {
                        "remainingThisTick", "getRemainingThisTick",
                        "availableThisTick", "getAvailableThisTick",
                        "remaining", "getRemaining",
                        "remainingBudget", "getRemainingBudget",
                        "remainingYen", "getRemainingYen",
                        "available", "getAvailable", "budgetRemaining", "currentRemaining"
                },
                new Class[] {}, new Object[] {});
        if (v instanceof BigDecimal bd)
            return bd;
        if (v instanceof Number n)
            return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    /** 予算の消費（消費できた金額を返す。boolean/numberにも対応） */
    private BigDecimal consumeBudget(BigDecimal amount) {
        Object v = callAny(budgetService,
                new String[] { "consume", "use", "debit", "spend", "allocate", "take" },
                new Class[] { BigDecimal.class }, new Object[] { amount });
        if (v instanceof BigDecimal bd)
            return bd;
        if (v instanceof Boolean b)
            return b ? amount : BigDecimal.ZERO;
        if (v instanceof Number n)
            return BigDecimal.valueOf(n.doubleValue());
        return null;
    }

    /** 出来高の残り（候補名広め） */
    private int remainingVolume(Long marketId, Long assetId) {
        Object v = callAny(volumeService,
                new String[] { "remaining", "getRemaining", "available", "getAvailable", "balance", "getBalance",
                        "remainingQty", "getRemainingQty", "remainingVolume", "getRemainingVolume" },
                new Class[] { Long.class, Long.class }, new Object[] { marketId, assetId });
        if (v instanceof Number n)
            return n.intValue();
        return 0;
    }

    /** 出来高を消費（実際に消費できた数量を返す） */
    private int consumeVolume(Long marketId, Long assetId, int qty) {
        Object v = callAny(volumeService,
                new String[] { "consume", "take", "decrement", "use", "allocate", "reserve" },
                new Class[] { Long.class, Long.class, Integer.class },
                new Object[] { marketId, assetId, qty });
        if (v instanceof Number n)
            return n.intValue();
        if (v instanceof Boolean b)
            return b ? qty : 0;
        return 0;
    }

    // ---------- 反射ユーティリティ ----------

    private static Object callAny(Object target, String[] methodNames, Class<?>[] preferSig, Object[] args) {
        if (target == null)
            return null;
        for (String name : methodNames) {
            Object v = callFlex(target, name, preferSig, args);
            if (v != null)
                return v;
            v = callFlexLenient(target, name, args);
            if (v != null)
                return v;
        }
        return null;
    }

    private static boolean callAnyBool(Object target, String[] names, Class<?>[] sig, Object[] args) {
        Object v = callAny(target, names, sig, args);
        return v != null;
    }

    private static Object callFlex(Object target, String name, Class<?>[] sig, Object[] args) {
        if (target == null)
            return null;
        try {
            Method m = target.getClass().getMethod(name, sig);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static Object callFlexLenient(Object target, String name, Object[] args) {
        if (target == null)
            return null;
        try {
            Method[] methods = target.getClass().getMethods();
            outer: for (Method m : methods) {
                if (!m.getName().equalsIgnoreCase(name))
                    continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length != args.length)
                    continue;
                for (int i = 0; i < p.length; i++) {
                    if (args[i] == null)
                        continue;
                    if (!wrap(p[i]).isInstance(args[i]))
                        continue outer;
                }
                m.setAccessible(true);
                return m.invoke(target, args);
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static Class<?> wrap(Class<?> c) {
        if (!c.isPrimitive())
            return c;
        if (c == int.class)
            return Integer.class;
        if (c == long.class)
            return Long.class;
        if (c == double.class)
            return Double.class;
        if (c == float.class)
            return Float.class;
        if (c == boolean.class)
            return Boolean.class;
        if (c == byte.class)
            return Byte.class;
        if (c == short.class)
            return Short.class;
        if (c == char.class)
            return Character.class;
        return c;
    }
}
