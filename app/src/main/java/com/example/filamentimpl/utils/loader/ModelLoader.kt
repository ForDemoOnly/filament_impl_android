package com.example.filamentimpl.utils.loader

import android.content.res.AssetManager
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.gltfio.*
import com.google.android.filament.utils.*
import java.nio.ByteBuffer

class ModelLoader(
    private val assetManager: AssetManager,
    private val modelViewer: ModelViewer,
    private val automation: AutomationEngine
) {
    companion object {
        private const val TAG = "ModelLoaderHelper"
    }

    fun loadGlb(name: String) {
        val buffer = readAsset("models/${name}.glb")
        modelViewer.loadModelGlb(buffer)
        updateRootTransform()
    }

    fun loadGltf(name: String) {
        try {
            val buffer = readAsset("models/${name}.gltf")
            modelViewer.loadModelGltf(buffer) { uri -> readAsset("models/$uri") }
            updateRootTransform()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadEnvironment(ibl: String): Int {
        // Create the indirect light source and add it to the scene.
        var buffer = readAsset("envs/$ibl/${ibl}_ibl.ktx")
        KTX1Loader.createIndirectLight(modelViewer.engine, buffer).apply {
            intensity = 50_000f
            modelViewer.scene.indirectLight = this
        }

        // Create the sky box and add it to the scene.
        buffer = readAsset("envs/$ibl/${ibl}_skybox.ktx")
        KTX1Loader.createSkybox(modelViewer.engine, buffer).apply {
            modelViewer.scene.skybox = this
        }

        val sunlight = EntityManager.get().create()

        // Add directional light
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.98f, 0.95f)
            .intensity(110_000f)
            .direction(0.28f, -0.6f, -0.76f)
            .build(modelViewer.engine, sunlight)

        modelViewer.scene.addEntity(sunlight)
        return sunlight
    }

    private fun readAsset(assetName: String): ByteBuffer {
        val input = assetManager.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    private fun updateRootTransform() {
        if (automation.viewerOptions.autoScaleEnabled) {
            modelViewer.transformToUnitCube()
        } else {
            modelViewer.clearRootTransform()
        }
    }
}