package com.example.camera2

import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PlayRender(private val drawer:IDrawer) : GLSurfaceView.Renderer {


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val textureId = TextureUtil.createTexture(1)
        drawer.bindTextureId(textureId)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        drawer.draw()
    }

}