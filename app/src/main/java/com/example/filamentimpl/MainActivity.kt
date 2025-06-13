package com.example.filamentimpl

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Choreographer
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.filamentimpl.utils.helper.GraphicQualityHelper
import com.example.filamentimpl.utils.loader.ModelLoader
import com.google.android.filament.*
import com.google.android.filament.android.*
import com.google.android.filament.gltfio.*
import com.google.android.filament.utils.*


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ModelLoaderHelper"

        init {
            Utils.init() // Initialize Filament
        }
    }

    private lateinit var fpsTextView: TextView
    private lateinit var googleMapSurface: SurfaceView
    private lateinit var filamentSurface: SurfaceView
    private lateinit var modelViewer: ModelViewer
    private lateinit var choreographer: Choreographer
    private lateinit var filamentAsset: FilamentAsset
    private lateinit var graphicQualityHelper: GraphicQualityHelper
    private lateinit var modelLoaderHelper: ModelLoader
    private lateinit var uiHelper: UiHelper
    private val automation = AutomationEngine()
    private val frameScheduler = FrameCallback()

    @Entity
    private var foxEntityID = 0

    @Entity var sunlightEntityID = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        fpsTextView = findViewById(R.id.fpsTxt)
        googleMapSurface = findViewById(R.id.googleMapSurface)
        filamentSurface = findViewById(R.id.filamentSurface)
        choreographer = Choreographer.getInstance()

        setupSurfaceView()
        setupFilament()
        setupView()

        graphicQualityHelper = GraphicQualityHelper(modelViewer.view)
        graphicQualityHelper.setRenderQuality(View.QualityLevel.MEDIUM)
        if (false) {
            graphicQualityHelper.dynamicResolutionOptions(View.QualityLevel.MEDIUM)
            graphicQualityHelper.multiSampleAntiAliasingOptions(true)
            graphicQualityHelper.setBloom(true)
            graphicQualityHelper.setAntiAliasing(View.AntiAliasing.FXAA)
            graphicQualityHelper.setAmbientOcclusion(true);
        }

        modelLoaderHelper = ModelLoader(assets, modelViewer, automation)

        foxEntityID = EntityManager.get().create()

//        RenderableManager.Builder(1).boundingBox(Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)).material(0, )
        modelLoaderHelper.loadGltf("Fox")
//        sunlightEntityID = modelLoaderHelper.loadEnvironment("default_env")
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

    private fun setupSurfaceView() {
        filamentSurface.setZOrderOnTop(true)
        filamentSurface.holder.setFormat(PixelFormat.TRANSLUCENT)
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)

        // NOTE: To choose a specific rendering resolution, add the following line:
        // uiHelper.setDesiredSize(1280, 720)

        uiHelper.attachTo(filamentSurface)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFilament() {
        val engine = Engine.create()
        modelViewer = ModelViewer(filamentSurface, engine)
        filamentSurface.setOnTouchListener(modelViewer)
    }

    private fun setupView() {
        modelViewer.view.blendMode = View.BlendMode.TRANSLUCENT

        modelViewer.renderer.clearOptions = modelViewer.renderer.clearOptions.apply {
            clear = false
        }
    }

    inner class FrameCallback : Choreographer.FrameCallback {
        private val startTime = System.nanoTime()
        private var frameCount = 0
        private var lastFpsUpdateTime = startTime
        @SuppressLint("DefaultLocale")
        override fun doFrame(frameTimeNanos: Long) {
            val deltaTime = (frameTimeNanos - startTime).toDouble() / 1_000_000_000
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

            modelViewer.render(frameTimeNanos)
        }
    }
}