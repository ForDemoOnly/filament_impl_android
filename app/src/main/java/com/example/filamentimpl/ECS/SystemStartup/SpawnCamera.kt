package com.example.filamentimpl.ECS.SystemStartup

import com.example.filamentimpl.ECS.Core.SystemIF.ISystemStartup
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.utils.Float3

class SpawnCamera(private val entityManager: EntityManager, private val engine: Engine) :
    ISystemStartup {
    override fun execute() {
        val entity: Int = entityManager.create();
        val camera: Camera = engine.createCamera(entity)
        // Setup camera position
        val eye = Float3(0.0f, 0.0f, 4.0f)
        val center = Float3(0.0f, 0.0f, 0.0f)
        val up = Float3(0.0f, 1.0f, 0.0f)
        camera.lookAt(
            eye.x.toDouble(),
            eye.y.toDouble(),
            eye.z.toDouble(),
            center.x.toDouble(),
            center.y.toDouble(),
            center.z.toDouble(),
            up.x.toDouble(),
            up.y.toDouble(), up.z.toDouble()
        )
    }
}