# A股智能选股系统

## 项目结构

```
java-stock/
├── pom.xml                              # Maven配置
├── src/main/resources/
│   ├── application.yml                   # 配置文件（钉钉/腾讯AI/定时任务）
│   └── stock-pool.txt                    # 股票池（可自行添加股票代码）
└── src/main/java/com/stock/
    ├── StockApplication.java             # 启动类
    ├── config/
    │   ├── DingTalkConfig.java           # 钉钉配置
    │   ├── TencentAIConfig.java          # 腾讯AI配置
    │   └── StockConfig.java              # 股票配置
    ├── model/
    │   ├── KLineData.java                # K线数据
    │   ├── StockQuote.java               # 实时行情
    │   ├── MACDResult.java               # MACD指标
    │   ├── StockSignal.java              # 选股信号
    │   ├── NewsItem.java                 # 新闻
    │   └── AnomalyEvent.java             # 异动事件
    ├── service/
    │   ├── StockDataService.java         # 数据源（新浪/东方财富API）
    │   ├── IndicatorService.java         # 技术指标引擎（MACD/MA/RSI/KDJ）
    │   ├── StockSelectService.java       # 选股策略引擎
    │   ├── AnomalyDetectService.java     # 异动监测
    │   ├── NewsService.java              # 新闻抓取
    │   ├── DingTalkService.java          # 钉钉推送
    │   └── TencentAIService.java         # 腾讯混元大模型
    ├── task/
    │   └── ScheduledTasks.java           # 定时任务调度
    └── controller/
        └── StockController.java          # REST API
```

## 核心功能

| 模块 | 功能 |
|------|------|
| **选股引擎** | MACD金叉（日/周/月线）、零轴上金叉、MACD底背离、放量突破、均线多头排列、综合评分 |
| **技术指标** | MACD(12/26/9)、MA(5/10/20/60)、RSI(6/12/24)、KDJ(9)、量比分析 |
| **异动监测** | 涨跌停、急涨急跌、换手率异常、尾盘异动，带30分钟冷却防重复 |
| **新闻推送** | 东方财富要闻 + 新浪7x24快讯，支持AI自动分析点评 |
| **钉钉机器人** | Markdown格式推送，支持加签安全验证 |
| **腾讯混元AI** | 新闻智能分析、选股结果点评、个股技术面分析 |

## 使用前配置

修改 `application.yml` 中的3个关键配置：

```yaml
# 1. 钉钉机器人（钉钉群设置 -> 智能群助手 -> 添加机器人）
dingtalk:
  webhook: https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN
  secret: YOUR_SECRET    # 加签密钥

# 2. 腾讯混元大模型（https://cloud.tencent.com/product/hunyuan）
tencent-ai:
  api-key: YOUR_API_KEY  # 腾讯云API密钥

# 3. 股票池 - 在 stock-pool.txt 中添加你关注的股票代码
```

## REST API 接口

| 接口 | 说明 |
|------|------|
| `GET /api/quote/{code}` | 实时行情 |
| `GET /api/kline/{code}?period=weekly` | K线数据（daily/weekly/monthly） |
| `GET /api/indicator/macd/{code}` | MACD指标 |
| `GET /api/indicator/all/{code}` | 全部技术指标 |
| `GET /api/select` | 执行选股 |
| `POST /api/select/notify` | 选股并推送钉钉 |
| `GET /api/anomaly/scan` | 扫描异动 |
| `GET /api/news` | 获取新闻 |
| `POST /api/ai/analyze-signals` | AI分析选股 |
| `GET /api/ai/analyze/{code}` | AI分析个股 |
| `GET /api/test/dingtalk` | 测试钉钉推送 |

## 定时任务（自动执行）

| 任务 | 时间 | 说明 |
|------|------|------|
| 异动监测 | 交易日 9:30-15:00 每5分钟 | 实时异动预警 |
| 选股 | 每交易日 15:30 | 收盘后MACD选股+AI点评+钉钉推送 |
| 新闻推送 | 每天 8:00 | 盘前财经要闻+AI市场分析 |

## 启动方式

```bash
# 编译运行
mvn spring-boot:run

# 或打包后运行
mvn clean package -DskipTests
java -jar target/java-stock-1.0.0.jar
```

访问地址：http://localhost:8088

> 需要 JDK 21+
