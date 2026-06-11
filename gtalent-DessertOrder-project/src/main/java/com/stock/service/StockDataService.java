package com.stock.service;

import cn.hutool.core.io.resource.ClassPathResource;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stock.model.KLineData;
import com.stock.model.StockQuote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 股票数据服务 - 使用新浪财经/东方财富免费API获取数据
 */
@Slf4j
@Service
public class StockDataService {

    private final Gson gson = new Gson();
    private final WebClient webClient = WebClient.builder().codecs(configurer ->
            configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)).build();

    /** 股票代码 -> 名称 缓存 */
    private final Map<String, String> nameCache = new ConcurrentHashMap<>();

    /**
     * 从股票池文件加载股票代码列表
     */
    public List<String> loadStockPool() {
        try {
            ClassPathResource resource = new ClassPathResource("stock-pool.txt");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getStream(), StandardCharsets.UTF_8));
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("加载股票池失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取实时行情（批量）
     * 使用新浪接口: https://hq.sinajs.cn/list=sh600519,sz000858
     */
    public List<StockQuote> getRealTimeQuotes(List<String> codes) {
        List<StockQuote> quotes = new ArrayList<>();
        if (codes == null || codes.isEmpty()) return quotes;

        // 新浪接口单次最多约50个，分批请求
        int batchSize = 50;
        for (int i = 0; i < codes.size(); i += batchSize) {
            List<String> batch = codes.subList(i, Math.min(i + batchSize, codes.size()));
            String sinaCodes = batch.stream()
                    .map(this::toSinaCode)
                    .collect(Collectors.joining(","));

            try {
                String url = "https://hq.sinajs.cn/list=" + sinaCodes;
                String response = webClient.get()
                        .uri(url)
                        .header("Referer", "https://finance.sina.com.cn")
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (response != null) {
                    parseSinaQuotes(response, quotes);
                }
            } catch (Exception e) {
                log.error("获取实时行情失败 batch={}", batch, e);
            }
        }
        return quotes;
    }

    /**
     * 获取单只股票实时行情
     */
    public Optional<StockQuote> getRealTimeQuote(String code) {
        List<StockQuote> quotes = getRealTimeQuotes(List.of(code));
        return quotes.isEmpty() ? Optional.empty() : Optional.of(quotes.get(0));
    }

    /**
     * 获取K线数据
     * 使用东方财富接口，支持日线/周线/月线
     * period: 101=日线, 102=周线, 103=月线
     */
    public List<KLineData> getKLineData(String code, String period) {
        String efCode = toEastMoneyCode(code);
        int periodCode = switch (period) {
            case "weekly" -> 102;
            case "monthly" -> 103;
            default -> 101;
        };

        // 获取最近120根K线
        int count = period.equals("daily") ? 120 : 60;
        String url = String.format(
                "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&fields1=f1,f2,f3,f4,f5,f6&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&klt=%d&fqt=1&end=20500101&lmt=%d",
                efCode, periodCode, count);

        try {
            String response = webClient.get()
                    .uri(url)
                    .header("Referer", "https://www.eastmoney.com")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseEastMoneyKLine(response, period);
        } catch (Exception e) {
            log.error("获取K线数据失败 code={}, period={}", code, period, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取涨幅榜
     * 使用东方财富接口
     */
    public List<StockQuote> getTopGainers(int topN) {
        String url = String.format(
                "https://push2.eastmoney.com/api/qt/clist/get?pn=1&pz=%d&po=1&np=1&fltt=2&invt=2&fid=f3&fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23&fields=f2,f3,f4,f5,f6,f7,f8,f12,f14,f15,f16,f17,f9",
                topN);
        try {
            String response = webClient.get()
                    .uri(url)
                    .header("Referer", "https://www.eastmoney.com")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return parseEastMoneyList(response);
        } catch (Exception e) {
            log.error("获取涨幅榜失败", e);
            return Collections.emptyList();
        }
    }

    // ========== 私有方法 ==========

    private String toSinaCode(String code) {
        if (code.startsWith("6")) return "sh" + code;
        return "sz" + code;
    }

    private String toEastMoneyCode(String code) {
        if (code.startsWith("6")) return "1." + code;
        return "0." + code;
    }

    /**
     * 解析新浪实时行情
     * 格式: var hq_str_sh600519="贵州茅台,开盘,昨收,当前,最高,最低,...";
     */
    private void parseSinaQuotes(String response, List<StockQuote> quotes) {
        String[] lines = response.split("\n");
        for (String line : lines) {
            try {
                line = line.trim();
                if (!line.startsWith("var hq_str_")) continue;

                int eqIdx = line.indexOf("=\"");
                int endIdx = line.lastIndexOf("\"");
                if (eqIdx < 0 || endIdx < 0) continue;

                String codePart = line.substring(12, eqIdx); // sh600519
                String data = line.substring(eqIdx + 2, endIdx);
                if (data.isEmpty()) continue;

                String code = codePart.substring(2); // 600519
                String[] parts = data.split(",");
                if (parts.length < 32) continue;

                double open = parseDouble(parts[1]);
                double preClose = parseDouble(parts[2]);
                double current = parseDouble(parts[3]);
                double high = parseDouble(parts[4]);
                double low = parseDouble(parts[5]);
                double amount = parseDouble(parts[37]) / 10000; // 万元
                long volume = (long) parseDouble(parts[8]);
                double changePercent = preClose > 0 ? (current - preClose) / preClose * 100 : 0;

                nameCache.put(code, parts[0]);

                quotes.add(StockQuote.builder()
                        .code(code)
                        .name(parts[0])
                        .currentPrice(current)
                        .openPrice(open)
                        .preClose(preClose)
                        .high(high)
                        .low(low)
                        .volume(volume)
                        .amount(amount)
                        .changePercent(changePercent)
                        .turnoverRate(parseDouble(parts[38]))
                        .build());
            } catch (Exception e) {
                log.trace("解析单行行情失败: {}", line);
            }
        }
    }

    /**
     * 解析东方财富K线数据
     */
    private List<KLineData> parseEastMoneyKLine(String response, String period) {
        List<KLineData> result = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(response).getAsJsonObject();
            JsonObject data = root.getAsJsonObject("data");
            if (data == null) return result;

            JsonArray klines = data.getAsJsonArray("klines");
            if (klines == null) return result;

            for (JsonElement element : klines) {
                String[] parts = element.getAsString().split(",");
                if (parts.length < 7) continue;

                double open = parseDouble(parts[1]);
                double close = parseDouble(parts[2]);
                double high = parseDouble(parts[3]);
                double low = parseDouble(parts[4]);
                long volume = (long) parseDouble(parts[5]);
                double amount = parseDouble(parts[6]);
                double changePercent = parseDouble(parts[8]);

                result.add(KLineData.builder()
                        .date(parts[0])
                        .open(open)
                        .close(close)
                        .high(high)
                        .low(low)
                        .volume(volume)
                        .amount(amount)
                        .changePercent(changePercent)
                        .changeAmount(parseDouble(parts[9]))
                        .build());
            }
        } catch (Exception e) {
            log.error("解析K线数据失败", e);
        }
        return result;
    }

    /**
     * 解析东方财富列表数据（涨幅榜等）
     */
    private List<StockQuote> parseEastMoneyList(String response) {
        List<StockQuote> result = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(response).getAsJsonObject();
            JsonObject data = root.getAsJsonObject("data");
            if (data == null) return result;

            JsonArray diff = data.getAsJsonArray("diff");
            if (diff == null) return result;

            for (JsonElement element : diff) {
                JsonObject item = element.getAsJsonObject();
                String code = item.has("f12") ? item.get("f12").getAsString() : "";
                String name = item.has("f14") ? item.get("f14").getAsString() : "";

                result.add(StockQuote.builder()
                        .code(code)
                        .name(name)
                        .currentPrice(getDouble(item, "f2"))
                        .changePercent(getDouble(item, "f3"))
                        .volume((long) getDouble(item, "f5"))
                        .amount(getDouble(item, "f6"))
                        .turnoverRate(getDouble(item, "f8"))
                        .build());
            }
        } catch (Exception e) {
            log.error("解析列表数据失败", e);
        }
        return result;
    }

    private double parseDouble(String val) {
        try {
            return Double.parseDouble(val.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private double getDouble(JsonObject obj, String key) {
        try {
            return obj.get(key).getAsDouble();
        } catch (Exception e) {
            return 0;
        }
    }

    public String getStockName(String code) {
        return nameCache.getOrDefault(code, code);
    }
}
