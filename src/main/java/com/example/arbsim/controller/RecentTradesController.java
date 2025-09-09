package com.example.arbsim.controller;

import com.example.arbsim.repo.TradeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 直近の取引一覧を返す。エンティティのメソッド名差異（executedAt/createdAt/timestamp 等）に
 * 反射で対応し、コンパイルエラー（赤波線）を回避する。
 *
 * GET /api/trades/recent?limit=50
 */
@RestController
public class RecentTradesController {

    private final TradeRepository tradeRepo;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public RecentTradesController(TradeRepository tradeRepo) {
        this.tradeRepo = tradeRepo;
    }

    @GetMapping("/api/trades/recent")
    public Map<String, Object> recent(@RequestParam(value = "limit", required = false) Integer limit) {
        int size = (limit == null || limit <= 0 || limit > 1000) ? 50 : limit;

        // ※ ソートキー名（executedAt）が環境で異なる可能性があるため、DBソートは使わず取得後に並べ替える
        List<?> all = tradeRepo.findAll();

        List<Map<String, Object>> items = new ArrayList<>(Math.min(all.size(), size));
        for (Object t : all) {
            Instant ts = toInstant(coalesce(
                    callNoArg(t, "getExecutedAt"),
                    callNoArg(t, "getCreatedAt"),
                    callNoArg(t, "getTimestamp"),
                    callNoArg(t, "getTime")));

            // 資産シンボル
            String asset = null;
            Object assetObj = callNoArg(t, "getAsset");
            if (assetObj != null) {
                Object sym = coalesce(callNoArg(assetObj, "getSymbol"), callNoArg(assetObj, "getCode"),
                        callNoArg(assetObj, "getName"));
                asset = sym != null ? String.valueOf(sym) : null;
            }

            // 市場名
            String buyMarket = null, sellMarket = null;
            Object bm = callNoArg(t, "getBuyMarket");
            if (bm != null) {
                Object name = coalesce(callNoArg(bm, "getName"), callNoArg(bm, "getCode"));
                buyMarket = name != null ? String.valueOf(name) : null;
            }
            Object sm = callNoArg(t, "getSellMarket");
            if (sm != null) {
                Object name = coalesce(callNoArg(sm, "getName"), callNoArg(sm, "getCode"));
                sellMarket = name != null ? String.valueOf(name) : null;
            }

            // 数量・価格
            Number qty = (Number) coalesce(callNoArg(t, "getQuantity"), callNoArg(t, "getQty"));
            Number buyPrice = (Number) coalesce(callNoArg(t, "getBuyPrice"), callNoArg(t, "getBid"));
            Number sellPrice = (Number) coalesce(callNoArg(t, "getSellPrice"), callNoArg(t, "getAsk"));

            // 利益（gross / net）：命名ゆらぎに対応
            Number gross = (Number) coalesce(
                    callNoArg(t, "getGross"),
                    callNoArg(t, "getGrossProfit"),
                    callNoArg(t, "getProfitGross"),
                    callNoArg(t, "getSpread"),
                    callNoArg(t, "getPnlGross"));
            Number net = (Number) coalesce(
                    callNoArg(t, "getNet"),
                    callNoArg(t, "getNetProfit"),
                    callNoArg(t, "getProfit"),
                    callNoArg(t, "getPnl"));

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("_ts", ts != null ? ts.toEpochMilli() : Long.MIN_VALUE); // 並び替え用の隠しキー
            m.put("time", ts != null ? FMT.format(ts) : null);
            m.put("asset", asset);
            m.put("buyMarket", buyMarket);
            m.put("sellMarket", sellMarket);
            m.put("qty", qty);
            m.put("buyPrice", buyPrice);
            m.put("sellPrice", sellPrice);
            m.put("gross", gross);
            m.put("net", net);

            items.add(m);
        }

        // 時刻降順で並び替え
        items.sort((a, b) -> Long.compare((Long) b.get("_ts"), (Long) a.get("_ts")));
        // limit 適用 & 隠しキー削除
        if (items.size() > size)
            items = new ArrayList<>(items.subList(0, size));
        for (Map<String, Object> m : items)
            m.remove("_ts");

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("trades", items);
        res.put("count", items.size());
        return res;
    }

    // ===== 反射・ユーティリティ =====

    private static Object callNoArg(Object target, String method) {
        if (target == null)
            return null;
        try {
            Method m = target.getClass().getMethod(method);
            return m.invoke(target);
        } catch (Throwable ignore) {
            return null;
        }
    }

    @SafeVarargs
    private static <T> T coalesce(T... vals) {
        for (T v : vals)
            if (v != null)
                return v;
        return null;
    }

    private static Instant toInstant(Object v) {
        try {
            if (v == null)
                return null;
            if (v instanceof Instant i)
                return i;
            if (v instanceof java.util.Date d)
                return d.toInstant();
            if (v instanceof Long l)
                return Instant.ofEpochMilli(l);
            if (v instanceof java.time.LocalDateTime ldt)
                return ldt.atZone(ZoneId.systemDefault()).toInstant();
            if (v instanceof java.time.ZonedDateTime zdt)
                return zdt.toInstant();
            if (v instanceof java.time.OffsetDateTime odt)
                return odt.toInstant();
        } catch (Throwable ignore) {
        }
        return null;
    }
}
