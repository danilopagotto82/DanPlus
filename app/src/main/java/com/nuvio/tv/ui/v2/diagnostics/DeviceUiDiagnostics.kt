package com.nuvio.tv.ui.v2.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowManager
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.R
import java.util.Locale

/** Capture from the Activity view before opening a dialog, whose window has different bounds. */
fun readUiCanvasSnapshot(context: Context, view: View): UiCanvasSnapshot {
    val metrics = context.resources.displayMetrics
    val configuration = context.resources.configuration
    val root = view.rootView
    val mode = view.display?.mode
    val bounds = if (Build.VERSION.SDK_INT >= 30) {
        (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.currentWindowMetrics?.bounds
    } else null
    return UiCanvasSnapshot(
        windowWidthPx = root.width.takeIf { it > 0 } ?: bounds?.width() ?: metrics.widthPixels,
        windowHeightPx = root.height.takeIf { it > 0 } ?: bounds?.height() ?: metrics.heightPixels,
        baseDensity = metrics.density,
        densityDpi = metrics.densityDpi,
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
        modeId = mode?.modeId,
        outputWidthPx = mode?.physicalWidth,
        outputHeightPx = mode?.physicalHeight,
        refreshRateHz = mode?.refreshRate
    )
}

fun deviceUiDiagnosticReport(
    context: Context,
    view: View,
    scalePercent: Int,
    scaleReason: String = "Original"
): String {
    val canvas = readUiCanvasSnapshot(context, view)
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memory = manager?.let { ActivityManager.MemoryInfo().also(it::getMemoryInfo) }
    val frames = UiFrameDiagnostics.frames.snapshot(System.nanoTime())
    val unavailable = context.getString(R.string.v2_ui_diag_unavailable)
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    fun number(value: Number?): String = value?.let { String.format(locale, "%.2f", it.toDouble()) } ?: unavailable
    return buildString {
        fun line(resource: Int, vararg args: Any) = appendLine(context.getString(resource, *args))
        appendLine("${Build.MODEL} / API ${Build.VERSION.SDK_INT}")
        appendLine("${context.getString(R.string.app_name)} ${BuildConfig.VERSION_NAME} / ${BuildConfig.BUILD_TYPE}")
        line(R.string.v2_ui_diag_window, canvas.windowWidthPx, canvas.windowHeightPx)
        line(R.string.v2_ui_diag_density, number(canvas.baseDensity), canvas.densityDpi)
        line(R.string.v2_ui_diag_screen, canvas.screenWidthDp, canvas.screenHeightDp)
        line(R.string.v2_ui_diag_base, number(canvas.logicalWidthDp), number(canvas.logicalHeightDp))
        line(R.string.v2_ui_diag_scale, scalePercent, scaleReason)
        line(R.string.v2_ui_diag_effective, number(canvas.effectiveWidthDp(scalePercent)), number(canvas.effectiveHeightDp(scalePercent)))
        line(R.string.v2_ui_diag_output, canvas.modeId?.toString() ?: unavailable,
            canvas.outputWidthPx?.toString() ?: "?", canvas.outputHeightPx?.toString() ?: "?", number(canvas.refreshRateHz))
        line(R.string.v2_ui_diag_ram, number(memory?.totalMem?.div(1_073_741_824.0)), manager?.memoryClass?.toString() ?: "?",
            manager?.isLowRamDevice?.let { context.getString(if (it) R.string.v2_ui_diag_yes else R.string.v2_ui_diag_no) } ?: "?")
        line(R.string.v2_ui_diag_acceleration, context.getString(if (view.isHardwareAccelerated) R.string.v2_ui_diag_yes else R.string.v2_ui_diag_no))
        line(R.string.v2_ui_diag_frames, frames.frames)
        line(R.string.v2_ui_diag_jank, number(frames.jankPercent))
        line(R.string.v2_ui_diag_timing, number(frames.p50UiMs), number(frames.p95UiMs), number(frames.p99UiMs))
        append(context.getString(R.string.v2_ui_diag_note))
    }
}
