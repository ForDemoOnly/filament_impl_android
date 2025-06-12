package com.example.filamentimpl.ECS.SystemStartup

import com.example.filamentimpl.ECS.Core.SystemIF.ISystemStartup
import com.example.filamentimpl.utils.loader.ModelLoader
import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.RenderableManager
import com.google.android.filament.Scene
import com.google.android.filament.VertexBuffer
import com.google.android.filament.utils.ModelViewer

class SpawnCuteFox(
    private val modelLoaderHelper: ModelLoader,
    private val entityManager: EntityManager,
    private val modelViewer: ModelViewer
) : ISystemStartup {
    override fun execute() {
        val entity: Int = entityManager.create();

        modelLoaderHelper.loadGltf("Fox")
        modelLoaderHelper.loadEnvironment("default_env")
//        RenderableManager.Builder(1).boundingBox(Box(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f))
//            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vb, ib)
//            .material(0, material)
//            .build(engine, entity)

        modelViewer.scene.addEntity(entity)
    }
}