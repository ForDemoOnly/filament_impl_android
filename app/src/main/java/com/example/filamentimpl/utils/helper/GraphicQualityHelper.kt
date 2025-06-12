package com.example.filamentimpl.utils.helper

import com.google.android.filament.View

class GraphicQualityHelper(private val view: View) {
    // on mobile, better use lower quality color buffer
    fun setRenderQuality(quality: View.QualityLevel) {
        view.renderQuality = view.renderQuality.apply {
            hdrColorBuffer = quality
        }
    }

    // dynamic resolution often helps a lot
    fun dynamicResolutionOptions(quality: View.QualityLevel) {
        view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
            enabled = true
            quality
        }
    }

    // MSAA is needed with dynamic resolution MEDIUM
    fun multiSampleAntiAliasingOptions(enabled: Boolean) {
        view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply {
            enabled
        }
    }

    fun setAntiAliasing(antiAliasing: View.AntiAliasing) {
        view.antiAliasing = antiAliasing
    }

    fun setAmbientOcclusion(enabled: Boolean) {
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
            enabled
        }
    }

    fun setBloom(enabled: Boolean) {
        view.bloomOptions = view.bloomOptions.apply {
            enabled
        }
    }
}