package io.github.yudelieyang.kline.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yudelieyang.kline.ui.model.*
import io.github.yudelieyang.kline.ui.theme.*
import kline_kmp_app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun ThemeToggleButton(dark: Boolean, onClick: () -> Unit) {
    val c = LocalKlineColors.current
    val desc = stringResource(if (dark) Res.string.cd_to_light else Res.string.cd_to_dark)
    Box(Modifier.size(48.dp).clickable(onClick = onClick).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(20.dp)) {
            val stroke = 1.6.dp.toPx()
            if (dark) {
                drawCircle(c.textSecondary, 4.dp.toPx(), style = Stroke(stroke))
                for (i in 0..7) {
                    val angle = i * kotlin.math.PI / 4
                    val x = kotlin.math.cos(angle).toFloat(); val y = kotlin.math.sin(angle).toFloat()
                    drawLine(c.textSecondary, center + Offset(x * 6.dp.toPx(), y * 6.dp.toPx()), center + Offset(x * 9.dp.toPx(), y * 9.dp.toPx()), stroke)
                }
            } else {
                drawCircle(c.textSecondary, 7.dp.toPx(), style = Stroke(stroke))
                drawCircle(c.background, 6.dp.toPx(), center + Offset(4.dp.toPx(), -4.dp.toPx()))
            }
        }
    }
}
