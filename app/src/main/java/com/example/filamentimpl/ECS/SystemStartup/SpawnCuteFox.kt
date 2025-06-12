package com.example.filamentimpl.ECS.SystemStartup

import com.example.filamentimpl.ECS.Core.SystemIF.ISystemStartup
import com.example.filamentimpl.utils.helper.ModelLoaderHelper
import com.google.android.filament.EntityManager

class SpawnCuteFox(private val modelLoaderHelper: ModelLoaderHelper, private val entityManager: EntityManager): ISystemStartup {
    override fun execute() {
        val entity: Int = entityManager.create();

        modelLoaderHelper.loadGltf("Fox")
        modelLoaderHelper.loadEnvironment("default_env")
    }
}