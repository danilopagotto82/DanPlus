package com.nuvio.tv.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor

/** Cap the zoom on source art that differs substantially from the banner shape. */
internal object CollectionBannerScale : ContentScale {
    override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
        val fit = ContentScale.Fit.computeScaleFactor(srcSize, dstSize).scaleX
        val crop = ContentScale.Crop.computeScaleFactor(srcSize, dstSize).scaleX
        val scale = minOf(crop, fit * 1.32f)
        return ScaleFactor(scale, scale)
    }
}
