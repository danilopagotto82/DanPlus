package com.nuvio.tv.core.player.thumbnail

internal enum class ThumbnailOrigin { SEEKR, LOCAL, MEDIA_SERVER }

internal enum class ThumbnailState { AVAILABLE, GENERATING, UNAVAILABLE, FAILED }

/** Facts about the current preview session. Sampling never opens a file, a player or a network connection. */
internal data class ThumbnailDiagnostic(
    val state: ThumbnailState,
    val origin: ThumbnailOrigin? = null,
    val fromCache: Boolean = false,
    val intervalMs: Long? = null
)

internal fun chooseThumbnailDiagnostic(
    remote: ThumbnailDiagnostic?,
    local: ThumbnailDiagnostic?
): ThumbnailDiagnostic = when {
    remote?.state == ThumbnailState.AVAILABLE -> remote
    local != null && local.state != ThumbnailState.UNAVAILABLE -> local
    remote != null -> remote
    else -> local ?: ThumbnailDiagnostic(ThumbnailState.UNAVAILABLE)
}
