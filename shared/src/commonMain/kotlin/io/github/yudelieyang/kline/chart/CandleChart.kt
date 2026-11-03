package io.github.yudelieyang.kline.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yudelieyang.kline.domain.Candle
import io.github.yudelieyang.kline.indicators.movingAverage
import kotlin.math.abs
import kotlin.math.pow

data class ChartColors(
    val up: Color, val down: Color, val grid: Color, val axisText: Color,
    val crosshair: Color, val gapMarker: Color, val averages: List<Color>,
    val panelBackground: Color, val panelBorder: Color, val panelText: Color,
)

private data class CrosshairSelection(val openTime: Long, val price: Double)

@Composable
fun CandleChart(candles: List<Candle>, modifier: Modifier = Modifier, colors: ChartColors, initialSlotDp: Int = 8) {
    val density = LocalDensity.current.density
    val measurer = rememberTextMeasurer()
    val style = TextStyle(color = colors.axisText, fontSize = 10.sp)
    var size by remember { mutableStateOf(IntSize.Zero) }
    var viewport by remember(density, initialSlotDp) { mutableStateOf(ChartViewport(0f, initialSlotDp * density)) }
    var previousCandles by remember { mutableStateOf(candles) }
    var touchCrosshair by remember { mutableStateOf<CrosshairSelection?>(null) }
    var movingCrosshair by remember { mutableStateOf(false) }
    var longPressCount by remember { mutableStateOf(0L) }
    var touchInput by remember { mutableStateOf(false) }
    var multiTouchGesture by remember { mutableStateOf(false) }
    var mousePosition by remember { mutableStateOf<Offset?>(null) }
    val axisWidth = remember(candles, size.width, density) {
        if (candles.isEmpty()) 0f else {
            val low = candles.minOf { it.low }
            val high = candles.maxOf { it.high }
            val labelWidth = maxOf(measurer.measure(priceLabel(low), style).size.width, measurer.measure(priceLabel(high), style).size.width)
            (labelWidth + 12f * density).coerceAtMost(size.width * 0.3f)
        }
    }
    val plot = plotWidth(size.width.toFloat(), axisWidth)
    LaunchedEffect(candles, plot) {
        viewport = preserveAnchor(previousCandles, candles, viewport, plot)
        previousCandles = candles
    }
    val gap = 8f * density
    val timeHeight = 24f * density

    fun selectAt(position: Offset): CrosshairSelection? {
        val view = viewport.clamp(candles.size, plot)
        val visible = visibleRange(candles.size, plot, view.slot, view.offset)
        if (visible.isEmpty() || position.y < 0f || plot <= 0f) return null
        val bottom = (size.height - timeHeight - gap) * 0.75f
        if (bottom <= 0f || position.y > bottom) return null
        val points = (visible.first - 1).coerceAtLeast(0)..(visible.last + 1).coerceAtMost(candles.lastIndex)
        val values = listOf(5, 10, 20).flatMap { period ->
            visibleMovingAverageValues(candles, movingAverage(candles, period, points), visible, plot, view)
        }
        val prices = visiblePriceRange(candles, visible, values) ?: return null
        val index = nearestCandleIndex(position.x, candles.size, plot, view) ?: return null
        return CrosshairSelection(candles[index].openTime, yToPrice(position.y, prices, 0f, bottom))
    }
    val currentSelectAt by rememberUpdatedState<(Offset) -> CrosshairSelection?> { position -> selectAt(position) }
    // Keep gesture handlers active while reading the latest chart dimensions and data size.
    val currentCandleCount by rememberUpdatedState(candles.size)
    val currentPlot by rememberUpdatedState(plot)
    val currentDensity by rememberUpdatedState(density)
    LaunchedEffect(candles, viewport, plot) {
        val selected = touchCrosshair ?: return@LaunchedEffect
        val index = candles.indexOfFirst { it.openTime == selected.openTime }
        if (index < 0 || (plot > 0f && visibleRange(candles.size, plot, viewport.slot, viewport.clamp(candles.size, plot).offset).let { index !in it })) {
            touchCrosshair = null
        }
    }

    Canvas(modifier.fillMaxSize().onSizeChanged { size = it }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                val pressCount = longPressCount
                if (waitForUpOrCancellation() != null && touchCrosshair != null && longPressCount == pressCount) touchCrosshair = null
            }
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.isEmpty()) multiTouchGesture = false
                    else {
                        touchInput = pressed.first().type == PointerType.Touch
                        if (pressed.size > 1) {
                            multiTouchGesture = true
                            movingCrosshair = false
                        }
                    }
                    val mouse = event.changes.firstOrNull { it.type == PointerType.Mouse }
                    if (event.type == PointerEventType.Exit || event.buttons.isPrimaryPressed) mousePosition = null
                    else if (mouse != null) mousePosition = mouse.position
                }
            }
        }
        .pointerInput(Unit) {
            detectTransformGestures { centroid, pan, zoom, _ ->
                val width = currentPlot
                val count = currentCandleCount
                val scale = currentDensity
                if (width > 0f && !movingCrosshair) {
                    viewport = viewport.zoom(zoom, centroid.x, count, width, 2f * scale, 40f * scale)
                        .pan(pan.x, count, width)
                }
            }
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val width = currentPlot
                    if (event.type == PointerEventType.Scroll && width > 0f) {
                        val change = event.changes.firstOrNull() ?: continue
                        val factor = 1.1f.pow(-change.scrollDelta.y)
                        val scale = currentDensity
                        viewport = viewport.zoom(factor, change.position.x, currentCandleCount, width, 2f * scale, 40f * scale)
                        change.consume()
                    }
                }
            }
        }
        .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { position ->
                    if (touchInput && !multiTouchGesture) {
                        longPressCount++
                        touchCrosshair = currentSelectAt(position)
                        movingCrosshair = touchCrosshair != null
                    }
                },
                onDragEnd = { movingCrosshair = false },
                onDragCancel = { movingCrosshair = false },
                onDrag = { change, _ ->
                    if (movingCrosshair && !multiTouchGesture) {
                        currentSelectAt(change.position)?.let { touchCrosshair = it }
                        change.consume()
                    }
                },
            )
        }) {
        if (candles.isEmpty() || plot <= 0f || size.height <= timeHeight + gap) return@Canvas
        val current = viewport.clamp(candles.size, plot)
        val range = visibleRange(candles.size, plot, current.slot, current.offset)
        if (range.isEmpty()) return@Canvas
        val periods = listOf(5, 10, 20)
        val pointRange = (range.first - 1).coerceAtLeast(0)..(range.last + 1).coerceAtMost(candles.lastIndex)
        val averages = periods.map { movingAverage(candles, it, pointRange) }
        val visibleValues = averages.flatMap { visibleMovingAverageValues(candles, it, range, plot, current) }
        val prices = visiblePriceRange(candles, range, visibleValues) ?: return@Canvas
        val panels = size.height - timeHeight - gap
        val mainBottom = panels * 0.75f
        val volumeTop = mainBottom + gap
        val volumeBottom = size.height - timeHeight
        val volumeMax = range.maxOf { candles[it].volume }
        val axisConstraints = Constraints(maxWidth = (axisWidth - 4f * density).coerceAtLeast(0f).toInt())

        for (i in 0..4) {
            val y = mainBottom * i / 4f
            drawLine(colors.grid, Offset(0f, y), Offset(plot, y), 0.5f * density)
            val fraction = i / 4.0
            val price = prices.max * (1.0 - fraction) + prices.min * fraction
            var label = priceLabel(price)
            var measured = measurer.measure(label, style)
            if (measured.size.width > axisWidth - 4f * density) {
                label = scientific(price)
                measured = measurer.measure(label, style, overflow = TextOverflow.Ellipsis, softWrap = false, maxLines = 1, constraints = axisConstraints)
            }
            val labelY = (y - measured.size.height / 2f).coerceIn(0f, (mainBottom - measured.size.height).coerceAtLeast(0f))
            clipRect(left = plot, right = size.width.toFloat()) {
                drawText(measured, topLeft = Offset(plot + 4f * density, labelY))
            }
        }

        clipRect(left = 0f, top = 0f, right = plot, bottom = volumeBottom) {
            for (i in gapMarkerIndices(candles, range)) {
                val x = candleCenter(i, candles.size, plot, current.slot, current.offset) - current.slot / 2f
                drawLine(colors.gapMarker, Offset(x, 0f), Offset(x, volumeBottom), density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * density, 4f * density)))
            }
            for (i in range) {
                val candle = candles[i]
                val x = candleCenter(i, candles.size, plot, current.slot, current.offset)
                val color = if (candle.close >= candle.open) colors.up else colors.down
                val high = priceToY(candle.high, prices, 0f, mainBottom)
                val low = priceToY(candle.low, prices, 0f, mainBottom)
                val open = priceToY(candle.open, prices, 0f, mainBottom)
                val close = priceToY(candle.close, prices, 0f, mainBottom)
                val bodyWidth = current.slot * 0.7f
                drawLine(color, Offset(x, high), Offset(x, low), maxOf(density, 1f))
                val bodyTop = minOf(open, close)
                val bodyHeight = abs(open - close)
                if (bodyHeight < density) drawLine(color, Offset(x - bodyWidth / 2, bodyTop), Offset(x + bodyWidth / 2, bodyTop), density)
                else drawRect(color, Offset(x - bodyWidth / 2, bodyTop), Size(bodyWidth, bodyHeight))
                if (volumeMax > 0 && candle.volume > 0) {
                    val height = ((candle.volume / volumeMax) * (volumeBottom - volumeTop)).toFloat()
                    drawRect(color, Offset(x - bodyWidth / 2, volumeBottom - height), Size(bodyWidth, height))
                }
            }
        }
        clipRect(left = 0f, top = 0f, right = plot, bottom = mainBottom) {
            for ((series, values) in averages.withIndex()) {
                for (segment in visibleMovingAverageSegments(candles, values, range, plot, current)) {
                    drawLine(colors.averages[series], Offset(segment.x0, priceToY(segment.value0, prices, 0f, mainBottom)), Offset(segment.x1, priceToY(segment.value1, prices, 0f, mainBottom)), 1.5f * density)
                }
            }

        }
        clipRect(left = plot, right = size.width.toFloat()) {
            val volume = measurer.measure(volumeLabel(volumeMax), style, overflow = TextOverflow.Ellipsis, softWrap = false, maxLines = 1, constraints = axisConstraints)
            drawText(volume, topLeft = Offset(plot + 4f * density, volumeTop))
            val utc = measurer.measure("UTC", style, overflow = TextOverflow.Ellipsis, softWrap = false, maxLines = 1, constraints = axisConstraints)
            drawText(utc, topLeft = Offset(plot + 4f * density, volumeBottom))
        }
        val selection = touchCrosshair ?: mousePosition?.let(::selectAt)
        val selectedIndex = selection?.let { chosen -> candles.indexOfFirst { it.openTime == chosen.openTime } } ?: -1
        val selectedX = if (selectedIndex in range) candleCenter(selectedIndex, candles.size, plot, current.slot, current.offset) else null
        val selectedTimeLabel = selectedX?.let { measurer.measure(utcDateTimeLabel(candles[selectedIndex].openTime), style) }
        val selectedLabelX = if (selectedX != null && selectedTimeLabel != null) {
            (selectedX - selectedTimeLabel.size.width / 2f).coerceIn(0f, (plot - selectedTimeLabel.size.width).coerceAtLeast(0f))
        } else null
        val step = timeStepMinutes(current.slot, 64f * density)
        var lastRight = -1f
        for (i in range) {
            val candle = candles[i]
            if (candle.openTime % (step * 60_000L) != 0L) continue
            val x = candleCenter(i, candles.size, plot, current.slot, current.offset)
            val label = measurer.measure(utcLabel(candle.openTime, step >= 1440), style)
            val left = x - label.size.width / 2f
            if (selectedLabelX != null && selectedTimeLabel != null && left < selectedLabelX + selectedTimeLabel.size.width && left + label.size.width > selectedLabelX) continue
            if (left >= 0f && left >= lastRight && left + label.size.width <= plot) {
                drawText(label, topLeft = Offset(left, volumeBottom + 2f * density))
                lastRight = left + label.size.width + 4f * density
            }
        }
        if (selection != null && selectedIndex >= 0 && selectedIndex in range) {
            val candle = candles[selectedIndex]
            val x = candleCenter(selectedIndex, candles.size, plot, current.slot, current.offset)
            clipRect(left = 0f, top = 0f, right = plot, bottom = mainBottom) {
                drawLine(colors.crosshair, Offset(x, 0f), Offset(x, mainBottom), density)
                if (selection.price in prices.min..prices.max) {
                    val y = priceToY(selection.price, prices, 0f, mainBottom)
                    drawLine(colors.crosshair, Offset(0f, y), Offset(plot, y), density)
                }
                val previous = candles.getOrNull(selectedIndex - 1)?.takeIf { candle.openTime - it.openTime == 60_000L }
                val change = previous?.let { candle.close - it.close }
                val percent = previous?.let { (candle.close - it.close) / it.close * 100.0 }?.takeIf { it.isFinite() }
                val labels = mutableListOf(
                    utcDateTimeLabel(candle.openTime) to colors.panelText,
                    "Open ${priceLabel(candle.open)}  High ${priceLabel(candle.high)}" to colors.panelText,
                    "Low ${priceLabel(candle.low)}  Close ${priceLabel(candle.close)}" to colors.panelText,
                    "Change ${change?.let(::priceLabel) ?: "--"}  ${percent?.let { "${priceLabel(it)}%" } ?: "--"}" to colors.panelText,
                    "Volume ${volumeLabel(candle.volume)}${if (!candle.isClosed) "  unclosed" else ""}" to colors.panelText,
                )
                for ((series, period) in periods.withIndex()) {
                    val value = movingAverage(candles, period, selectedIndex..selectedIndex)[selectedIndex]
                    labels.add(movingAveragePanelLabel(period, selectedIndex, value) to colors.averages[series])
                }
                val maxWidth = (plot - 8f * density).coerceAtLeast(0f).toInt()
                val measured = labels.map { (text, color) ->
                    measurer.measure(text, TextStyle(color = color, fontSize = 10.sp), overflow = TextOverflow.Ellipsis, softWrap = false, maxLines = 1, constraints = Constraints(maxWidth = maxWidth))
                }
                val panelWidth = (measured.maxOfOrNull { it.size.width } ?: 0) + 8f * density
                val panelHeight = measured.sumOf { it.size.height } + 8f * density
                val panelX = if (x < plot / 2f) (plot - panelWidth).coerceAtLeast(0f) else 0f
                val panelSize = Size(panelWidth.coerceAtMost(plot), panelHeight.coerceAtMost(mainBottom))
                drawRoundRect(colors.panelBackground, Offset(panelX, 0f), panelSize, CornerRadius(6.dp.toPx()))
                drawRoundRect(colors.panelBorder, Offset(panelX, 0f), panelSize, CornerRadius(6.dp.toPx()), style = Stroke(1.dp.toPx()))
                var panelY = 4f * density
                for (line in measured) {
                    if (panelY + line.size.height > mainBottom) break
                    drawText(line, topLeft = Offset(panelX + 4f * density, panelY))
                    panelY += line.size.height
                }
            }
            if (selection.price in prices.min..prices.max) {
                val y = priceToY(selection.price, prices, 0f, mainBottom)
                val label = measurer.measure(priceLabel(selection.price), style, overflow = TextOverflow.Ellipsis, softWrap = false, maxLines = 1, constraints = axisConstraints)
                clipRect(left = plot, right = size.width.toFloat()) {
                    val labelY = (y - label.size.height / 2f).coerceIn(0f, (mainBottom - label.size.height).coerceAtLeast(0f))
                    drawRoundRect(colors.panelBackground, Offset(plot, labelY), Size(axisWidth, label.size.height.toFloat()), CornerRadius(3.dp.toPx()))
                    drawRoundRect(colors.panelBorder, Offset(plot, labelY), Size(axisWidth, label.size.height.toFloat()), CornerRadius(3.dp.toPx()), style = Stroke(1.dp.toPx()))
                    drawText(label, topLeft = Offset(plot + 4f * density, labelY))
                }
            }
            if (selectedTimeLabel != null && selectedLabelX != null) {
                clipRect(left = 0f, top = volumeBottom, right = plot, bottom = size.height.toFloat()) {
                    drawRoundRect(colors.panelBackground, Offset(selectedLabelX, volumeBottom), Size(selectedTimeLabel.size.width.toFloat(), timeHeight), CornerRadius(3.dp.toPx()))
                    drawRoundRect(colors.panelBorder, Offset(selectedLabelX, volumeBottom), Size(selectedTimeLabel.size.width.toFloat(), timeHeight), CornerRadius(3.dp.toPx()), style = Stroke(1.dp.toPx()))
                    drawText(selectedTimeLabel, topLeft = Offset(selectedLabelX, volumeBottom))
                }
            }
        }
    }
}
