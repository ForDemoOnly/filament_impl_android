package com.example.filamentimpl.ECS

import com.google.android.filament.Entity

data class PositionComponent(val x: Float, val y: Float, val z: Float)
data class RenderableComponent(val entity: Entity) // Filament Entity
