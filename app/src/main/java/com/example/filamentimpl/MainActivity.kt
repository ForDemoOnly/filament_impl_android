package com.example.filamentimpl

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.filamentimpl.ECS.SystemStartup.SpawnCuteFox
import com.example.filamentimpl.ECS.SystemsUpdate.TransformSystem
import com.example.filamentimpl.utils.helper.GraphicQualityHelper
import com.example.filamentimpl.utils.loader.ModelLoader
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.MaterialProvider
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.AutomationEngine
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ModelLoaderHelper"
        init {
            Utils.init() // Initialize Filament
        }
    }

    private lateinit var fpsTextView: TextView
    private lateinit var surfaceView: SurfaceView
    private lateinit var modelViewer: ModelViewer
    private lateinit var choreographer: Choreographer
    private lateinit var assetLoader: AssetLoader
    private lateinit var materialProvider: MaterialProvider
    private lateinit var entityManager: EntityManager
    private val automation = AutomationEngine()
    private val frameScheduler = FrameCallback()
    private lateinit var modelLoaderHelper: ModelLoader
    private lateinit var displayHelper: DisplayHelper
    private lateinit var uiHelper: UiHelper
    private lateinit var graphicQualityHelper: GraphicQualityHelper
    // A renderer instance is tied to a single surface (SurfaceView, TextureView, etc.)
    private lateinit var renderer: Renderer
    // A swap chain is Filament's representation of a surface
    private var swapChain: SwapChain? = null
    // Should be pretty obvious :)
    private lateinit var camera: Camera
    // A view defines a viewport, a scene and a camera for rendering
    private lateinit var view: View
    // A scene holds all the renderable, lights, etc. to be drawn
    private lateinit var scene: Scene

    // ECS - System
    private lateinit var transformSystem: TransformSystem;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        fpsTextView = findViewById(R.id.fpsTxt)
        surfaceView = findViewById(R.id.filamentSurface)
        choreographer = Choreographer.getInstance()
        displayHelper = DisplayHelper(this)

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

        modelLoaderHelper = ModelLoader(assetLoader, assets, modelViewer, automation)

        // ECS - System - Initialize
        transformSystem = TransformSystem(modelViewer.engine.transformManager)
        SpawnCuteFox(modelLoaderHelper, entityManager, modelViewer).execute();
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
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
        uiHelper.renderCallback = SurfaceCallback()

        // NOTE: To choose a specific rendering resolution, add the following line:
        // uiHelper.setDesiredSize(1280, 720)

        uiHelper.attachTo(surfaceView)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFilament() {
        val engine = Engine.create()
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        camera = engine.createCamera(engine.entityManager.create())

        modelViewer = ModelViewer(surfaceView, engine)
        surfaceView.setOnTouchListener(modelViewer)
        entityManager = EntityManager.get()
        materialProvider = UbershaderProvider(modelViewer.engine)
        assetLoader = AssetLoader(modelViewer.engine, materialProvider, entityManager)
    }

    private fun setupView() {
        // NOTE: Try to disable post-processing (tone-mapping, etc.) to see the difference
        // view.isPostProcessingEnabled = false

        // Tell the view which camera we want to use
        view.camera = camera

        // Tell the view which scene we want to render
        view.scene = scene
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

    inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
            swapChain?.let { modelViewer.engine.destroySwapChain(it) }
            swapChain = modelViewer.engine.createSwapChain(surface)
            displayHelper.attach(renderer, surfaceView.display)
        }

        override fun onDetachedFromSurface() {
            displayHelper.detach()
            swapChain?.let {
                modelViewer.engine.destroySwapChain(it)
                // Required to ensure we don't return before Filament is done executing the
                // destroySwapChain command, otherwise Android might destroy the Surface
                // too early
                modelViewer.engine.flushAndWait()
                swapChain = null
            }
        }

        override fun onResized(width: Int, height: Int) {
            val aspect = width.toDouble() / height.toDouble()
            camera.setProjection(45.0, aspect, 0.1, 20.0, Camera.Fov.VERTICAL)

            view.viewport = Viewport(0, 0, width, height)

            FilamentHelper.synchronizePendingFrames(modelViewer.engine)
        }
    }
}