package com.example.filamentimpl

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.filamentimpl.ECS.SystemStartup.SpawnCuteFox
import com.example.filamentimpl.ECS.SystemsUpdate.TransformSystem
import com.example.filamentimpl.utils.helper.GraphicQualityHelper
import com.example.filamentimpl.utils.loader.Ibl
import com.example.filamentimpl.utils.loader.Mesh
import com.example.filamentimpl.utils.loader.ModelLoader
import com.example.filamentimpl.utils.loader.loadIbl
import com.example.filamentimpl.utils.loader.loadMesh
import com.google.android.filament.Camera
import com.google.android.filament.Colors
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.filamat.MaterialBuilder
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.MaterialProvider
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.AutomationEngine
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import com.google.android.filament.*

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    private lateinit var material: Material
    private lateinit var mesh: Mesh
    private lateinit var ibl: Ibl
    private lateinit var materialInstance: MaterialInstance
    private val automation = AutomationEngine()
    private val frameScheduler = FrameCallback()
    private val animator = ValueAnimator.ofFloat(0.0f, (2.0 * PI).toFloat())

    // Filament entity representing a renderable object
    @Entity
    private var light = 0

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
        setupScene()

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

    private fun setupScene() {
        buildMaterial()
        setupMaterial()
        loadImageBasedLight()

        scene.skybox = ibl.skybox
        scene.indirectLight = ibl.indirectLight

        // This map can contain named materials that will map to the material names
        // loaded from the filamesh file. The material called "DefaultMaterial" is
        // applied when no named material can be found
        val materials = mapOf("DefaultMaterial" to materialInstance)

        // Load the mesh in the filamesh format (see filamesh tool)
        mesh = loadMesh(assets, "models/shader_ball.filamesh", materials, modelViewer.engine)

        // Move the mesh down
        // Filament uses column-major matrices
        modelViewer.engine.transformManager.setTransform(
            modelViewer.engine.transformManager.getInstance(mesh.renderable),
            floatArrayOf(
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, -1.2f, 0.0f, 1.0f
            )
        )

        // Add the entity to the scene to render it
        scene.addEntity(mesh.renderable)

        // We now need a light, let's create a directional light
        light = EntityManager.get().create()

        // Create a color from a temperature (D65)
        val (r, g, b) = Colors.cct(6_500.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(r, g, b)
            // Intensity of the sun in lux on a clear day
            .intensity(110_000.0f)
            // The direction is normalized on our behalf
            .direction(-0.753f, -1.0f, 0.890f)
            .castShadows(true)
            .build(modelViewer.engine, light)

        // Add the entity to the scene to light it
        scene.addEntity(light)

        // Set the exposure on the camera, this exposure follows the sunny f/16 rule
        // Since we've defined a light that has the same intensity as the sun, it
        // guarantees a proper exposure
        camera.setExposure(16.0f, 1.0f / 125.0f, 100.0f)

        startAnimation()
    }

    private fun loadImageBasedLight() {
        ibl = loadIbl(assets, "envs/flower_road_no_sun_2k", modelViewer.engine)
        ibl.indirectLight.intensity = 40_000.0f
    }

    private fun startAnimation() {
        // Animate the triangle
        animator.interpolator = LinearInterpolator()
        animator.duration = 18_000
        animator.repeatMode = ValueAnimator.RESTART
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener { a ->
            val v = (a.animatedValue as Float)
            camera.lookAt(cos(v) * 4.5, 1.5, sin(v) * 4.5, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
        }
        animator.start()
    }

    private fun buildMaterial() {
        // MaterialBuilder.init() must be called before any MaterialBuilder methods can be used.
        // It only needs to be called once per process.
        // When your app is done building materials, call MaterialBuilder.shutdown() to free
        // internal MaterialBuilder resources.
        MaterialBuilder.init()

        val matPackage = MaterialBuilder()
            // By default, materials are generated only for DESKTOP. Since we're an Android
            // app, we set the platform to MOBILE.
            .platform(MaterialBuilder.Platform.MOBILE)

            // Set the name of the Material for debugging purposes.
            .name("Clear coat")

            // Defaults to LIT. We could change the shading model here if we desired.
            .shading(MaterialBuilder.Shading.LIT)

            // Add a parameter to the material that can be set via the setParameter method once
            // we have a material instance.
            .uniformParameter(MaterialBuilder.UniformType.FLOAT3, "baseColor")

            // Fragment block- see the material readme (docs/Materials.md.html) for the full
            // specification.
            .material(
                "void material(inout MaterialInputs material) {\n" +
                        "    prepareMaterial(material);\n" +
                        "    material.baseColor.rgb = materialParams.baseColor;\n" +
                        "    material.roughness = 0.65;\n" +
                        "    material.metallic = 1.0;\n" +
                        "    material.clearCoat = 1.0;\n" +
                        "}\n"
            )

            // Turn off shader code optimization so this sample is compatible with the "lite"
            // variant of the filamat library.
            .optimization(MaterialBuilder.Optimization.NONE)

            // When compiling more than one material variant, it is more efficient to pass an Engine
            // instance to reuse the Engine's job system
            .build(modelViewer.engine)

        if (matPackage.isValid) {
            val buffer = matPackage.buffer
            material =
                Material.Builder().payload(buffer, buffer.remaining()).build(modelViewer.engine)
        }

        // We're done building materials, so we call shutdown here to free resources. If we wanted
        // to build more materials, we could call MaterialBuilder.init() again (with a slight
        // performance hit).
        MaterialBuilder.shutdown()
    }

    private fun setupMaterial() {
        // Create an instance of the material to set different parameters on it
        materialInstance = material.createInstance()
        // Specify that our color is in sRGB so the conversion to linear
        // is done automatically for us. If you already have a linear color
        // you can pass it directly, or use Colors.RgbType.LINEAR
        materialInstance.setParameter("baseColor", Colors.RgbType.SRGB, 0.71f, 0.0f, 0.0f)
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