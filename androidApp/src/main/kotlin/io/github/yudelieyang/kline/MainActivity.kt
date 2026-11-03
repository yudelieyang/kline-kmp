package io.github.yudelieyang.kline

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val perfMode = BuildConfig.PERF_BUILD && intent.getBooleanExtra("perf_mode", false)
        val count = intent.getIntExtra("perf_count", 500)
        val width = intent.getIntExtra("perf_width_dp", 8)
        require(!perfMode || (count in listOf(500, 5000) && width in listOf(2, 8))) {
            "Invalid benchmark arguments"
        }
        setContent {
            App(perfCount = if (perfMode) count else null, perfWidthDp = width,
                perfDiagnostic = if (BuildConfig.PERF_BUILD && !perfMode) ({ message -> Log.i("KlinePerf", message); Unit }) else null) { dark ->
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() { App() }
