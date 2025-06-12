package com.example.filamentimpl.ECS.Systems

import android.util.Log
import com.example.filamentimpl.ECS.Core.SystemIF.ISystemUpdate
import com.google.android.filament.EntityManager
import com.google.android.filament.TransformManager

class TransformSystem(private val transformManager: TransformManager): ISystemUpdate {
    override fun update(deltaTime: Float) {
        Log.d("TransformSystem", "update: $deltaTime")
    }
}