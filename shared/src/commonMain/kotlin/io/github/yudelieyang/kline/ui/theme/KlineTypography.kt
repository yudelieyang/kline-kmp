package io.github.yudelieyang.kline.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object KlineTypography {
    val symbolTitle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    val symbolSubtitle = TextStyle(fontSize = 12.sp)
    val priceLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
    val priceChange = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
    val caption = TextStyle(fontSize = 11.sp)
    val statValue = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
    val maValue = TextStyle(fontSize = 12.sp, fontFeatureSettings = "tnum")
    val chip = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val banner = TextStyle(fontSize = 13.sp)
    val listHeader = TextStyle(fontSize = 11.sp)
    val listRow = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum")
    val sheetTitle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val sheetBody = TextStyle(fontSize = 13.sp)
    val mono = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
}
