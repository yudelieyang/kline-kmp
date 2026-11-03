package io.github.yudelieyang.kline

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.yudelieyang.kline.chart.CandleChart
import io.github.yudelieyang.kline.data.*
import io.github.yudelieyang.kline.domain.Candle
import io.github.yudelieyang.kline.domain.Interval
import io.github.yudelieyang.kline.state.LiveMarketHolder
import io.github.yudelieyang.kline.ui.*
import io.github.yudelieyang.kline.ui.model.*
import io.github.yudelieyang.kline.ui.theme.*
import kotlinx.coroutines.CancellationException
import kline_kmp_app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private const val LIVE_WS_BASE = "wss://data-stream.binance.vision:443"
private const val LIVE_REST_URL = "https://data-api.binance.vision"

@Composable
fun App(onDarkThemeChange: (Boolean) -> Unit = {}) {
    var symbol by remember { mutableStateOf(MarketSymbol.BTCUSDT) }
    val holder = remember(symbol) { LiveMarketHolder(wsUrl = websocketUrl(LIVE_WS_BASE, symbol), restUrl = LIVE_REST_URL, symbol = symbol) }
    val state = key(holder) { holder.state.collectAsState().value }
    LaunchedEffect(holder) { holder.run() }
    var darkOverride by remember { mutableStateOf<Boolean?>(null) }
    val dark = darkOverride ?: isSystemInDarkTheme()
    LaunchedEffect(dark) { onDarkThemeChange(dark) }
    var mode by remember { mutableStateOf(DataSourceMode.Live) }
    val displayedSymbol = if (mode == DataSourceMode.Offline) MarketSymbol.BTCUSDT else symbol
    var sample by remember { mutableStateOf<List<Candle>?>(null) }
    var sampleFailed by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    LaunchedEffect(mode) {
        if (mode == DataSourceMode.Offline && sample == null) {
            sampleFailed = false
            try { sample = CsvCandleSource("files/btcusdt_1m_sample.csv", Interval.ONE_MINUTE).loadHistory() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { sampleFailed = true }
        }
    }
    val connection = remember(state.status) { parseConnectionStatus(state.status) }
    val candles = if (mode == DataSourceMode.Live) state.candles else sample.orEmpty()
    val stats = remember(candles) { loadedStats(candles) }
    val change = remember(candles) { lastPriceChange(candles) }
    val averages = remember(candles) { movingAverageSnapshot(candles) }
    val rows = remember(candles) { recentClosedRows(candles) }
    val banner = bannerFor(mode, connection, state.lastUpdate, candles.isNotEmpty(), stats)
    val offlineState = offlineLoadState(loaded = sample != null, failed = sampleFailed)
    KlineTheme(dark) {
        val colors = LocalKlineColors.current
        val density = LocalDensity.current
        val detail = when {
            mode == DataSourceMode.Offline && offlineState != OfflineLoadState.Ready -> stringResource(
                if (offlineState == OfflineLoadState.Failed) Res.string.banner_offline_failed else Res.string.banner_offline_loading
            )
            banner is BannerUi.Offline -> stringResource(Res.string.banner_offline, utcShortDateTime(banner.startTime), utcShortDateTime(banner.endTime))
            banner is BannerUi.Warning -> warningText(banner)
            else -> stringResource(Res.string.sheet_live_ok)
        }
        val bannerText = if (mode == DataSourceMode.Offline || banner != null) detail else null
        BoxWithConstraints(Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
            val compact = maxWidth < 372.dp || density.fontScale > 1.15f
            val baseListHeight = if (density.fontScale > 1.15f) 100.dp else (maxHeight * .22f).coerceIn(100.dp, 172.dp)
            var bannerHeight by remember { mutableStateOf(0.dp) }
            val listHeight = (baseListHeight - bannerHeight).coerceAtLeast(64.dp)
            Column(Modifier.fillMaxSize()) {
                TopBar(mode, connection.phase, state.malformed + state.invalid + state.unknown > 0, dark, displayedSymbol, { symbol = it }, { details = true }, { darkOverride = !dark })
                PriceBlock(change, stats, mode, connection.phase, state.lastUpdate, compact, displayedSymbol.baseAsset)
                LoadedRangeNote(stats, mode)
                MaRow(averages, mode == DataSourceMode.Live && candles.lastOrNull()?.isClosed == false)
                StatusBanner(bannerText, mode == DataSourceMode.Offline, {
                    mode = if (mode == DataSourceMode.Live) DataSourceMode.Offline else DataSourceMode.Live
                }, Modifier.onSizeChanged { bannerHeight = with(density) { it.height.toDp() } })
                Text(stringResource(Res.string.chart_volume, displayedSymbol.baseAsset), Modifier.padding(start = 12.dp),
                    style = KlineTypography.caption, color = colors.textSecondary)
                key(mode, displayedSymbol) {
                    val chartModifier = Modifier.weight(1f).fillMaxWidth().padding(start = 8.dp, end = 4.dp)
                    if (candles.isEmpty()) ChartPlaceholder(chartModifier) else CandleChart(candles, chartModifier, colors.chart)
                }
                HorizontalDivider(thickness = 1.dp, color = colors.divider)
                RecentCandleList(rows, listHeight, displayedSymbol.baseAsset)
            }
        }
        if (details) ConnectionDetailsSheet(mode, connection, state, stats, detail, websocketUrl(LIVE_WS_BASE, symbol), snapshotUrl(LIVE_REST_URL, symbol)) { details = false }
    }
}
