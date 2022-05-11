package com.example.camera2

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PlayRender : GLSurfaceView.Renderer {

    private val drawers = mutableListOf<IDrawer>()

    fun addDrawer(drawer: IDrawer) {
        drawers.add(drawer)
    }

    private lateinit var surfaceTexture: SurfaceTexture

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val textureId = TextureUtil.createTexture(1)
        surfaceTexture = SurfaceTexture(textureId).apply {
            onSurfaceTextureAvailable(this)
        }

        drawers.map { drawer->
            drawer.bindTextureId(textureId, surfaceTexture)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        drawers.map { drawer->
            drawer.draw()
        }

    }

    private var mSurfaceTextureListener: ((surface: SurfaceTexture) -> Unit)? = { _ -> }

    private fun onSurfaceTextureAvailable(surface: SurfaceTexture) {
        mSurfaceTextureListener?.apply {
            this.invoke(surface)
        }
    }

    fun setSurfaceTextureAvailableLisener(listener: ((surface: SurfaceTexture) -> Unit)? = null) {
        mSurfaceTextureListener = listener
    }

}