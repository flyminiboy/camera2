package com.example.camera2

import android.graphics.SurfaceTexture

interface IDrawer {

    fun draw()

    fun release()

    fun bindTextureId(textureId:Int,surfaceTexture: SurfaceTexture)

}