package io.github.yudelieyang.kline.ui.model

enum class DataSourceMode { Live, Offline }
enum class OfflineLoadState { Loading, Failed, Ready }
fun offlineLoadState(loaded: Boolean, failed: Boolean): OfflineLoadState = when {
    loaded -> OfflineLoadState.Ready
    failed -> OfflineLoadState.Failed
    else -> OfflineLoadState.Loading
}
sealed interface BannerUi {
    data class Warning(val connection: ConnectionUi, val stoppedAt: Long?, val hasData: Boolean) : BannerUi
    data class Offline(val startTime: Long, val endTime: Long) : BannerUi
}
fun bannerFor(mode: DataSourceMode, connection: ConnectionUi, lastUpdate: Long?, hasData: Boolean, offline: LoadedStats?): BannerUi? =
    if (mode == DataSourceMode.Offline) offline?.let { BannerUi.Offline(it.startTime, it.endTime) }
    else if (connection.phase == ConnectionPhase.Live) null else BannerUi.Warning(connection, lastUpdate, hasData)
