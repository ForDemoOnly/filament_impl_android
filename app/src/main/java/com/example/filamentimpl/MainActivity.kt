package com.example.filamentimpl

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Choreographer
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.filamentimpl.ECS.Systems.TransformSystem
import com.example.filamentimpl.utils.helper.GraphicQualityHelper
import com.example.filamentimpl.utils.helper.ModelLoaderHelper
import com.google.android.filament.View
import com.google.android.filament.utils.AutomationEngine
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils

class MainActivity : AppCompatActivity() {
    companion object {
        init {
            Utils.init() // Initialize Filament
        }
    }

    private lateinit var fpsTextView: TextView
    private lateinit var surfaceView: SurfaceView
    private lateinit var modelViewer: ModelViewer
    private lateinit var choreographer: Choreographer
    private val automation = AutomationEngine()
    private val viewerContent = AutomationEngine.ViewerContent()
    private val frameScheduler = FrameCallback()
    private lateinit var modelLoaderHelper: ModelLoaderHelper
    private lateinit var graphicQualityHelper: GraphicQualityHelper

    // ECS - System
    private lateinit var transformSystem: TransformSystem;

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        fpsTextView = findViewById(R.id.fpsTxt)
        surfaceView = findViewById(R.id.filamentSurface)
        choreographer = Choreographer.getInstance()

        modelViewer = ModelViewer(surfaceView)
        surfaceView.setOnTouchListener(modelViewer)

        viewerContent.view = modelViewer.view
        viewerContent.sunlight = modelViewer.light
        viewerContent.lightManager = modelViewer.engine.lightManager
        viewerContent.scene = modelViewer.scene
        viewerContent.renderer = modelViewer.renderer

        modelLoaderHelper = ModelLoaderHelper(assets, modelViewer, automation)
        graphicQualityHelper = GraphicQualityHelper(modelViewer.view)

        // ECS - System - Initialize
        transformSystem = TransformSystem(modelViewer.engine.transformManager)

        modelLoaderHelper.loadGltf("Fox")
        modelLoaderHelper.loadEnvironment("default_env")

        graphicQualityHelper.setRenderQuality(View.QualityLevel.MEDIUM)

        graphicQualityHelper.dynamicResolutionOptions(View.QualityLevel.MEDIUM)

        graphicQualityHelper.multiSampleAntiAliasingOptions(true)
    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameScheduler)
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameScheduler)
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.removeFrameCallback(frameScheduler)
    }

    inner class FrameCallback : Choreographer.FrameCallback {
        private val startTime = System.nanoTime()
        private var frameCount = 0
        private var lastFpsUpdateTime = startTime
        override fun doFrame(frameTimeNanos: Long) {
            val deltaTime = (frameTimeNanos - startTime).toDouble() / 1_000_000_000

            transformSystem.update(deltaTime.toFloat())

            choreographer.postFrameCallback(this)

            // Calculate FPS
            frameCount++
            val elapsedNanos = frameTimeNanos - lastFpsUpdateTime

            // Update FPS every second
            if (elapsedNanos > 1_000_000_000) {
                val fps = (frameCount * 1e9) / elapsedNanos
                fpsTextView.post {
                    fpsTextView.text = String.format("FPS: %.1f", fps)
                }
                // Reset counters
                frameCount = 0
                lastFpsUpdateTime = frameTimeNanos
            }

            modelViewer.animator?.apply {
                if (animationCount > 0) {
                    applyAnimation(0, deltaTime.toFloat())
                }
                updateBoneMatrices()
            }

            modelViewer.asset?.apply {

            }
            modelViewer.render(frameTimeNanos)
        }
    }
}