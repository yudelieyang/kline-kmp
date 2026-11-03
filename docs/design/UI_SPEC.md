# UI_SPEC.md：K 线 App 移动端界面规格 v1

日期：2026-11-02
适用范围：`shared/src/commonMain` 的 UI 层，以及 `androidApp`、`desktopApp` 的启动入口。
配套文件：`docs/design/mockups.html`（视觉稿，1 CSS px = 1 dp）和 `docs/design/png/*.png`。
**视觉稿和本规格冲突时，以本规格为准。** 视觉稿里的数字都是示意数据，实现时所有数字都必须来自真实数据。

---

## 0. 已确认的设计决定（Jun 在设计对话中拍板）

| # | 问题 | 决定 |
|---|---|---|
| 1 | 留白怎么填 | 只用现有数据：价格区放"已加载区间"统计；图表下方放"最近 K 线列表"。不接 24h 行情，不碰网络代码。不做诊断卡片。 |
| 2 | 连接状态 | 顶栏状态胶囊（可点开详情面板）＋ 非实时时出现琥珀色横幅。 |
| 2a | 状态映射 | 用纯函数解析 `status` 字符串，**不改** `LiveMarketHolder`。函数返回结构化类型，不返回文字。 |
| 2b | 异常计数 | 只放在详情面板里；计数大于 0 时胶囊右上角加小角标。 |
| 3 | 十字光标面板 | **留在 Canvas 内**，只改样式：主题色、圆角、半透明、描边。 |
| 3a | MA 图例 | **破例删除** Canvas 内画图例的代码，在图表上方加一行 MA 数值栏。这是图表代码里唯一的非样式改动。 |
| 4 | 深色模式 | 浅色＋深色，启动时跟随系统，顶栏有手动切换按钮。切换结果**不持久化**。 |
| 5 | 语言 | 跟随系统，中英双语，文字放进 Compose 资源文件。 |
| 6 | 离线样例入口 | 只在横幅里出现"查看离线样例"；离线模式下横幅改为"返回实时"。 |
| 7 | 横屏 | Android 锁定竖屏。Desktop 窗口照常可缩放。 |
| — | 页面滚动 | 整页**不滚动**（图表的 `detectTransformGestures` 会吃掉竖直拖动）。只有最近 K 线列表内部可滚动。 |
| — | 安全区 | `safeContentPadding()` 换成 `windowInsetsPadding(WindowInsets.safeDrawing)`，不再让出左右系统手势区。 |
| — | 图标 | 不加 Material Icons 依赖。太阳、月亮图标用 Compose Canvas 自己画。 |

---

## 1. 页面结构

竖屏，从上到下一列（`Column`），整体放在 `windowInsetsPadding(WindowInsets.safeDrawing)` 里：

```
TopBar              56 dp（字体放大时随内容增高）
PriceBlock          自适应高度
LoadedRangeNote     自适应高度（一行，放大时可能两行）
MaRow               最小 24 dp，FlowRow 可换行
StatusBanner        仅非实时或离线时出现，最小 40 dp，带展开动画
CandleChart         Modifier.weight(1f)
HorizontalDivider   1 dp
RecentCandleList    固定高度 listHeight（见 1.1），内部 LazyColumn 可滚动
```

### 1.1 高度分配规则（关键）

目标：**横幅出现时，由列表让出高度，图表高度尽量不变。**

```
contentHeight   = 安全区内可用高度（BoxWithConstraints 的 maxHeight）
baseListHeight  = if (fontScale > 1.15f) 100.dp
                  else (contentHeight * 0.22f).coerceIn(100.dp, 172.dp)
bannerHeight    = 横幅实际高度 + 上下外边距 8 dp（横幅不可见时为 0；用 onSizeChanged 测量，跟随横幅的动画变化）
listHeight      = (baseListHeight - bannerHeight).coerceAtLeast(64.dp)
CandleChart     = Modifier.weight(1f)
```

`64 dp` = 列表表头 28 dp + 一行 36 dp。列表已经缩到 64 dp 之后横幅还要更多空间，才由图表让出。

参考数值（来自视觉稿实测，字体 1.0 倍）：

| 屏幕 | 可用高度 | 列表高度 | 图表高度（无横幅） |
|---|---|---|---|
| 384 × 845 dp（iQOO 15 当前） | 约 793 | 172 | 约 435 |
| 360 × 780 dp | 约 728 | 160 | 约 317（有横幅时） |
| 430 × 932 dp | 约 880 | 172 | 约 522 |
| 360 × 780 dp，字体 1.3 倍 | 约 728 | 100 | 约 261 |

Desktop：同一套布局。`desktopApp/main.kt` 把窗口初始尺寸设为 `DpSize(480.dp, 900.dp)`（用 `rememberWindowState(size = …)`），方便截图时接近手机比例；窗口仍可任意缩放。

### 1.2 宽窄两种价格区布局

```
compact = maxWidth < 372.dp || fontScale > 1.15f
```

- **宽布局**（视觉稿 ①②③⑤⑥⑦）：左边价格列 `weight(1f)`，右边 2×2 统计网格，两者间距 16 dp。
- **紧凑布局**（视觉稿 ④⑧）：价格列在上，统计网格在下，网格两列各 `weight(1f)`，占满宽度。

---

## 2. 设计令牌（Design Tokens）

### 2.1 颜色

所有颜色集中定义在 `ui/theme/KlineColors.kt`，通过 `CompositionLocal` 提供。**任何组件和图表都不能直接写 `Color(0x…)` 或 `Color.White`。**

| 令牌 | 浅色 | 深色 | 用途 |
|---|---|---|---|
| `background` | `#FFFFFF` | `#0F141A` | 页面背景 |
| `surface` | `#F4F6F9` | `#171D24` | 详情面板背景（深色）、次级区域 |
| `surfaceVariant` | `#EEF1F5` | `#1F2731` | 状态胶囊底色、骨架占位 |
| `divider` | `#E2E7EE` | `#26303B` | 分隔线、列表行线 |
| `textPrimary` | `#111827` | `#E6EAF0` | 主要文字 |
| `textSecondary` | `#5F6B7A` | `#98A3B3` | 标签、说明 |
| `textTertiary` | `#8A94A3` | `#6E7A8A` | 脚注、`--` |
| `accent` | `#4F46E5` | `#8B8BF5` | "实时"圆点、"同步中"圆点 |
| `upText` | `#D23535` | `#F0605D` | 上涨的**文字**（价格、涨跌幅） |
| `downText` | `#0F7F55` | `#2EBD85` | 下跌的**文字** |
| `statusConnecting` | `#8A94A3` | `#6E7A8A` | 连接中、离线样例的圆点 |
| `statusWarning` | `#D97706` | `#F5A524` | 重连中圆点、横幅圆点、异常角标 |
| `statusError` | `#4B5563` | `#98A3B3` | 已断开的感叹号图标 |
| `bannerWarningBg / Text / Border` | `#FFF4E0 / #7A4100 / #F3D19C` | `#33270F / #F7C67A / #5C4316` | 琥珀色横幅 |
| `bannerInfoBg / Text / Border` | `#EEF1F5 / #374151 / #D9DFE7` | `#1F2731 / #C9D1DB / #2F3A47` | 离线样例横幅 |
| `scrim` | `#111827` 40% | `#000000` 55% | 详情面板背后的遮罩 |

**图表颜色** `ChartColors`（从 `CandleChart.kt` 的 `private object` 改为公开 `data class`，见第 5 节）：

| 字段 | 浅色 | 深色 | 说明 |
|---|---|---|---|
| `up` | `#E64B4B` | `#F0605D` | K 线和成交量柱，收盘价 ≥ 开盘价 |
| `down` | `#1F9D6C` | `#2EBD85` | 收盘价 < 开盘价。浅色从原来的 `#24A875` 略加深，对比度从约 3.0 提到 3.44 |
| `grid` | `#E2E7EE` | `#26303B` | 网格线（原 `#CBD5E1`） |
| `axisText` | `#5F6B7A` | `#98A3B3` | 坐标轴文字（原 `ChartColors.text`） |
| `crosshair` | `#64748B` | `#8A96A6` | 十字线（原来用的是 `ChartColors.text`） |
| `gapMarker` | `#8A94A3` | `#6E7A8A` | 缺口虚线（原来用的是 `ChartColors.text`） |
| `averages` | `#B45309`, `#2474C7`, `#8E44AD` | `#F39C4A`, `#4C9BE8`, `#B07CD8` | MA5、MA10、MA20。浅色 MA5 从 `#E67E22` 加深，否则作为文字对比度只有 2.85 |
| `panelBackground` | `#FFFFFF` 92% | `#171D24` 92% | 十字光标面板、坐标轴高亮标签底色 |
| `panelBorder` | `#E2E7EE` | `#2F3A47` | 面板描边 |
| `panelText` | `#374151` | `#C9D1DB` | 面板正文 |

**对比度**（WCAG 公式，在设计对话中用脚本计算，背景为 `background`）：
- 浅色：`textPrimary` 17.7，`textSecondary` 5.4，`upText` 4.9，`downText` 5.0，MA5/10/20 文字 5.0 / 4.8 / 5.9，横幅文字 7.5。
- 深色：`textPrimary` 15.3，`textSecondary` 7.2，`up` 5.8，`down` 7.7，MA5/10/20 8.5 / 6.3 / 5.9，横幅文字 9.3。
- 文字都达到 4.5:1。K 线图形（非文字）浅色 `up` 3.8、`down` 3.4，达到图形 3:1。
- 这些数字是设计阶段算的，**不要写进 README 当作实测结果**。

**关于红绿：** 状态颜色刻意避开红和绿，因为本 App 红涨绿跌，红色"已断开"会被误读成上涨。

**Material 主题映射：** `MaterialTheme(colorScheme = …)` 至少设置 `background`、`surface`、`onBackground`、`onSurface`（= `textPrimary`）、`primary`（= `accent`）、`outline`（= `divider`），让底部面板、按钮等 Material 组件和主题一致。

### 2.2 字号（sp）

所有字号用 `sp`，跟随系统字体缩放。数字一律开启等宽数字：`fontFeatureSettings = "tnum"`，避免价格跳动时文字左右抖动。

| 样式 | 字号 | 字重 | 用于 |
|---|---|---|---|
| `symbolTitle` | 18 | SemiBold | BTCUSDT |
| `symbolSubtitle` | 12 | Regular | Binance 现货 · 1分钟 · UTC |
| `priceLarge` | 30 | Bold，tnum | 最新价 |
| `priceChange` | 14 | Medium，tnum | +12.35  +0.02% |
| `caption` | 11 | Regular | 较上一根、更新于、脚注、统计标签 |
| `statValue` | 13 | Medium，tnum | 统计网格数值 |
| `maValue` | 12 | Regular，tnum | MA 数值栏 |
| `chip` | 12 | Medium | 状态胶囊 |
| `banner` | 13 | Regular（按钮 SemiBold） | 横幅 |
| `listHeader` | 11 | Regular | 列表表头 |
| `listRow` | 13 | Regular，tnum | 列表行 |
| `sheetTitle` | 16 | SemiBold | 详情面板标题 |
| `sheetBody` | 13 | Regular | 详情面板正文 |
| `mono` | 12 | `FontFamily.Monospace` | 原始状态字符串、URL |
| 图表内文字 | 10 | 不变 | 保持 `CandleChart.kt` 原样 |

字体用系统默认，不新增字体资源。

### 2.3 间距与形状（dp）

- 页面水平内边距：**12**（图表除外）。
- 图表：左 8，右 4。
- 区块之间的竖直间距：4 到 8。
- 圆角：状态胶囊 14（全圆），横幅 8，十字光标面板 6，坐标轴高亮标签 3，详情面板顶部 16。
- 所有可点击元素的点击区域至少 **48 × 48**（视觉尺寸可以更小，用 `minimumInteractiveComponentSize()` 或外层 `Box` 扩大）。

---

## 3. 组件规格

### 3.1 TopBar

- 高度 56 dp；`Row`，`verticalAlignment = CenterVertically`，起始内边距 12，末尾 0（按钮自带 48 dp 点击区）。
- 左侧 `Column(weight(1f))`：
  - 当前交易对（`symbolTitle`，`textPrimary`），不翻译。点击展开 BTCUSDT、ETHUSDT、SOLUSDT 下拉菜单；离线样例显示 BTCUSDT 并禁用选择。
  - 副标题（`symbolSubtitle`，`textSecondary`），中文"Binance 现货 · 1分钟 · UTC"，英文"Binance Spot · 1m · UTC"。单行，超长省略。
- 右侧依次：`StatusChip`、`ThemeToggleButton`。

### 3.2 StatusChip

- 外层点击区至少 48 × 48；内层胶囊高 28，水平内边距 10，`surfaceVariant` 底色，14 dp 圆角。
- 内容：8 dp 圆点 ＋ 6 dp 间距 ＋ 标签（`chip` 样式，`textPrimary`）。
- 已断开时圆点换成 12 dp 的圆圈感叹号（1.5 dp 描边，`statusError`），用 Canvas 画。
- 实时时圆点做呼吸动画：透明度 1.0 到 0.35，周期 1200 ms，无限循环。
- `malformed + invalid + unknown > 0` 时，胶囊右上角 6 dp 圆点角标，颜色 `statusWarning`。（`ignored` 是正常的重复或过期消息，不触发角标。）
- 点击打开 `ConnectionDetailsSheet`。
- 无障碍描述：例如"连接状态：重连中。点击查看详情"。

| 显示状态 | 中文标签 | 英文标签 | 圆点颜色 |
|---|---|---|---|
| Connecting | 连接中 | Connecting | `statusConnecting` |
| Synchronizing | 同步中 | Syncing | `accent` |
| Live | 实时 | Live | `accent`，呼吸 |
| Reconnecting | 重连中 | Reconnecting | `statusWarning` |
| Disconnected | 已断开 | Disconnected | `statusError`，感叹号图标 |
| 离线样例模式 | 离线样例 | Offline sample | `statusConnecting` |

### 3.3 ThemeToggleButton

- 48 × 48 点击区，中间 20 × 20 图标，颜色 `textSecondary`，Canvas 绘制（约 20 行，不加依赖）：
  - 当前浅色：画**月亮**（点了切到深色）。
  - 当前深色：画**太阳**（半径 4 dp 的圆环加 8 条短射线，描边 1.6 dp）。
- 无障碍描述："切换到深色模式" / "切换到浅色模式"。
- 状态：`var darkOverride by remember { mutableStateOf<Boolean?>(null) }`，`val dark = darkOverride ?: isSystemInDarkTheme()`，点击 `darkOverride = !dark`。不持久化。

### 3.4 PriceBlock

内边距：水平 12，上 4，下 4。

**价格列：**
1. 最新价：`priceLarge`。数值 = 最后一根 K 线的 `close`。颜色：
   - 实时，且相对上一根上涨：`upText`；下跌：`downText`；持平或无法比较：`textPrimary`。
   - **非实时（重连中、已断开、连接中但已有旧数据）：一律 `textSecondary`**，提示数据已停止更新。
   - 没有数据：显示 `--`，颜色 `textPrimary`。
2. 涨跌行：`priceChange` 显示"+12.35  +0.02%"（带符号，百分比两位小数），颜色规则同上；后接 8 dp 间距和 `caption` 样式"较上一根"（`textSecondary`）。上一根必须和最后一根时间连续（相差 60 000 ms），否则涨跌显示 `--`。这与现有十字光标面板的规则一致。
3. 说明行（`caption`，`textSecondary`），按优先级：
   - 离线模式："离线样例 · 非实时"
   - 实时："更新于 HH:mm:ss UTC"（`lastUpdate`）
   - 非实时且 `lastUpdate != null`："数据停在 HH:mm:ss UTC"
   - 没有数据："等待数据"

**统计网格**（2 列 × 2 行，列间距 16，行间距 6；每格上面是 `caption`/`textSecondary` 标签，下面是 `statValue`/`textPrimary` 数值）：

| 中文标签 | 英文标签 | 数值 |
|---|---|---|
| 区间最高 | Range high | 已加载 K 线的最高 `high` |
| 区间最低 | Range low | 已加载 K 线的最低 `low` |
| 区间成交量 {币种} | Range volume {asset} | 已加载 K 线 `volume` 之和；单位随交易对变化 |
| 区间时长 | Range span | 最后一根的 `openTime + 60 000` 减第一根的 `openTime`，格式见 4.4 |

数据为空时，数值都显示 `--`。

### 3.5 LoadedRangeNote

一行脚注（`caption`，`textTertiary`），水平内边距 12，允许换行：
- 实时数据：`区间 = 已加载 {N} 根 · {MM-dd HH:mm} 至 {MM-dd HH:mm} UTC`
- 离线模式：`区间 = 离线样例 {N} 根 · {MM-dd HH:mm} 至 {MM-dd HH:mm} UTC`
- 无数据：不显示。

起点是第一根的 `openTime`，终点是最后一根的 `openTime + 60 000`。这句话就是"统计不是 24 小时数据"的明确标注，**不能省略**。

### 3.6 MaRow

- `FlowRow`，水平间距 12，最小高度 24，水平内边距 12。
- 三项：`MA5 {值}`、`MA10 {值}`、`MA20 {值}`，`maValue` 样式，颜色分别是 `averages[0..2]`。
- 值 = `movingAverage(candles, period, lastIndex..lastIndex)` 的结果，和原 Canvas 图例的算法一致；算不出来显示 `--`。
- 实时数据且最后一根未收盘时，末尾追加一个 `caption`/`textTertiary` 的"含未收盘"（英文"incl. unclosed"）。离线模式下样例全部已收盘，不显示。

### 3.7 StatusBanner

- 外边距：水平 12，竖直 4。最小高度 40；1 dp 描边，8 dp 圆角。
- 内容 `Row`：左内边距 12，8 dp 圆点，8 dp 间距，文字（`banner` 样式，最多 3 行，`weight(1f)`），右侧文字按钮（最小 48 dp 高，水平内边距 10，SemiBold，带下划线）。
- 出现和消失用 `AnimatedVisibility(expandVertically() + fadeIn(), shrinkVertically() + fadeOut())`，时长 200 ms。
- 两种外观：
  - **警告**（琥珀）：实时数据源且状态不是 Live 时。按钮"查看离线样例"。
  - **信息**（中性灰）：离线模式。按钮"返回实时"。
- 实时数据源且状态是 Live：不显示横幅。

横幅文字见 4.3。

### 3.8 CandleChart 容器

- `CandleChart(candles = displayedCandles, colors = LocalKlineColors.current.chart, modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 8.dp, end = 4.dp))`
- 用 `key(dataSourceMode, displayedSymbol) { … }` 包住，切换数据源或交易对时重置视口和十字光标。这一步只在图表外面做，不改图表代码。图表上方标明成交量单位。
- 没有数据时：图表区显示骨架占位（5 条 `divider` 色网格线，十几个 `surfaceVariant` 色的圆角矩形），下方居中"等待第一批数据…"（`caption`，`textSecondary`）。骨架是图表外的独立 Composable，不进 `CandleChart`。

### 3.9 RecentCandleList

- 高度 = `listHeight`（1.1 节）。
- 表头 28 dp，水平内边距 12，`listHeader`/`textSecondary`，单行不换行，下方 0.5 dp `divider`：

  | 列 | 中文 | 英文 | 权重 | 对齐 |
  |---|---|---|---|---|
  | 1 | 时间 UTC | Time UTC | 1.1 | 左 |
  | 2 | 收盘价 | Close | 1.2 | 右 |
  | 3 | 涨跌幅 | Change | 1.0 | 右 |
  | 4 | 成交量 | Volume | 0.9 | 右 |

- 行高 36，`LazyColumn`，下方 0.5 dp `divider`。
  - 时间：该 K 线的 `openTime`，格式 `HH:mm`，`textSecondary`。
  - 收盘价：`textPrimary`。
  - 涨跌幅：相对列表中前一根（时间上更早的那根）的收盘价；时间不连续显示 `--`（`textTertiary`）；正数 `upText`，负数 `downText`，零 `textPrimary`。
  - 成交量：`textSecondary`。
- 数据：只取 `isClosed == true` 的 K 线，取最近 20 根，最新的在最上面。
- 行**不可点击**，不加任何点击效果。
- 空状态："暂无已收盘的 K 线"（`listRow` 字号，`textTertiary`），左对齐，上内边距 16。

### 3.10 ConnectionDetailsSheet

- Material3 `ModalBottomSheet`（需要 `@OptIn(ExperimentalMaterial3Api::class)`）。如果它在 Desktop 上不可用或表现异常，Desktop 改用居中的 `Dialog` 显示同样的内容，并在报告里写明。
- 内边距：水平 16，底部 28。标题"连接详情"（`sheetTitle`）。
- 行布局：左边 72 dp 宽标签（`textSecondary`），右边内容（`textPrimary`），每行上下 7 dp：

| 标签 | 内容 |
|---|---|
| 状态 | 一个不可点击的 `StatusChip` 副本 |
| 说明 | 与横幅相同的那句话；实时时显示"实时数据正常" |
| 原始状态 | `state.status` 原文，`mono` 样式，可选择复制（`SelectionContainer`），**不翻译** |
| 最后更新 | `yyyy-MM-dd HH:mm:ss UTC`，没有就 `--` |
| WebSocket | WebSocket 地址，`mono` |
| REST | REST 地址，`mono` |
| 已加载 | `{N} 根 · {起} 至 {止} UTC` |

- 小标题"消息统计（自启动以来）"，下面 4 列等宽：格式错误 `malformed`、校验失败 `invalid`、未知类型 `unknown`、已忽略 `ignored`。数值 `statValue`，标签 `caption`。
- 底部脚注（`caption`，`textTertiary`）："所有时间均为 UTC。仅展示行情数据，不提供交易功能。"
- 离线模式下：状态显示"离线样例"，原始状态一行显示当前实时连接的原始状态（实时连接仍在后台运行），数据源显示"离线样例 btcusdt_1m_sample.csv"，已加载显示样例的根数和时间范围，消息统计仍显示实时连接的计数并在小标题后加"（实时连接）"。

**URL 的来源：** 使用当前实时交易对生成实际使用的 WebSocket 和 REST K 线地址，并在详情面板显示。离线时仍显示后台实时连接的地址。

---

## 4. 纯函数和数据映射

全部放在 `commonMain` 的 `ui/model/` 下（包名 `io.github.yudelieyang.kline.ui.model`），不依赖 Compose，全部写单元测试（`commonTest`）。

### 4.1 状态解析

```kotlin
enum class ConnectionPhase { Connecting, Synchronizing, Live, Reconnecting, Disconnected }

enum class DisconnectReason {
    TimeGap, Overflow, Resync, RestTimeout, RestError,
    SocketClosed, ConnectTimeout, ServerRestart, WrongStream, DataConflict, Other
}

data class ConnectionUi(
    val phase: ConnectionPhase,
    val attempt: Int?,              // 第几次重连；没有就是 null
    val reason: DisconnectReason?,  // 断开原因；没有就是 null
    val raw: String,                // 原始字符串，给详情面板用
)

fun parseConnectionStatus(raw: String): ConnectionUi
```

解析步骤：
1. 先用正则 `; retry (\d+)$` 去掉**末尾**的重试后缀，记下数字 `retry`。注意必须锚定在末尾：原因文本本身可能含分号，例如 `Older candle remains unclosed; resynchronization required`。
2. 对剩下的 `base` 匹配：

| base | phase | attempt | reason |
|---|---|---|---|
| `Connecting` | Connecting | null | null |
| `Synchronizing` | Synchronizing | null | null |
| `Live` | Live | null | null |
| `Reconnecting (N)` | Reconnecting | N | null |
| `Server restarting; reconnecting` | Reconnecting | null | ServerRestart |
| `Connection timed out` | Disconnected | null | ConnectTimeout |
| `Disconnected: <msg>` | Disconnected | null | `classifyReason(msg)` |
| 其他任何字符串 | Disconnected | null | Other |

3. 如果第 1 步拿到了 `retry`：`phase = Reconnecting`，`attempt = retry`，`reason` 保留第 2 步的结果。

`classifyReason(msg)`（按顺序，第一个命中的生效）：

| 条件 | reason |
|---|---|
| 包含 `time gap` | TimeGap |
| 包含 `overflow`（不区分大小写） | Overflow |
| 包含 `resynchronization` | Resync |
| 等于 `REST request timed out` | RestTimeout |
| 以 `REST:` 开头 | RestError |
| 等于 `WebSocket closed` 或 `WebSocket error` | SocketClosed |
| 等于 `Unexpected symbol or interval` | WrongStream |
| 完全等于 `Closed candle has fewer trades than an open version` 或 `Equal versions have different candle values` | DataConflict |
| 其他（包括 `Connection error`、`Network unavailable`、各种异常消息） | Other |

**实现前必须做一件事：** 在 `data` 和 `state` 包里全局搜索所有 `error("…")`、`IllegalStateException("…")` 和 `MergeResult.Conflict(...)` 的原因字符串，确认上表覆盖了所有已知原因；不在表里的原因归入 Other，并在 UI_PLAN.md 里列出来给 Jun 看。

### 4.2 显示用的派生数据

```kotlin
data class PriceChange(val price: Double, val change: Double?, val percent: Double?)
fun lastPriceChange(candles: List<Candle>): PriceChange?            // 空列表返回 null

data class LoadedStats(
    val count: Int, val startTime: Long, val endTime: Long,         // endTime = 最后一根 openTime + 60_000
    val high: Double, val low: Double, val volume: Double,
)
fun loadedStats(candles: List<Candle>): LoadedStats?                // 空列表返回 null

data class RecentRow(val openTime: Long, val close: Double, val percent: Double?, val volume: Double)
fun recentClosedRows(candles: List<Candle>, limit: Int = 20): List<RecentRow>   // 最新在前

fun movingAverageSnapshot(candles: List<Candle>): List<Double?>     // [MA5, MA10, MA20] 的最新值，复用 indicators.movingAverage
```

规则：
- "相对上一根"的前一根必须时间连续（`openTime` 相差 60 000），否则 `change`、`percent` 为 null。百分比非有限值（比如上一根收盘价为 0）时为 null。
- `recentClosedRows` 的涨跌幅也按这条规则：比较对象是**原列表**中紧挨着的前一根，要求时间连续。第 20 根（列表最底下那根）的前一根不在显示范围内，也要正常计算，不能显示 `--`。测试覆盖：普通情况、缺口、少于 20 根、最后一根未收盘被排除。

### 4.3 横幅文字

```kotlin
sealed interface BannerUi {
    data class Warning(val connection: ConnectionUi, val stoppedAt: Long?, val hasData: Boolean) : BannerUi
    data class Offline(val startTime: Long, val endTime: Long) : BannerUi
}
fun bannerFor(mode: DataSourceMode, connection: ConnectionUi, lastUpdate: Long?, hasData: Boolean, offline: LoadedStats?): BannerUi?
```

`bannerFor` 保持纯函数；离线加载中和加载失败由 UI 层单独显示信息样式横幅，不构造虚假的离线时间范围。

文字由 UI 层按 `BannerUi` 从资源文件里拼。中文模板（英文见第 6 节）：

| 情况 | 文字 |
|---|---|
| Connecting，无数据 | 正在连接 Binance… |
| Connecting，有数据 | 正在重新连接。{数据停在} |
| Synchronizing，无数据 | 正在同步历史 K 线… |
| Synchronizing，有数据 | 正在重新同步数据。{数据停在} |
| Reconnecting，有 attempt | {原因}，正在第 {n} 次重连。{数据停在} |
| Reconnecting，ServerRestart，无 attempt | 服务器重启中，正在重新连接。{数据停在} |
| Reconnecting，无 attempt 且 reason 不是 ServerRestart | 正在重新连接。{数据停在} |
| Disconnected | {原因}。{数据停在} |
| 离线模式 | 正在查看离线样例（非实时）。样例时间 {MM-dd HH:mm} 至 {MM-dd HH:mm} UTC。 |
| 离线样例加载中 | 正在加载离线样例…（信息样式，按钮为"返回实时"，图表显示骨架） |
| 离线样例加载失败 | 离线样例加载失败。（信息样式，按钮为"返回实时"） |

`{数据停在}` = `数据停在 HH:mm:ss UTC。`，`lastUpdate` 为 null 时省略。

`{原因}`：

| reason | 中文 |
|---|---|
| null | 连接中断 |
| TimeGap | 数据出现时间缺口 |
| Overflow | 消息积压 |
| Resync | 数据需要重新同步 |
| RestTimeout | 历史数据请求超时 |
| RestError | 历史数据请求失败 |
| SocketClosed | 连接被关闭 |
| ConnectTimeout | 连接超时 |
| ServerRestart | 服务器重启中 |
| WrongStream | 收到了不匹配的数据流 |
| DataConflict | 收到相互矛盾的数据 |
| Other | 连接已断开 |

### 4.4 格式化

- **价格和成交量：** 复用图表里现有的 `priceLabel`、`volumeLabel`，保证图表内外显示一致。如果它们是 `private`，移到一个 `internal` 的格式化文件里，**行为不变**，原有测试必须照常通过。
- **数字格式不跟随系统语言**：始终用小数点，不加千分位。
- **涨跌：** 带符号，`+12.35`、`-19.26`；百分比两位小数并带 `%`。
- **时间：** 全部 UTC。`HH:mm:ss`、`HH:mm`、`MM-dd HH:mm`、`yyyy-MM-dd HH:mm:ss`。复用 `App.kt` 现有的 `utcTime` 思路，写成纯函数并测试跨日的情况。
- **时长**（`formatSpan`）：返回天、小时、分钟三个整数，由 UI 层拼文字。中文"8小时22分"、"1天2小时5分"；英文"8h 22m"、"1d 2h 5m"。为 0 的高位省略（不写"0天"），分钟始终显示。注意：实时列表没有裁剪（M3b 未做），长时间运行后可能超过 24 小时，所以必须支持"天"。

---

## 5. 对 CandleChart.kt 的改动（完整清单，不得超出）

1. `private object ChartColors` 改为公开的 `data class ChartColors(up, down, grid, axisText, crosshair, gapMarker, averages: List<Color>, panelBackground, panelBorder, panelText)`，浅色和深色两个实例定义在主题文件里。
2. `CandleChart` 加参数：`fun CandleChart(candles: List<Candle>, modifier: Modifier = Modifier, colors: ChartColors)`。函数体内所有 `ChartColors.x` 改为 `colors.x`，对应关系：
   - `ChartColors.text` 用作坐标轴文字时 改 `colors.axisText`；用于缺口虚线时 改 `colors.gapMarker`；用于十字线时 改 `colors.crosshair`；用于面板正文时 改 `colors.panelText`。
3. 三处 `Color.White`（面板背景、价格高亮标签背景、时间高亮标签背景）改为 `colors.panelBackground`。
4. 十字光标面板：`drawRect` 换成 `drawRoundRect(cornerRadius = CornerRadius(6.dp.toPx()))` 填充 `panelBackground`，再画一次 `Stroke(1.dp.toPx())` 的 `panelBorder` 描边。价格和时间高亮标签同理，圆角 3 dp。
5. **删除** MA 图例的绘制代码（`var legendX = …` 到图例 `for` 循环结束，原文件约 417 到 429 行）。MA 线本身的绘制保留。

**不改：** 手势（所有 `pointerInput`）、视口计算、可见区间、`selectAt`、十字线位置逻辑、面板内容和位置规则、坐标轴、主图和副图 3:1 比例、字号 10 sp。

如果实现时发现必须改动以上清单之外的图表代码，**停下来先问 Jun**。

---

## 6. 文字资源（中英）

放在 `shared/src/commonMain/composeResources/values/strings.xml`（英文，默认）和 `values-zh/strings.xml`（中文），用 `stringResource(Res.string.…)` 读取。不翻译的：`BTCUSDT`、`Binance`、`UTC`、`MA5/10/20`、原始状态字符串、URL。

| key | 英文 | 中文 |
|---|---|---|
| `subtitle` | Binance Spot · 1m · UTC | Binance 现货 · 1分钟 · UTC |
| `chip_connecting` | Connecting | 连接中 |
| `chip_syncing` | Syncing | 同步中 |
| `chip_live` | Live | 实时 |
| `chip_reconnecting` | Reconnecting | 重连中 |
| `chip_disconnected` | Disconnected | 已断开 |
| `chip_offline` | Offline sample | 离线样例 |
| `vs_previous` | vs prev. candle | 较上一根 |
| `updated_at` | Updated %1$s UTC | 更新于 %1$s UTC |
| `stopped_at` | Data stopped at %1$s UTC. | 数据停在 %1$s UTC。 |
| `waiting_data` | Waiting for data | 等待数据 |
| `offline_not_live` | Offline sample · not live | 离线样例 · 非实时 |
| `stat_high` | Range high | 区间最高 |
| `stat_low` | Range low | 区间最低 |
| `stat_volume` | Range volume %1$s | 区间成交量 %1$s |
| `stat_span` | Range span | 区间时长 |
| `range_note_live` | Range = %1$d loaded candles · %2$s to %3$s UTC | 区间 = 已加载 %1$d 根 · %2$s 至 %3$s UTC |
| `range_note_offline` | Range = %1$d offline sample candles · %2$s to %3$s UTC | 区间 = 离线样例 %1$d 根 · %2$s 至 %3$s UTC |
| `ma_unclosed` | incl. unclosed | 含未收盘 |
| `banner_connecting` | Connecting to Binance… | 正在连接 Binance… |
| `banner_reconnecting` | Reconnecting. | 正在重新连接。 |
| `banner_syncing` | Syncing candle history… | 正在同步历史 K 线… |
| `banner_resyncing` | Resynchronizing data. | 正在重新同步数据。 |
| `banner_retry` | %1$s. Reconnect attempt %2$d. | %1$s，正在第 %2$d 次重连。 |
| `banner_server_restart` | Server restarting. Reconnecting. | 服务器重启中，正在重新连接。 |
| `banner_disconnected` | %1$s. | %1$s。 |
| `banner_offline` | Viewing offline sample (not live). Sample time %1$s to %2$s UTC. | 正在查看离线样例（非实时）。样例时间 %1$s 至 %2$s UTC。 |
| `banner_offline_loading` | Loading offline sample… | 正在加载离线样例… |
| `banner_offline_failed` | Could not load the offline sample. | 离线样例加载失败。 |
| `action_view_offline` | View offline sample | 查看离线样例 |
| `action_back_live` | Back to live | 返回实时 |
| `reason_none` | Connection lost | 连接中断 |
| `reason_time_gap` | Data has a time gap | 数据出现时间缺口 |
| `reason_overflow` | Message backlog | 消息积压 |
| `reason_resync` | Data needs resynchronizing | 数据需要重新同步 |
| `reason_rest_timeout` | History request timed out | 历史数据请求超时 |
| `reason_rest_error` | History request failed | 历史数据请求失败 |
| `reason_socket_closed` | Connection closed | 连接被关闭 |
| `reason_connect_timeout` | Connection timed out | 连接超时 |
| `reason_server_restart` | Server restarting | 服务器重启中 |
| `reason_wrong_stream` | Unexpected data stream | 收到了不匹配的数据流 |
| `reason_data_conflict` | Conflicting data received | 收到相互矛盾的数据 |
| `reason_other` | Disconnected | 连接已断开 |
| `list_time` | Time UTC | 时间 UTC |
| `list_close` | Close | 收盘价 |
| `list_change` | Change | 涨跌幅 |
| `list_volume` | Volume | 成交量 |
| `list_empty` | No closed candles yet | 暂无已收盘的 K 线 |
| `chart_waiting` | Waiting for the first data… | 等待第一批数据… |
| `sheet_title` | Connection details | 连接详情 |
| `sheet_status` | Status | 状态 |
| `sheet_detail` | Detail | 说明 |
| `sheet_live_ok` | Live data is flowing normally | 实时数据正常 |
| `sheet_raw` | Raw status | 原始状态 |
| `sheet_last_update` | Last update | 最后更新 |
| `sheet_loaded` | Loaded | 已加载 |
| `sheet_loaded_value` | %1$d candles · %2$s to %3$s UTC | %1$d 根 · %2$s 至 %3$s UTC |
| `sheet_counts` | Message counts (since start) | 消息统计（自启动以来） |
| `sheet_counts_live_suffix` | (live connection) | （实时连接） |
| `count_malformed` | Malformed | 格式错误 |
| `count_invalid` | Invalid | 校验失败 |
| `count_unknown` | Unknown | 未知类型 |
| `count_ignored` | Ignored | 已忽略 |
| `sheet_source_offline` | Offline sample btcusdt_1m_sample.csv | 离线样例 btcusdt_1m_sample.csv |
| `sheet_footer` | All times are UTC. Market data display only; no trading. | 所有时间均为 UTC。仅展示行情数据，不提供交易功能。 |
| `span_dhm` / `span_hm` / `span_m` | %1$dd %2$dh %3$dm / %1$dh %2$dm / %1$dm | %1$d天%2$d小时%3$d分 / %1$d小时%2$d分 / %1$d分 |
| `cd_status_chip` | Connection status: %1$s. Tap for details | 连接状态：%1$s。点击查看详情 |
| `cd_to_dark` / `cd_to_light` | Switch to dark mode / Switch to light mode | 切换到深色模式 / 切换到浅色模式 |

**Desktop 语言：** 资源按 JVM 默认 `Locale` 选择。验证中文时用 `-Duser.language=zh -Duser.country=CN` 启动，具体做法由 Codex 按 Compose Multiplatform 官方文档确认。Windows 上要确认中文不出现方块字。

---

## 7. 平台改动

### 7.1 Android

- **锁定竖屏：** `AndroidManifest.xml` 的 activity 加 `android:screenOrientation="portrait"`。说明：Android 16 在大屏设备（最小宽度 ≥ 600 dp）上可能忽略方向限制，手机不受影响，README 的已知限制里写一句即可。
- **系统栏图标颜色：** 手动切到深色而系统是浅色时，状态栏图标会和背景同色看不见。`App` 加参数 `onDarkThemeChange: (Boolean) -> Unit = {}`，在 `LaunchedEffect(dark)` 里回调；`MainActivity` 收到后重新调用 `enableEdgeToEdge(statusBarStyle = …, navigationBarStyle = …)`，浅色用 `SystemBarStyle.light(…)`，深色用 `SystemBarStyle.dark(…)`，参数按 androidx.activity 1.13 的官方文档确认。
- Manifest 的 `android:theme` 保持不动（启动瞬间可能闪一下白底，记入已知限制）。

### 7.2 Desktop

- 窗口初始尺寸 `DpSize(480.dp, 900.dp)`，标题保持 `Kline_KMP_App`。
- 深色：`isSystemInDarkTheme()` 能否读到 Windows 设置以实测为准；读不到也没关系，手动按钮可以切换。在报告里写明实测结果。

---

## 8. 离线样例模式

```kotlin
enum class DataSourceMode { Live, Offline }
var mode by remember { mutableStateOf(DataSourceMode.Live) }
```

- 第一次切到离线时，用 `CsvCandleSource("files/btcusdt_1m_sample.csv", <1 分钟的 Interval>).loadHistory()` 读取（路径和 `Interval` 的写法以现有测试里的用法为准）；结果用 `remember` 缓存，之后不再重复读取。读取失败时不崩溃，显示"离线样例加载失败"横幅，按钮仍为"返回实时"。
- 离线模式下，图表、价格区、统计、MA、列表全部使用 CSV 数据；状态胶囊显示"离线样例"。
- **实时连接在后台照常运行**，`LiveMarketHolder` 不暂停、不重建。点"返回实时"立即切回 `state.candles`。
- 横幅时间范围、脚注都由 CSV 数据实际计算，不能写死。

---

## 9. 不做的事（防止范围蔓延）

- 不接 24h 行情、不加周期切换、不加深度图和成交明细。
- 不放任何点了没反应的按钮或标签页；列表行不可点击。
- 不加买入、卖出、账户、自选等元素。
- 不加新依赖（Material Icons、DataStore、字体文件都不加）。
- 交易对切换所需的 `LiveMarket.kt` 和 `data` 包参数化除外，不改变合并逻辑或 `indicators` 包逻辑。
- 不支持横屏布局，不宣称支持 iOS。
